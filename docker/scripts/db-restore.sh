#!/usr/bin/env bash
set -euo pipefail

BACKUP_FILE="/docker-entrypoint-initdb.d/finlumia_transactions.dump"

echo "Restaurando backup finlumia_transactions a partir de ${BACKUP_FILE}..."

pg_restore \
  --username="${POSTGRES_USER}" \
  --dbname="${POSTGRES_DB}" \
  --no-owner \
  --no-acl \
  --verbose \
  "${BACKUP_FILE}"

echo "Backup finlumia_transactions restaurado com sucesso."
