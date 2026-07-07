#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="/docker-entrypoint-initdb.d/backup"

if [ ! -d "$BACKUP_DIR" ]; then
  echo "ERRO: diretorio de backup nao montado em ${BACKUP_DIR}."
  exit 1
fi

mapfile -t BACKUP_FILES < <(find "$BACKUP_DIR" -maxdepth 1 -type f -name "*.backup")

if [ "${#BACKUP_FILES[@]}" -eq 0 ]; then
  echo "ERRO: nenhum arquivo .backup encontrado em ${BACKUP_DIR}."
  exit 1
fi

if [ "${#BACKUP_FILES[@]}" -gt 1 ]; then
  echo "ERRO: encontrado mais de um arquivo .backup em ${BACKUP_DIR}. Mantenha apenas um."
  printf '  - %s\n' "${BACKUP_FILES[@]}"
  exit 1
fi

BACKUP_FILE="${BACKUP_FILES[0]}"

echo "Restaurando backup a partir de ${BACKUP_FILE}..."

pg_restore \
  --username="${POSTGRES_USER}" \
  --dbname="${POSTGRES_DB}" \
  --no-owner \
  --no-acl \
  --verbose \
  "${BACKUP_FILE}"

echo "Backup restaurado com sucesso."
