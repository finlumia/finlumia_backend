#!/usr/bin/env bash
set -euo pipefail

echo "Aplicando hardening de seguranca: removendo usuario padrao postgres..."

psql -v ON_ERROR_STOP=1 \
  --username="${POSTGRES_USER}" \
  --dbname="${POSTGRES_DB}" <<EOSQL
DO \$\$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'postgres') THEN
    EXECUTE 'REASSIGN OWNED BY postgres TO CURRENT_USER';
    EXECUTE 'DROP OWNED BY postgres';
    EXECUTE 'DROP ROLE postgres';
    RAISE NOTICE 'Role postgres removida.';
  END IF;
END
\$\$;

REVOKE ALL ON DATABASE ${POSTGRES_DB} FROM PUBLIC;
GRANT ALL ON DATABASE ${POSTGRES_DB} TO ${POSTGRES_USER};
EOSQL

if psql -v ON_ERROR_STOP=1 --username="${POSTGRES_USER}" --dbname=postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname = 'postgres'" | grep -q 1; then
  echo "ERRO: a role postgres ainda existe apos o hardening."
  exit 1
fi

echo "Hardening concluido: usuario postgres inexistente."
