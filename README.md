# Finlumia Backend

Backend da plataforma Finlumia, organizado como monorepo Gradle com módulos Spring Boot.

Este documento foi estruturado para apoiar onboarding, manutenção e evolução do projeto com segurança, cobrindo:

- visão arquitetural e responsabilidades por módulo;
- fluxo de chamadas HTTP e camadas internas;
- subida do ambiente de desenvolvimento;
- build, empacotamento e subida de containers;
- boas práticas operacionais e de segurança.

## Menu de navegação

- [1) Visão geral do repositório](#1-visão-geral-do-repositório)
- [2) Fluxo de responsabilidades por camada](#2-fluxo-de-responsabilidades-por-camada)
- [3) Fluxo de chamadas HTTP (requisição a resposta)](#3-fluxo-de-chamadas-http-requisição-a-resposta)
- [4) Fluxo entre módulos e documentação (chamadas internas de docs)](#4-fluxo-entre-módulos-e-documentação-chamadas-internas-de-docs)
- [5) Portas e perfis locais](#5-portas-e-perfis-locais)
- [6) Subida do ambiente de desenvolvimento (container de desenvolvimento)](#6-subida-do-ambiente-de-desenvolvimento-container-de-desenvolvimento)
- [7) Rodar módulos sem container (execução Gradle local)](#7-rodar-módulos-sem-container-execução-gradle-local)
- [8) Build de imagem e subida de containers dos módulos](#8-build-de-imagem-e-subida-de-containers-dos-módulos)
- [9) Subida de containers de produção (módulos + banco de dados)](#9-subida-de-containers-de-produção-módulos--banco-de-dados)
- [10) Sequência recomendada para onboarding técnico](#10-sequência-recomendada-para-onboarding-técnico)
- [11) Troubleshooting rápido](#11-troubleshooting-rápido)
- [12) Boas práticas de manutenção](#12-boas-práticas-de-manutenção)
- [13) Segurança: pontos obrigatórios](#13-segurança-pontos-obrigatórios)
- [14) Alinhamento técnico](#14-alinhamento-técnico)

---

## 1) Visão geral do repositório

Este repositório concentra serviços de backend e bibliotecas compartilhadas.

### Módulos atuais (arquivo `settings.gradle`)

Monorepo de **microserviços** com **banco monolítico** (PostgreSQL único; cada módulo = schema homônimo).

| Módulo | Executável | Schema DB | Porta dev |
|--------|------------|-----------|-----------|
| `configurator` | sim | `configurator` | 28081 |
| `docs` | sim | — | 28082 |
| `identify` | sim | `identify` | 28083 |
| `movement` | sim | `movement` | 28084 |
| `shared` | **não** (biblioteca) | — | — |

- `shared`: apenas classes genéricas (`FinlumiaException`, `DialogDefault`, `GlobalExceptionHandler`) e `shared.properties` (datasource comum).
- Demais módulos: segurança, interceptors, OpenAPI e JDBC ficam **no próprio módulo**.

### Estrutura de pacotes (cada microserviço)

```
br.com.finlumia.<modulo>/
  controllers/
    internal/   → /internal/<modulo>/**  (service-to-service)
    external/   → /api/<modulo>/**       (clientes)
  models/
  services/
  repositorys/
  views/
  config/       → beans Spring (não é domínio de negócio)
```

### Como decidir “em qual módulo mexer”

- regra ou endpoint de um domínio: no módulo do domínio (`configurator`, `identify`, `movement`);
- contrato HTTP genérico de erro: `shared`;
- agregação Swagger: `docs`.

### Stack técnica

- Java 21
- Spring Boot 4.0.5
- Gradle multi-módulo
- PostgreSQL (acesso JDBC)
- Lombok
- Docker/Buildx para empacotamento e execução em container

---

## 2) Fluxo de responsabilidades por camada

Nos módulos de aplicação, o padrão esperado é:

- `controllers/internal` e `controllers/external`: endpoints internos vs públicos;
- `services`: regras de negócio, filtros JWT (identify), token interno, interceptors;
- `repositorys`: JDBC no schema do módulo (`identify.users`, etc.);
- `models` / `views`: entrada e saída HTTP;
- `shared`: somente exceção/resposta padrão compartilhada.

### Regra prática para manutenção

- mudança de rota/contrato: `controllers` + `models` / `views`;
- mudança de regra de negócio: `services`;
- mudança de SQL: `repositorys` (schema = nome do módulo);
- **não** colocar segurança, datasource ou interceptors no `shared`.

### Fronteira de responsabilidades (evitar acoplamento)

- `controller` não deve conter regra de negócio complexa;
- `service` não deve conhecer detalhes de HTTP (headers, status code, etc.);
- `repository` não deve decidir regra de negócio;
- contratos (`models/views`) devem ser claros, estáveis e versionáveis quando necessário.

---

## 3) Fluxo de chamadas HTTP (requisição a resposta)

Fluxo simplificado de uma chamada para o módulo `configurator`:

1. O cliente chama um endpoint em `/api/configurator/**`.
2. O `KeyUserInterceptor` (em cada módulo de API) intercepta rotas `/api/**` e resolve o contexto `keyUser`.
3. O `Controller` valida o payload e encaminha para o `Service`.
4. O `Service` aplica as regras de negócio e chama o `Repository`.
5. O `repository` do módulo usa o `DataSource` local e o schema homônimo no PostgreSQL.
6. A resposta retorna ao cliente no formato definido em `views`/DTOs.
7. Em caso de erro, o `GlobalExceptionHandler` padroniza o retorno com base em `FinlumiaException`.

### O que observar em cada etapa do fluxo

- **Entrada HTTP (`controller`)**: validar payload, mapear campos obrigatórios e garantir contrato consistente.
- **Contexto de segurança (`KeyUserInterceptor`)**: garantir leitura e validação de `keyUser` para rotas sob `/api/**`.
- **Regra de negócio (`service`)**: garantir consistência de domínio, evitando regra duplicada em outros pontos.
- **Persistência (`repository`)**: manter consultas claras, seguras e fáceis de manter.
- **Resposta e exceções**: padronizar erros para facilitar observabilidade, suporte e consumo por front-end/integrações.

### Checklist de alteração por endpoint

1. Validar se o endpoint fica em `/api/**` e exige `keyUser`.
2. Confirmar contrato de request e response.
3. Implementar/ajustar regra de negócio no `service`.
4. Revisar impacto em consulta/persistência.
5. Validar retorno de erro padronizado em cenários de falha.
6. Atualizar documentação no módulo `docs`, quando aplicável.

### Exemplo de endpoints já existentes (`configurator`)

Base: `/api/configurator`

- `POST /generic_register`
- `POST /generic_list`

---

## 4) Fluxo entre módulos e documentação (chamadas internas de docs)

O módulo `docs` centraliza visualização de APIs em `http://localhost:40574/docs/swagger-ui.html`.

Ele consulta os endpoints de documentação de cada serviço:

- `configurator`: `http://localhost:40571/internal/docs/api-docs`
- `identify`: `http://localhost:28083/internal/docs/api-docs`
- `movement`: `http://localhost:40573/internal/docs/api-docs`
- `docs`: `http://localhost:40574/v3/api-docs`

Se algum serviço não subir, o Swagger agregador pode apresentar falhas parciais de carregamento.

### Dependência operacional do módulo `docs`

Para visualizar toda a documentação agregada, o `docs` depende de:

- `docs` ativo na porta `40574`;
- módulos de negócio ativos nas portas esperadas (`40571`, `40572`, `40573`);
- endpoints de docs internos acessíveis (`/internal/docs/api-docs`).

Se um módulo estiver fora do ar, o Swagger pode continuar abrindo, mas com falha no carregamento da API específica.

---

## 5) Portas e perfis locais

Cada aplicação roda de forma independente:

- `configurator`: `40571`
- `identify`: `28083`
- `movement`: `40573`
- `docs`: `40574`
- ambiente de desenvolvimento (`dev container`): `40570`

Os perfis suportados pelos scripts são:

- `dev`: desenvolvimento local;
- `pro`: perfil de empacotamento/distribuição.

---

## 6) Subida do ambiente de desenvolvimento (container de desenvolvimento)

O ambiente de desenvolvimento usa `docker-compose.dev.yml`, com:

- container `finlumiadev`;
- volume do repositório montado em `/workspace`;
- cache Gradle persistido em volume Docker;
- Docker socket montado para permitir build de imagens de dentro do container.

### Quando usar o dev container

Use o ambiente `finlumiadev` quando você quiser:

- padronizar o ambiente entre desenvolvedores;
- reduzir diferenças de versão entre máquinas locais;
- executar build Docker/Gradle em contexto controlado;
- evitar instalação manual de dependências na máquina host.

### Pré-requisitos

- Docker Desktop ativo;
- permissão para uso do Docker;
- arquivo de imagem base: `docker/bases/finlumia-dev-almalinux10.tar`.

### Opção A: usando script

Linux/macOS:

```bash
./finlumiadev.sh -up
```

Windows PowerShell:

```powershell
.\finlumiadev.ps1 -up
```

Fluxo recomendado:

1. subir ambiente (`-up`);
2. validar estado (`-status`);
3. entrar no shell (`-shell`) para build/testes;
4. acompanhar logs (`-logs`) quando necessário;
5. encerrar ambiente (`-down`) ao finalizar.

Comandos disponíveis:

- `-up`: sobe o ambiente;
- `-rebuild`: recria containers;
- `-shell`: entra no container (`bash` como usuário `finlumia`);
- `-down`: remove o ambiente;
- `-status`: lista status dos serviços;
- `-logs`: acompanha logs em tempo real.

Exemplos (bash ou PowerShell, mesmos flags):

```bash
./finlumiadev.sh -status
./finlumiadev.sh -shell
```

Validação automatizada dos scripts de build:

```bash
./scripts/validate-exec-scripts.sh
```

### Opção B: usando Docker Compose diretamente

```bash
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml ps
docker compose -f docker-compose.dev.yml down
```

### Verificação rápida pós-subida

- confirmar container `finlumiadev` em execução;
- testar acesso ao shell do container;
- validar se o diretório `/workspace` contém o código do projeto;
- confirmar se comandos Docker funcionam dentro do container (quando necessário build de imagens).

---

## 7) Rodar módulos sem container (execução Gradle local)

### Pré-requisitos

- JDK 21 instalado e configurado no `PATH`;
- PostgreSQL acessível com configurações compatíveis ao perfil;
- Gradle Wrapper do projeto (`gradlew`/`gradlew.bat`).

### Build completo de todos os módulos

Linux/macOS:

```bash
./gradlew build
```

Windows PowerShell:

```powershell
.\gradlew.bat build
```

### Subir módulo específico com perfil `dev`

Linux/macOS:

```bash
./gradlew :configurator:bootRun -Dspring.profiles.active=dev
```

Windows PowerShell:

```powershell
.\gradlew.bat :configurator:bootRun "-Dspring.profiles.active=dev"
```

### Quando preferir execução local sem container

- depuração com breakpoints diretamente na IDE;
- testes rápidos em um único módulo;
- ajustes pontuais de código com ciclo curto de feedback.

---

## 8) Build de imagem e subida de containers dos módulos

O script `finlumia.ps1` (Windows) e `finlumia.sh` (Linux/macOS) automatiza:

- build do `bootJar` por módulo;
- build da imagem Docker com Buildx;
- exportação da imagem para `.tar`;
- subida de container de teste quando usado modo `-t`.

### Diferença entre `-t` e `-c`

- `-t` (teste): ideal para validação funcional em container local, pois inicia container automaticamente.
- `-c` (distribuição): ideal para gerar artefato `.tar` da imagem sem executar container.

### Sintaxe principal

Windows:

```powershell
.\finlumia.ps1 <modulo>|-all -t|-c -dev|-pro [-s]
```

Linux/macOS:

```bash
./finlumia.sh <modulo>|-all -t|-c -dev|-pro [-s]
```

Parâmetros:

- `<modulo>`: `configurator`, `identify`, `movement` ou `docs`;
- `-all`: processa todos os módulos;
- `-t`: modo teste (sobe container automaticamente);
- `-c`: modo distribuição (gera artefato, sem subir container);
- `-dev` ou `-pro`: define perfil;
- `-s`: remove container de teste (somente com `-t`).

### Exemplos de uso

Subir container de teste do `configurator` em `dev`:

```powershell
.\finlumia.ps1 configurator -t -dev
```

Gerar artefatos de todos os módulos para distribuição em `pro`:

```powershell
.\finlumia.ps1 -all -c -pro
```

Remover container de teste do `configurator` em `dev`:

```powershell
.\finlumia.ps1 configurator -t -dev -s
```

### Nomes e saídas geradas

- imagem do módulo: `finlumia/<modulo>:latest`;
- container de teste: `test-<modulo>-<profile>`;
- arquivos `.tar`:
  - teste: `docker/test/<modulo>-<profile>.tar`;
  - distribuição: `docker/build/<modulo>-<profile>.tar`.

### Fluxo operacional recomendado para release técnica

1. gerar imagem em `-t -dev` e validar comportamento local;
2. revisar logs do container e endpoints críticos;
3. gerar artefato final com `-c -pro`;
4. armazenar/publicar artefato conforme processo interno da equipe.

---

## 9) Subida de containers de produção (módulos + banco de dados)

O deploy em produção (ex.: VPS Linux) usa `finlumia_backend.sh`. Diferente do `finlumia.sh` (seção 8), esse script **não builda a partir do código-fonte**: ele parte de artefatos já prontos (`.tar` dos módulos) e só builda a imagem do banco na própria VPS.

### Pré-requisitos

- Docker e `docker buildx` instalados na VPS;
- artefatos dos módulos gerados via `./finlumia.sh -all -c -pro` e copiados para `docker/build/<modulo>-pro.tar` na VPS (ver seção 8);
- um único arquivo de backup do banco (`*.backup`, formato `pg_dump` custom) em `docker/backup/`. O script aborta com erro se o diretório estiver vazio ou tiver mais de um arquivo;
- variável de ambiente `FINLUMIA_DB_PASS` exportada (obrigatória — o script aborta sem ela).

### Variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `FINLUMIA_DB_PASS` | sim | — | Senha do usuário do banco. |
| `FINLUMIABACK_HOME` | não | diretório do script | Raiz do projeto na VPS. |
| `FINLUMIA_DB_USER` | não | `papadopoulos` | Usuário do banco. |
| `FINLUMIA_DB_NAME` | não | `finlumia_transactions` | Nome do banco. |

> ⚠️ O Postgres só aplica `POSTGRES_PASSWORD` na **primeira inicialização** do volume de dados. Se `FINLUMIA_DB_PASS` mudar entre execuções de `bd` sem `-reset`, os módulos recebem a senha nova via `SPRING_DATASOURCE_PASSWORD`, mas o banco continua com a senha antiga gravada — a autenticação falha. Fixe `FINLUMIA_DB_PASS` em um local persistente na VPS (ex.: `/etc/environment`, fora do git) em vez de exportar manualmente a cada sessão.

### Configurar como comando global na VPS (opcional)

```bash
# ~/.bashrc
export FINLUMIABACK_HOME=/caminho/para/o/projeto
alias finlumiaback="$FINLUMIABACK_HOME/finlumia_backend.sh"
```

```bash
source ~/.bashrc
finlumiaback -all
```

### Ordem recomendada

Suba o banco antes dos módulos, pois eles dependem dele:

```bash
export FINLUMIA_DB_PASS=<senha-do-banco>
./finlumia_backend.sh bd
./finlumia_backend.sh -all
```

Ou um módulo por vez, acompanhando logs:

```bash
./finlumia_backend.sh identify -logs
```

### Comando `bd` (banco de dados)

```bash
./finlumia_backend.sh bd          # sobe/atualiza o container do banco, preservando dados existentes
./finlumia_backend.sh bd -reset   # apaga o volume de dados e restaura o backup do zero
```

O que o comando faz, em ordem:

1. valida que existe **exatamente um** arquivo `*.backup` em `docker/backup/` (erro e aborta se faltar ou se houver mais de um);
2. remove o container antigo do banco (a imagem é reconstruída a cada execução);
3. com `-reset`, remove também o volume `finlumia-postgres-data` — **isso apaga os dados atuais**;
4. builda a imagem `finlumia/db:postgres18` a partir de `docker/scripts/dbfinlumia.Dockerfile`;
5. sobe o container `finlumiadb`, montando o volume de dados e o diretório `docker/backup/` (somente leitura) em `/docker-entrypoint-initdb.d/backup`;
6. o Postgres restaura o backup **apenas se o volume estiver vazio** (primeira vez, ou logo após `-reset`). Se o volume já tem dados, o restore é pulado e os dados existentes são preservados;
7. aguarda o healthcheck (`pg_isready`) reportar `healthy` (até ~60s).

O banco fica acessível somente em `127.0.0.1:28079` (não exposto publicamente); use túnel SSH para acessar com uma ferramenta de administração externa.

### Comandos de módulo (`configurator`, `identify`, `movement`, `docs`)

```bash
./finlumia_backend.sh <modulo>        # sobe um módulo específico
./finlumia_backend.sh <modulo> -logs  # sobe e acompanha os logs em seguida
./finlumia_backend.sh -all            # sobe todos os módulos
```

O que acontece internamente, por módulo:

1. exige que `docker/build/<modulo>-pro.tar` já exista (gerado na seção 8 com `./finlumia.sh <modulo> -c -pro`);
2. remove container e imagem antigos do módulo;
3. carrega o `.tar` (`docker load`) e sobe o container, ligado à rede `finlumia-net`;
4. injeta `SPRING_PROFILES_ACTIVE=pro` e, para módulos com banco, `SPRING_DATASOURCE_*` apontando para `finlumiadb:5432` (rede interna, não a porta publicada no host);
5. o módulo `docs` recebe as URLs internas dos demais módulos (`DOCS_MODULES_BASE_URL_*`) em vez de datasource.

Cada módulo fica acessível somente em `127.0.0.1:<porta>` (ver tabela da seção 1).

### Backup do banco (`docker/backup/`)

- deve conter **um único** arquivo `*.backup` (pg_dump formato custom);
- **nunca commitar** esse arquivo — `docker/backup/*.backup` está no `.gitignore`; transfira-o para a VPS por fora do git (`scp`, `rsync`, etc.);
- para trocar de backup, substitua o arquivo antes de rodar `bd -reset` (sem `-reset`, o restore não roda de novo em um volume já existente).

---

## 10) Sequência recomendada para onboarding técnico

1. Subir ambiente dev (`finlumiadev.ps1 -up`).
2. Entrar no container (`finlumiadev.ps1 -shell`), quando necessário.
3. Executar build (`gradlew build`) e validar compilação.
4. Subir um módulo em `-t -dev` para validar endpoint.
5. Subir `docs` para validar se os `api-docs` dos módulos estão acessíveis.
6. Acompanhar logs do módulo em execução.

---

## 11) Troubleshooting rápido

- erro de imagem base ausente: validar arquivos em `docker/bases/*.tar`;
- erro de porta em uso: verificar processos ocupando `40570-40574`;
- erro de docs sem endpoints: confirmar se os módulos referenciados estão ativos;
- erro de banco: revisar variáveis/propriedades de conexão do perfil em uso;
- erro no Docker dentro do dev container: validar montagem de `/var/run/docker.sock`;
- `ERRO: nenhum arquivo .backup encontrado em docker/backup` (produção): copiar o backup para `docker/backup/` na VPS antes de rodar `finlumia_backend.sh bd`;
- `ERRO: encontrado mais de um arquivo .backup` (produção): manter apenas um arquivo `*.backup` no diretório;
- módulo de produção falhando autenticação no banco: conferir se `FINLUMIA_DB_PASS` é o mesmo valor usado na primeira inicialização do volume (ver seção 9).

### Diagnóstico sugerido (ordem prática)

1. Validar se o container/serviço subiu (`status`/`ps`).
2. Validar logs (`logs`) e identificar o primeiro erro relevante.
3. Validar portas ocupadas e conflitos locais.
4. Validar perfil ativo (`dev` ou `pro`) e variáveis associadas.
5. Reexecutar build limpo após correção.

---

## 12) Boas práticas de manutenção

### Antes de codar

- ler `controller`, `service` e `repository` do fluxo impactado;
- identificar se a responsabilidade é local do módulo ou compartilhada;
- validar impacto em request/response e em contratos de consumidores.

### Durante a implementação

- manter separação de responsabilidades por camada;
- validar entradas com anotações (`@NotNull`, `@NotBlank`, etc.);
- evitar SQL concatenado com dados de usuário;
- padronizar erros com exceções de domínio e handler global.

### Antes de abrir PR

- executar build local;
- testar endpoints alterados (Postman/Insomnia/cURL);
- revisar logs e mensagens de erro;
- atualizar este README em caso de alteração de fluxo operacional.

---

## 13) Segurança: pontos obrigatórios

- não versionar credenciais reais em `application*.properties`;
- priorizar variáveis de ambiente para usuário, senha e chaves;
- restringir documentação interna em ambientes produtivos;
- revisar autenticação/autorização ao expor novas rotas;
- não confiar em headers do cliente sem validação de contexto;
- nunca versionar backups reais do banco (`docker/backup/*.backup`) — usar apenas para restore local/VPS, sempre fora do git.

Ao criar endpoints em `/api/**`, validar explicitamente o tratamento de `keyUser` e os critérios de autorização associados.

---

## 14) Alinhamento técnico

Em caso de dúvida de implementação:

- consultar primeiro este README e o módulo `shared`;
- alinhar com o responsável técnico do domínio;
- registrar decisões arquiteturais relevantes para facilitar o onboarding futuro.
