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
- [9) Sequência recomendada para onboarding técnico](#9-sequência-recomendada-para-onboarding-técnico)
- [10) Troubleshooting rápido](#10-troubleshooting-rápido)
- [11) Boas práticas de manutenção](#11-boas-práticas-de-manutenção)
- [12) Segurança: pontos obrigatórios](#12-segurança-pontos-obrigatórios)
- [13) Alinhamento técnico](#13-alinhamento-técnico)

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

## 9) Sequência recomendada para onboarding técnico

1. Subir ambiente dev (`finlumiadev.ps1 -up`).
2. Entrar no container (`finlumiadev.ps1 -shell`), quando necessário.
3. Executar build (`gradlew build`) e validar compilação.
4. Subir um módulo em `-t -dev` para validar endpoint.
5. Subir `docs` para validar se os `api-docs` dos módulos estão acessíveis.
6. Acompanhar logs do módulo em execução.

---

## 10) Troubleshooting rápido

- erro de imagem base ausente: validar arquivos em `docker/bases/*.tar`;
- erro de porta em uso: verificar processos ocupando `40570-40574`;
- erro de docs sem endpoints: confirmar se os módulos referenciados estão ativos;
- erro de banco: revisar variáveis/propriedades de conexão do perfil em uso;
- erro no Docker dentro do dev container: validar montagem de `/var/run/docker.sock`.

### Diagnóstico sugerido (ordem prática)

1. Validar se o container/serviço subiu (`status`/`ps`).
2. Validar logs (`logs`) e identificar o primeiro erro relevante.
3. Validar portas ocupadas e conflitos locais.
4. Validar perfil ativo (`dev` ou `pro`) e variáveis associadas.
5. Reexecutar build limpo após correção.

---

## 11) Boas práticas de manutenção

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

## 12) Segurança: pontos obrigatórios

- não versionar credenciais reais em `application*.properties`;
- priorizar variáveis de ambiente para usuário, senha e chaves;
- restringir documentação interna em ambientes produtivos;
- revisar autenticação/autorização ao expor novas rotas;
- não confiar em headers do cliente sem validação de contexto.

Ao criar endpoints em `/api/**`, validar explicitamente o tratamento de `keyUser` e os critérios de autorização associados.

---

## 13) Alinhamento técnico

Em caso de dúvida de implementação:

- consultar primeiro este README e o módulo `shared`;
- alinhar com o responsável técnico do domínio;
- registrar decisões arquiteturais relevantes para facilitar o onboarding futuro.
