# =============================================================================
# dbfinlumia.Dockerfile
# -----------------------------------------------------------------------------
# PostgreSQL 18 com restauracao automatica a partir de um backup (*.backup,
# formato custom pg_dump / PGDMP) montado em /docker-entrypoint-initdb.d/backup
# no momento da criacao do container (ver finlumia_backend.sh e
# docker-compose.dev.yml). O backup NAO e copiado para dentro da imagem.
#
# O restore roda apenas na primeira inicializacao do volume de dados, via
# /docker-entrypoint-initdb.d/, conforme o entrypoint oficial da imagem.
# =============================================================================

FROM postgres:18

LABEL maintainer="finlumia"
LABEL description="PostgreSQL 18 com restore inicial a partir de backup montado em runtime"

COPY docker/scripts/db-restore.sh /docker-entrypoint-initdb.d/01-restore-finlumia_transactions.sh
COPY docker/scripts/db-harden-security.sh /docker-entrypoint-initdb.d/02-harden-security.sh

RUN chmod +x /docker-entrypoint-initdb.d/01-restore-finlumia_transactions.sh \
    && chmod +x /docker-entrypoint-initdb.d/02-harden-security.sh

EXPOSE 5432
