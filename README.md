# Finlumia Backend

Backend da plataforma Finlumia, organizado como monorepo Gradle com módulos Spring Boot.

Este documento foi estruturado para apoiar onboarding, manutenção e evolução do projeto com segurança, cobrindo:

- visão arquitetural e responsabilidades por módulo;
- fluxo de chamadas HTTP e camadas internas;
- subida do ambiente de desenvolvimento;
- build, empacotamento e subida de containers;
- migrations de banco de dados;
- sistema de suporte (tickets) com anexos via MinIO e conversão de vídeo via FFmpeg;
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
- [10) Migrations do banco de dados](#10-migrations-do-banco-de-dados)
- [11) Sistema de suporte: anexos com MinIO e FFmpeg](#11-sistema-de-suporte-anexos-com-minio-e-ffmpeg)
- [12) Sequência recomendada para onboarding técnico](#12-sequência-recomendada-para-onboarding-técnico)
- [13) Troubleshooting rápido](#13-troubleshooting-rápido)
- [14) Boas práticas de manutenção](#14-boas-práticas-de-manutenção)
- [15) Segurança: pontos obrigatórios](#15-segurança-pontos-obrigatórios)
- [16) Alinhamento técnico](#16-alinhamento-técnico)

---

## 1) Visão geral do repositório

Este repositório concentra serviços de backend e bibliotecas compartilhadas.

### Módulos atuais (arquivo `settings.gradle`)

Monorepo de **microserviços** com **banco monolítico** (PostgreSQL único; cada módulo = schema homônimo).

| Módulo | Executável | Schema DB | Porta dev |
|--------|------------|-----------|-----------|
| `configurator` | sim | `configurator` | 28081 |
| `docs` | sim | `docs` (+ sistema de suporte/tickets) | 28082 |
| `identify` | sim | `identify` | 28083 |
| `movement` | sim | `movement` | 28084 |
| `document` | sim | — | 28085 |
| `shared` | **não** (biblioteca) | — | — |

- `shared`: apenas classes genéricas (`FinlumiaException`, `DialogDefault`, `GlobalExceptionHandler`) e `shared.properties` (datasource comum).
- Demais módulos: segurança, interceptors, OpenAPI e JDBC ficam **no próprio módulo**.
- `docs` acumula duas responsabilidades: agregação de Swagger dos demais módulos **e** o sistema de suporte/helpdesk (tickets, respostas, anexos de foto/vídeo/documento) — ver [seção 11](#11-sistema-de-suporte-anexos-com-minio-e-ffmpeg). Por causa disso, é o único módulo com dependência externa em um serviço de object storage (MinIO) além do Postgres.

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

- regra ou endpoint de um domínio: no módulo do domínio (`configurator`, `identify`, `movement`, `document`);
- contrato HTTP genérico de erro: `shared`;
- agregação Swagger e sistema de suporte/tickets: `docs`.

### Stack técnica

- Java 21
- Spring Boot 4.0.5
- Gradle multi-módulo
- PostgreSQL (acesso JDBC)
- Lombok
- Docker/Buildx para empacotamento e execução em container
- MinIO (S3-compatível) para storage de anexos do sistema de suporte (módulo `docs`)
- FFmpeg (conversão assíncrona de vídeo, embutido na imagem do módulo `docs`)

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

O módulo `docs` centraliza visualização de APIs em `http://localhost:28082/docs/swagger-ui.html`.

Ele consulta os endpoints de documentação de cada serviço:

- `configurator`: `http://localhost:28081/internal/docs/api-docs`
- `identify`: `http://localhost:28083/internal/docs/api-docs`
- `movement`: `http://localhost:28084/internal/docs/api-docs`
- `docs`: `http://localhost:28082/v3/api-docs`

Se algum serviço não subir, o Swagger agregador pode apresentar falhas parciais de carregamento.

### Dependência operacional do módulo `docs`

Para visualizar toda a documentação agregada, o `docs` depende de:

- `docs` ativo na porta `28082`;
- módulos de negócio ativos nas portas esperadas (`28081`, `28083`, `28084`);
- endpoints de docs internos acessíveis (`/internal/docs/api-docs`).

Se um módulo estiver fora do ar, o Swagger pode continuar abrindo, mas com falha no carregamento da API específica.

---

## 5) Portas e perfis locais

Cada aplicação roda de forma independente:

- `configurator`: `28081`
- `docs`: `28082`
- `identify`: `28083`
- `movement`: `28084`
- `document`: `28085`
- banco de dados (Postgres): `28079`
- MinIO — API S3 / console web: `9000` / `9001`
- ambiente de desenvolvimento (`dev container`): `40570`

Os perfis suportados pelos scripts são:

- `local`: desenvolvimento na sua máquina (segredos em `shared-local.properties`);
- `hom`: homologação (segredos em `shared-hom.properties`);
- `pro`: produção (segredos em `shared-pro.properties`).

Hoje `hom` e `pro` apontam para a mesma VPS/banco — os dois arquivos devem ficar com os mesmos valores até existir infraestrutura separada.

---

## 6) Subida do ambiente de desenvolvimento (container de desenvolvimento)

O ambiente de desenvolvimento usa `docker-compose.dev.yml`, com:

- container `finlumiadev` (build/Gradle);
- container `finlumiadb` (Postgres);
- container `finlumia-minio` (storage de anexos do sistema de suporte);
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
- arquivo de imagem base: `docker/bases/finlumia-dev-almalinux10.tar`;
- arquivo `.env` na raiz do projeto (não versionado — ver abaixo).

### Variáveis de ambiente (`.env`)

O `docker-compose.dev.yml` lê um `.env` local (fora do git) com, no mínimo:

```dotenv
DB_USERNAME=papadopoulos
DB_PASSWORD=<senha do banco local>
DB_NAME=finlumia_transactions
MINIO_ROOT_USER=finlumia-storage
MINIO_ROOT_PASSWORD=<senha do MinIO local>
```

Sem `MINIO_ROOT_USER`/`MINIO_ROOT_PASSWORD`, o serviço `minio` do compose não sobe.

### Opção A: usando script

Linux/macOS:

```bash
./finlumiadev.sh -up
```

Windows PowerShell:

```powershell
.\finlumiadev.ps1 -up
```

`-up`/`-rebuild` sobem apenas `db` e `dev` — não incluem `docs` nem `minio` (evita builds acoplados no primeiro `up`; ver [seção 11](#11-sistema-de-suporte-anexos-com-minio-e-ffmpeg) para subir o sistema de suporte completo).

Fluxo recomendado:

1. subir ambiente (`-up`);
2. validar estado (`-status`);
3. entrar no shell (`-shell`) para build/testes;
4. acompanhar logs (`-logs`) quando necessário;
5. encerrar ambiente (`-down`) ao finalizar.

Comandos disponíveis:

- `-up`: sobe `db` e `dev`;
- `-rebuild`: recria `db` e `dev`;
- `-shell`: entra no container (`bash` como usuário `finlumia`);
- `-down`: remove o ambiente;
- `-status`: lista status dos serviços;
- `-logs`: acompanha logs em tempo real (do container `dev`).

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
docker compose -f docker-compose.dev.yml up -d          # sobe todos os serviços, incluindo docs e minio
docker compose -f docker-compose.dev.yml up -d db dev    # só banco + container de dev
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
.\finlumia.ps1 <modulo>|-all -t|-c -local|-hom|-pro [-s]
```

Linux/macOS:

```bash
./finlumia.sh <modulo>|-all -t|-c -local|-hom|-pro [-s]
```

Parâmetros:

- `<modulo>`: `configurator`, `identify`, `movement`, `docs` ou `document`;
- `-all`: processa todos os módulos;
- `-t`: modo teste (sobe container automaticamente);
- `-c`: modo distribuição (gera artefato, sem subir container);
- `-local`, `-hom` ou `-pro`: define o profile (segredos vêm de `shared-<profile>.properties`, ver seção 7);
- `-s`: remove container de teste (somente com `-t`).

### Exemplos de uso

Subir container de teste do `configurator` em `local`:

```powershell
.\finlumia.ps1 configurator -t -local
```

Gerar artefatos de todos os módulos para distribuição em `pro`:

```powershell
.\finlumia.ps1 -all -c -pro
```

Remover container de teste do `configurator` em `local`:

```powershell
.\finlumia.ps1 configurator -t -local -s
```

### Nomes e saídas geradas

- imagem do módulo: `finlumia/<modulo>:<profile>`;
- container de teste: `test-<modulo>-<profile>`;
- arquivos `.tar`:
  - teste: `docker/test/<modulo>-<profile>.tar`;
  - distribuição: `docker/build/<modulo>-<profile>.tar`.

### Fluxo operacional recomendado para release técnica

1. gerar imagem em `-t -local` e validar comportamento local;
2. revisar logs do container e endpoints críticos;
3. gerar artefato final com `-c -hom` (ou `-c -pro`);
4. armazenar/publicar artefato conforme processo interno da equipe.

---

## 9) Subida de containers de produção (módulos + banco de dados)

O deploy em produção (ex.: VPS Linux) usa `finlumia_backend.sh`. Diferente do `finlumia.sh` (seção 8), esse script **não builda a partir do código-fonte**: ele parte de artefatos já prontos (`.tar` dos módulos) e só builda a imagem do banco na própria VPS. Não há `docker-compose` em homologação/produção — tudo roda via `docker run` direto, orquestrado por esse script.

### Pré-requisitos

- Docker e `docker buildx` instalados na VPS;
- artefatos dos módulos gerados via `./finlumia.sh -all -c -hom` (ou `-pro`) e copiados para `docker/build/<modulo>-<profile>.tar` na VPS (ver seção 8);
- um único arquivo de backup do banco (`*.backup`, formato `pg_dump` custom) em `docker/backup/`. O script aborta com erro se o diretório estiver vazio ou tiver mais de um arquivo;
- variável de ambiente `FINLUMIA_DB_PASS` exportada (obrigatória — o script aborta sem ela).

### Variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `FINLUMIA_DB_PASS` | sim | — | Senha do usuário do banco. |
| `FINLUMIABACK_HOME` | não | diretório do script | Raiz do projeto na VPS. |
| `FINLUMIA_DB_USER` | não | `papadopoulos` | Usuário do banco. |
| `FINLUMIA_DB_NAME` | não | `finlumia_transactions` | Nome do banco. |
| `FINLUMIA_MINIO_USER` | não | `finlumia-storage` | Usuário admin do MinIO. |
| `FINLUMIA_MINIO_PASS` | sim, pro `minio` e pro `docs` | — | Senha do MinIO. |
| `FINLUMIA_MINIO_PUBLIC_ENDPOINT` | sim, pro `docs` | — | URL pública do MinIO (ex.: `https://apifinlumia-storage.<dominio>`), usada para assinar URLs de upload/download. |
| `FINLUMIA_MINIO_CORS_ORIGIN` | não | `https://finlumia.thiagobenevide.com` | Origin liberada no CORS do servidor MinIO. |

> ⚠️ O Postgres só aplica `POSTGRES_PASSWORD` na **primeira inicialização** do volume de dados. Se `FINLUMIA_DB_PASS` mudar entre execuções de `bd` sem `-reset`, os módulos recebem a senha nova via `SPRING_DATASOURCE_PASSWORD`, mas o banco continua com a senha antiga gravada — a autenticação falha. Fixe `FINLUMIA_DB_PASS` (e `FINLUMIA_MINIO_PASS`) em um local persistente na VPS (ex.: `/etc/environment`, fora do git) em vez de exportar manualmente a cada sessão. O mesmo vale pro `MINIO_ROOT_PASSWORD` do MinIO.

### Configurar como comando global na VPS (opcional)

```bash
# ~/.bashrc
export FINLUMIABACK_HOME=/caminho/para/o/projeto
alias finlumiaback="$FINLUMIABACK_HOME/finlumia_backend.sh"
```

```bash
source ~/.bashrc
finlumiaback -all -pro
```

### Ordem recomendada

Suba o banco e o MinIO antes dos módulos, pois `docs` depende dos dois:

```bash
export FINLUMIA_DB_PASS=<senha-do-banco>
export FINLUMIA_MINIO_PASS=<senha-do-minio>
export FINLUMIA_MINIO_PUBLIC_ENDPOINT=https://apifinlumia-storage.<dominio>
./finlumia_backend.sh bd
./finlumia_backend.sh minio
./finlumia_backend.sh -all -pro
```

Ou um módulo por vez, acompanhando logs:

```bash
./finlumia_backend.sh identify -pro -logs
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

**Importante**: o backup restaurado reflete o estado do banco no momento em que foi gerado. Alterações de schema feitas depois (novas migrations em `docker/bases/migrations/`) **não** são reaplicadas automaticamente — ver [seção 10](#10-migrations-do-banco-de-dados).

### Comando `minio` (storage de anexos)

```bash
./finlumia_backend.sh minio
```

Sobe o container `finlumia-minio`, com volume nomeado `finlumia-minio-data` (persistente) e portas `9000`/`9001` publicadas só em `127.0.0.1` — o acesso externo passa pelo nginx (ver [seção 11](#11-sistema-de-suporte-anexos-com-minio-e-ffmpeg)). Exige `FINLUMIA_MINIO_PASS`.

### Comandos de módulo (`configurator`, `identify`, `movement`, `docs`, `document`)

```bash
./finlumia_backend.sh <modulo>        # sobe um módulo específico
./finlumia_backend.sh <modulo> -logs  # sobe e acompanha os logs em seguida
./finlumia_backend.sh -all            # sobe todos os módulos
```

O que acontece internamente, por módulo:

1. exige que `docker/build/<modulo>-<profile>.tar` já exista (gerado na seção 8 com `./finlumia.sh <modulo> -c -hom|-pro`);
2. remove container e imagem antigos do módulo;
3. carrega o `.tar` (`docker load`) e sobe o container, ligado à rede `finlumia-net`;
4. injeta `SPRING_PROFILES_ACTIVE=<hom|pro>` e `SPRING_DATASOURCE_*` apontando para `finlumiadb:5432` (rede interna, não a porta publicada no host);
5. o módulo `docs` recebe adicionalmente as URLs internas dos demais módulos (`DOCS_MODULES_BASE_URL_*`) e a configuração do MinIO (`MINIO_*`, ver seção 11).

Cada módulo fica acessível somente em `127.0.0.1:<porta>` (ver tabela da seção 5).

### Backup do banco (`docker/backup/`)

- deve conter **um único** arquivo `*.backup` (pg_dump formato custom);
- **nunca commitar** esse arquivo — `docker/backup/*.backup` está no `.gitignore`; transfira-o para a VPS por fora do git (`scp`, `rsync`, etc.);
- para trocar de backup, substitua o arquivo antes de rodar `bd -reset` (sem `-reset`, o restore não roda de novo em um volume já existente).

---

## 10) Migrations do banco de dados

Os scripts SQL em `docker/bases/migrations/` documentam a evolução incremental do schema, numerados sequencialmente (`000_setup_completo.sql` define a base completa; os demais são incrementais).

### Como (não) são aplicadas

**Não existe Flyway/Liquibase neste projeto.** As migrations em `docker/bases/migrations/` **não rodam automaticamente** — elas são um registro versionado, aplicado manualmente. O que roda automaticamente no `docker-entrypoint-initdb.d/` do container do banco (só na **primeira** inicialização de um volume vazio) é:

1. `db-restore.sh`: restaura o `*.backup` (`docker/backup/`, ver seção 9) — esse backup já reflete o schema completo até a última migration aplicada no momento em que foi gerado;
2. `db-harden-security.sh`: remove a role `postgres` padrão e ajusta permissões.

Ou seja: **um banco novo (volume vazio) já nasce atualizado**, via backup. Um banco **já existente** (dev que você já usa há tempo, ou a VPS em produção) precisa da migration nova aplicada manualmente.

### Aplicando uma migration manualmente

Mesmo comando em dev e produção (o container do banco se chama `finlumiadb` nos dois ambientes):

```bash
docker exec -i finlumiadb psql -U papadopoulos -d finlumia_transactions -v ON_ERROR_STOP=1 \
  < docker/bases/migrations/0XX_nome_da_migration.sql
```

### Convenções ao escrever uma nova migration

- nomear como `0XX_descricao_curta.sql`, incrementando a partir do maior número existente;
- envolver criação/alteração de tabela com o padrão de disable/enable do event trigger `trg_add_default_columns` (ver qualquer migration existente como referência);
- usar `CREATE TABLE IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`, `DROP CONSTRAINT IF EXISTS` — migrations devem ser seguras de rodar mais de uma vez;
- depois de aplicar em produção, **atualizar o backup em `docker/backup/`** (novo `pg_dump`) para que próximas inicializações de volume zerado já nasçam com o schema atualizado.

### Lista atual (referência rápida)

| Migration | Conteúdo |
|---|---|
| `000_setup_completo.sql` | Schema base completo (todos os módulos) |
| `001`–`003` | `identify`: autenticação, RBAC, perfil |
| `004` | `movement`: schema base |
| `005`, `007` | `configurator`: schema e seed |
| `006` | Usuário administrador inicial |
| `008` | `docs`: schema de suporte (tickets, respostas, anexos) |
| `009`, `012`, `014` | `movement`: ajustes incrementais (UUID, import, orçamentos) |
| `010`, `011`, `013` | `identify`: proteção brute-force, refresh token, verificação de e-mail |
| `015` | `docs.ticket_attachments`: migração de disco local para object storage (MinIO) |

---

## 11) Sistema de suporte: anexos com MinIO e FFmpeg

O módulo `docs` implementa um helpdesk de suporte (abertura de ticket, respostas, anexos) sob `/api/v1/support/**`, com schema `docs.*` (`tickets`, `ticket_responses`, `ticket_attachments`).

### Armazenamento de anexos (fotos, vídeos, documentos)

Anexos **não** passam pelo servidor de aplicação — o upload é feito via URL pré-assinada, direto do navegador para o **MinIO** (object storage self-hosted, compatível com API S3):

1. `POST /api/v1/support/tickets/{ticketId}/attachments/presign` — valida tipo/tamanho, devolve uma URL de upload assinada (TTL curto).
2. Cliente faz `PUT` do arquivo direto nessa URL.
3. `POST /api/v1/support/tickets/{ticketId}/attachments/{attachmentId}/complete` — confirma o upload (verifica no MinIO) e grava o registro no banco.
4. Download/miniatura: `GET .../attachments/{id}/download` e `.../thumbnail` devolvem `{"url": "..."}` (JSON, `200`) com uma URL assinada de leitura gerada na hora — **não** é redirect `302`. Isso é proposital: como esses endpoints exigem JWT, e `<video>`/`<img>`/`<a>` nativos do navegador não enviam o header `Authorization`, o cliente precisa chamar via `fetch()` autenticado e só then usar a URL devolvida (essa sim, sem token, já assinada) como `src`/link.

Limites de tipo/tamanho (validados no `TicketAttachmentService`):

| Categoria | MIME aceitos | Tamanho máximo |
|---|---|---|
| Imagem / documento | `image/png`, `image/jpeg`, `image/webp`, `application/pdf`, `text/plain`, `text/csv` | 10MB |
| Vídeo | `video/mp4`, `video/quicktime`, `video/webm` | 100MB (limitado pelo teto de upload do proxy Cloudflare em produção — plano Free/Pro trava em 100MB) |

### Conversão de vídeo (assíncrona)

Vídeos são processados em background por um `ThreadPoolTaskExecutor` dedicado (`core=1, max=2` — evita saturar a VPS, que também roda o Postgres e os demais módulos):

1. Baixa o bruto do MinIO para um diretório temporário do container.
2. Gera miniatura (`ffmpeg -vframes 1`).
3. Converte para MP4/H.264/720p (`-preset veryfast -crf 26 -movflags +faststart`) — leve e universalmente compatível com navegador.
4. Sobe convertido + miniatura de volta pro MinIO; atualiza `conversion_status` no banco.

Estados de `conversion_status`: `not_applicable` (não é vídeo), `pending`, `processing`, `completed`, `failed`.

**Regra de exibição**: o usuário final **nunca** recebe o vídeo bruto para reprodução — só o convertido (`conversion_status = completed`). Se a conversão falhar, apenas `admin`/`gerente` conseguem baixar o bruto (para reprocessar/auditar); usuário comum recebe `409`.

### Variáveis de ambiente do módulo `docs`

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| `MINIO_INTERNAL_ENDPOINT` | `http://localhost:9000` | Endpoint usado pela aplicação para operações administrativas (criação de bucket, download para conversão) — rede interna Docker em produção. |
| `MINIO_PUBLIC_ENDPOINT` | `http://localhost:9000` | Endpoint usado para **assinar** URLs de upload/download — precisa ser alcançável pelo navegador do usuário. |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | — | Credenciais do MinIO. |
| `MINIO_BUCKET` | `finlumia-support-attachments` | Bucket usado. |
| `SUPPORT_VIDEO_MAX_SIZE_BYTES` | `104857600` (100MB) | Cap de tamanho de vídeo. |
| `FFMPEG_BIN` | `/usr/local/bin/ffmpeg` | Caminho do binário FFmpeg (embutido na imagem via `docker/scripts/docs.Dockerfile`, multi-stage a partir de `mwader/static-ffmpeg`). |

### Subida local (dev)

`docker-compose.dev.yml` já inclui o serviço `minio` com as variáveis acima pré-configuradas para `localhost`. Console web: `http://localhost:9001` (login com `MINIO_ROOT_USER`/`MINIO_ROOT_PASSWORD` do `.env`, ver seção 6).

### Deploy em produção

```bash
export FINLUMIA_MINIO_PASS=<senha>
export FINLUMIA_MINIO_PUBLIC_ENDPOINT=https://apifinlumia-storage.<dominio>
./finlumia_backend.sh minio                 # primeira vez / sempre que precisar recriar
# aplicar migration 015 (ver seção 10) se o banco já existia antes dessa feature
./finlumia.sh docs -c -pro                  # gera o jar/tar do docs com FFmpeg embutido
./finlumia_backend.sh docs -pro
```

### nginx em produção (topologia real da VPS)

⚠️ O nginx **não** roda em container neste projeto — é um serviço nativo (`systemctl`) na VPS, com **um subdomínio por módulo** em `/etc/nginx/sites-enabled/` (ex.: `apifinlumia-docs.<dominio>` → `proxy_pass http://127.0.0.1:28082`), certificado único multi-SAN via `certbot` (webroot em `/var/www/html`) cobrindo todos os subdomínios. **Os arquivos em `docker/nginx/` deste repositório não refletem essa topologia** — são um template de referência (routing por path sob um único domínio) usado apenas como exemplo/dev; a config real da VPS é mantida manualmente, fora do git.

O storage de anexos segue o mesmo padrão: subdomínio próprio (ex.: `apifinlumia-storage.<dominio>`) proxiando para `127.0.0.1:9000`, com `proxy_buffering off` e `proxy_request_buffering off` (essenciais — sem isso o nginx bufferiza o arquivo inteiro antes de repassar, reintroduzindo o problema de RAM que a URL assinada evita). Pra adicionar esse subdomínio a um certificado multi-SAN existente:

```bash
sudo certbot certificates                       # confirma a lista exata de domínios já no certificado
sudo certbot certonly --cert-name <nome-do-certificado> --expand \
  -d <dominio-1> -d <dominio-2> ... \
  -d apifinlumia-storage.<dominio>
```

Se o subdomínio de storage ficar atrás de um proxy/CDN (ex.: Cloudflare), respeitar o teto de tamanho de corpo de requisição do plano contratado — motivo do limite de vídeo estar em 100MB (ver tabela acima).

---

## 12) Sequência recomendada para onboarding técnico

1. Subir ambiente dev (`finlumiadev.ps1 -up`).
2. Entrar no container (`finlumiadev.ps1 -shell`), quando necessário.
3. Executar build (`gradlew build`) e validar compilação.
4. Subir um módulo em `-t -local` para validar endpoint.
5. Subir `docs` (`docker compose -f docker-compose.dev.yml up -d docs minio`) para validar tickets/anexos e se os `api-docs` dos módulos estão acessíveis.
6. Acompanhar logs do módulo em execução.

---

## 13) Troubleshooting rápido

- erro de imagem base ausente: validar arquivos em `docker/bases/*.tar`;
- erro de porta em uso: verificar processos ocupando `28079`–`28085`, `9000`/`9001` ou `40570`;
- erro de docs sem endpoints: confirmar se os módulos referenciados estão ativos;
- erro de banco: revisar variáveis/propriedades de conexão do perfil em uso;
- erro no Docker dentro do dev container: validar montagem de `/var/run/docker.sock`;
- `ERRO: nenhum arquivo .backup encontrado em docker/backup` (produção): copiar o backup para `docker/backup/` na VPS antes de rodar `finlumia_backend.sh bd`;
- `ERRO: encontrado mais de um arquivo .backup` (produção): manter apenas um arquivo `*.backup` no diretório;
- módulo de produção falhando autenticação no banco: conferir se `FINLUMIA_DB_PASS` é o mesmo valor usado na primeira inicialização do volume (ver seção 9);
- feature nova de banco funcionando local mas dando erro de "relation does not exist" na VPS: falta aplicar a migration manualmente (ver seção 10) — o backup restaurado só tem o schema até a última migration aplicada no momento em que foi gerado;
- MinIO retorna `501 not implemented` ao tentar configurar CORS por bucket: esperado — o MinIO não implementa `PutBucketCors` por bucket via API; CORS é configurado globalmente via variável de ambiente `MINIO_API_CORS_ALLOW_ORIGIN` do próprio servidor;
- container `docs` reiniciando em loop com `UnknownHostException: minio`: geralmente o container foi criado antes de entrar na rede Docker corretamente — remova e recrie (`docker rm -f finlumia-docs && docker compose -f docker-compose.dev.yml up -d docs`);
- vídeo não sai de `conversion_status=pending`/`failed`: conferir `docker logs <container-docs>` por `VIDEO_CONVERSION_FAILURE`; confirmar que `ffmpeg -version` funciona dentro do container (binário embutido via `docs.Dockerfile`);
- `nginx -t` reportando `Permission denied` ao ler certificado: normal se rodado sem `sudo` (o teste manual sem privilégio não lê arquivos do Let's Encrypt) — rode `sudo nginx -t`; o serviço real (`systemctl`) já roda como root.

### Diagnóstico sugerido (ordem prática)

1. Validar se o container/serviço subiu (`status`/`ps`).
2. Validar logs (`logs`) e identificar o primeiro erro relevante.
3. Validar portas ocupadas e conflitos locais.
4. Validar perfil ativo (`local`, `hom` ou `pro`) e variáveis associadas.
5. Reexecutar build limpo após correção.

---

## 14) Boas práticas de manutenção

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
- atualizar este README em caso de alteração de fluxo operacional;
- se a mudança envolve schema de banco, adicionar migration nova (não editar uma existente) — ver seção 10.

---

## 15) Segurança: pontos obrigatórios

- não versionar credenciais reais em `application*.properties`;
- priorizar variáveis de ambiente para usuário, senha e chaves;
- restringir documentação interna em ambientes produtivos;
- revisar autenticação/autorização ao expor novas rotas;
- não confiar em headers do cliente sem validação de contexto;
- nunca versionar backups reais do banco (`docker/backup/*.backup`) — usar apenas para restore local/VPS, sempre fora do git;
- nunca versionar o `.env` local (dev) nem credenciais do MinIO/banco em texto plano fora de variáveis de ambiente.

Ao criar endpoints em `/api/**`, validar explicitamente o tratamento de `keyUser` e os critérios de autorização associados.

---

## 16) Alinhamento técnico

Em caso de dúvida de implementação:

- consultar primeiro este README e o módulo `shared`;
- alinhar com o responsável técnico do domínio;
- registrar decisões arquiteturais relevantes para facilitar o onboarding futuro.
