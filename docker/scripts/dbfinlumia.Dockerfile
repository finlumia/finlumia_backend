# =============================================================================
# dbfinlumia.Dockerfile
# -----------------------------------------------------------------------------
# PostgreSQL 18 com restauracao automatica do backup base em
# docker/bases/finlumia_transactions (formato custom pg_dump / PGDMP).
#
# O restore roda apenas na primeira inicializacao do volume de dados, via
# /docker-entrypoint-initdb.d/, conforme o entrypoint oficial da imagem.
# =============================================================================

FROM postgres:18

LABEL maintainer="finlumia"
LABEL description="PostgreSQL 18 com restore inicial do backup finlumia_transactions"

COPY docker/bases/finlumia_transactions /docker-entrypoint-initdb.d/finlumia_transactions.dump
COPY docker/scripts/db-restore.sh /docker-entrypoint-initdb.d/01-restore-finlumia_transactions.sh
COPY docker/scripts/db-harden-security.sh /docker-entrypoint-initdb.d/02-harden-security.sh

RUN chmod +x /docker-entrypoint-initdb.d/01-restore-finlumia_transactions.sh \
    && chmod +x /docker-entrypoint-initdb.d/02-harden-security.sh

EXPOSE 5432
