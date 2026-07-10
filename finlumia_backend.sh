#!/usr/bin/env bash
# =============================================================================
# finlumia_backend.sh — Deploy de producao Finlumia na VPS
#
# Uso:
#   ./finlumia_backend.sh bd [-reset]
#   ./finlumia_backend.sh <modulo> [-logs]
#   ./finlumia_backend.sh -all
#
# Para chamar como comando global na VPS:
#   1. Edite ~/.bashrc e adicione:
#        export FINLUMIABACK_HOME=/caminho/para/o/projeto
#        alias finlumiaback="$FINLUMIABACK_HOME/finlumia_backend.sh"
#   2. Recarregue: source ~/.bashrc
#   3. Agora pode usar de qualquer lugar: finlumiaback -all
#
# Pre-requisitos:
#   - docker e docker buildx instalados
#   - Tars dos modulos gerados pelo finlumia.sh (./finlumia.sh -all -c -pro)
#     e disponiveis em docker/build/<modulo>-pro.tar
#   - Um unico arquivo *.backup (pg_dump formato custom) em docker/backup/,
#     usado para restaurar o banco na criacao do container (ver deploy_db).
# =============================================================================
set -euo pipefail

# ---------------------------------------------------------------------------
# Resolucao do diretorio raiz do projeto
# ---------------------------------------------------------------------------
if [ -n "${FINLUMIABACK_HOME:-}" ]; then
  PROJECT_ROOT="$FINLUMIABACK_HOME"
else
  PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
fi

# ---------------------------------------------------------------------------
# Configuracao global
# ---------------------------------------------------------------------------
PROFILE="pro"
NETWORK_NAME="finlumia-net"

DB_CONTAINER="finlumiadb"
DB_IMAGE="finlumia/db:postgres18"
DB_HOST_PORT="28079"
DB_VOLUME_NAME="finlumia-postgres-data"
DB_USER="${FINLUMIA_DB_USER:-papadopoulos}"
DB_PASS="${FINLUMIA_DB_PASS:?FINLUMIA_DB_PASS obrigatorio. Execute: export FINLUMIA_DB_PASS=<sua-senha>}"
DB_NAME="${FINLUMIA_DB_NAME:-finlumia_transactions}"

VALID_MODULES="configurator identify movement docs document"

declare -A CONTAINER_NAMES=(
  ["configurator"]="finlumia-configurator"
  ["identify"]="finlumia-identify"
  ["movement"]="finlumia-movement"
  ["docs"]="finlumia-docs"
  ["document"]="finlumia-document"
)
declare -A IMAGES=(
  ["configurator"]="finlumia/configurator:latest"
  ["identify"]="finlumia/identify:latest"
  ["movement"]="finlumia/movement:latest"
  ["docs"]="finlumia/docs:latest"
  ["document"]="finlumia/document:latest"
)
declare -A TAR_FILES=(
  ["configurator"]="docker/build/configurator-pro.tar"
  ["identify"]="docker/build/identify-pro.tar"
  ["movement"]="docker/build/movement-pro.tar"
  ["docs"]="docker/build/docs-pro.tar"
  ["document"]="docker/build/document-pro.tar"
)
declare -A HOST_PORTS=(
  ["configurator"]="28081"
  ["identify"]="28083"
  ["movement"]="28084"
  ["docs"]="28082"
  ["document"]="28085"
)
declare -A CONTAINER_PORTS=(
  ["configurator"]="28081"
  ["identify"]="28083"
  ["movement"]="28084"
  ["docs"]="28082"
  ["document"]="28085"
)

# ---------------------------------------------------------------------------
# Utilitarios
# ---------------------------------------------------------------------------

ensure_network() {
  if ! docker network ls --format "{{.Name}}" | grep -qx "$NETWORK_NAME"; then
    echo "Criando rede Docker: $NETWORK_NAME"
    docker network create "$NETWORK_NAME"
  fi
}

remove_container() {
  local name="$1"
  if docker ps -a --format "{{.Names}}" | grep -qx "$name"; then
    echo "Parando container: $name"
    docker stop "$name" >/dev/null 2>&1 || true
    echo "Removendo container: $name"
    docker rm "$name" >/dev/null
  fi
}

remove_image() {
  local image="$1"
  if docker images --format "{{.Repository}}:{{.Tag}}" | grep -qx "$image"; then
    echo "Removendo imagem: $image"
    docker rmi "$image" >/dev/null
  fi
}

remove_db_volume() {
  if docker volume ls --format "{{.Name}}" | grep -qx "$DB_VOLUME_NAME"; then
    echo "Removendo volume: $DB_VOLUME_NAME"
    docker volume rm "$DB_VOLUME_NAME" >/dev/null
  fi
}

# ---------------------------------------------------------------------------
# Deploy do banco de dados
# ---------------------------------------------------------------------------

deploy_db() {
  local reset_data="${1:-false}"
  local dockerfile="$PROJECT_ROOT/docker/scripts/dbfinlumia.Dockerfile"
  local backup_dir="$PROJECT_ROOT/docker/backup"

  if [ ! -f "$dockerfile" ]; then
    echo "ERRO: Dockerfile nao encontrado: $dockerfile"
    exit 1
  fi

  if [ ! -d "$backup_dir" ]; then
    echo "ERRO: diretorio de backup nao encontrado: $backup_dir"
    exit 1
  fi

  local backup_files=("$backup_dir"/*.backup)
  if [ ! -e "${backup_files[0]}" ]; then
    echo "ERRO: nenhum arquivo .backup encontrado em $backup_dir"
    exit 1
  fi
  if [ "${#backup_files[@]}" -gt 1 ]; then
    echo "ERRO: encontrado mais de um arquivo .backup em $backup_dir. Mantenha apenas um."
    printf '  - %s\n' "${backup_files[@]}"
    exit 1
  fi
  echo "Backup selecionado: ${backup_files[0]}"

  remove_container "$DB_CONTAINER"

  if [ "$reset_data" = true ]; then
    remove_db_volume
  fi

  remove_image "$DB_IMAGE"

  ensure_network

  echo "Construindo imagem do banco: $DB_IMAGE"
  docker buildx build --load \
    -t "$DB_IMAGE" \
    -f "$dockerfile" \
    "$PROJECT_ROOT"

  echo "Iniciando container: $DB_CONTAINER"
  docker run -d \
    --name "$DB_CONTAINER" \
    --network "$NETWORK_NAME" \
    -p "127.0.0.1:${DB_HOST_PORT}:5432" \
    -e "POSTGRES_USER=$DB_USER" \
    -e "POSTGRES_PASSWORD=$DB_PASS" \
    -e "POSTGRES_DB=$DB_NAME" \
    -v "${DB_VOLUME_NAME}:/var/lib/postgresql" \
    -v "${backup_dir}:/docker-entrypoint-initdb.d/backup:ro" \
    --health-cmd "pg_isready -U $DB_USER -d $DB_NAME" \
    --health-interval=5s \
    --health-timeout=5s \
    --health-retries=10 \
    --health-start-period=30s \
    --restart unless-stopped \
    "$DB_IMAGE"

  echo "Aguardando banco ficar saudavel..."
  for _ in $(seq 1 30); do
    if docker inspect --format='{{.State.Health.Status}}' "$DB_CONTAINER" 2>/dev/null | grep -qx "healthy"; then
      echo "Banco | container=$DB_CONTAINER | port=127.0.0.1:${DB_HOST_PORT} | status=healthy | reset=$reset_data"
      return 0
    fi
    sleep 2
  done

  echo "AVISO: container iniciado, mas healthcheck nao reportou healthy ainda."
  docker ps --filter "name=$DB_CONTAINER" --format "table {{.Names}}\t{{.Status}}"
}

# ---------------------------------------------------------------------------
# Deploy de modulo de aplicacao
# ---------------------------------------------------------------------------

deploy_module() {
  local module="$1"
  local tar_file="${PROJECT_ROOT}/${TAR_FILES[$module]}"
  local image="${IMAGES[$module]}"
  local container="${CONTAINER_NAMES[$module]}"
  local host_port="${HOST_PORTS[$module]}"
  local container_port="${CONTAINER_PORTS[$module]}"
  local bind_host="127.0.0.1"

  if [ ! -f "$tar_file" ]; then
    echo "ERRO: tar nao encontrado: $tar_file"
    echo "       Gere os artefatos antes: ./finlumia.sh ${module} -c -pro"
    exit 1
  fi

  remove_container "$container"
  remove_image "$image"

  ensure_network

  echo "Carregando imagem de $tar_file"
  docker load -i "$tar_file"

  echo "Iniciando container: $container"

  local run_args=(
    run -d
    --name "$container"
    --network "$NETWORK_NAME"
    -p "${bind_host}:${host_port}:${container_port}"
    -e "SPRING_PROFILES_ACTIVE=$PROFILE"
    --restart unless-stopped
  )

  if [ "$module" != "docs" ]; then
    run_args+=(
      -e "SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_CONTAINER}:5432/${DB_NAME}"
      -e "SPRING_DATASOURCE_USERNAME=$DB_USER"
      -e "SPRING_DATASOURCE_PASSWORD=$DB_PASS"
    )
  fi

  if [ "$module" = "docs" ]; then
    run_args+=(
      -e "DOCS_MODULES_BASE_URL_CONFIGURATOR=http://${CONTAINER_NAMES[configurator]}:${CONTAINER_PORTS[configurator]}"
      -e "DOCS_MODULES_BASE_URL_IDENTIFY=http://${CONTAINER_NAMES[identify]}:${CONTAINER_PORTS[identify]}"
      -e "DOCS_MODULES_BASE_URL_MOVEMENT=http://${CONTAINER_NAMES[movement]}:${CONTAINER_PORTS[movement]}"
    )
  fi

  run_args+=("$image")
  docker "${run_args[@]}"

  echo "$module | container=$container | host=${bind_host}:${host_port} | image=$image | status=iniciado"
}

# ---------------------------------------------------------------------------
# Parse de argumentos
# ---------------------------------------------------------------------------

show_usage() {
  echo "Uso:"
  echo "  $0 bd [-reset]          Sobe o banco de dados (com -reset apaga os dados)"
  echo "  $0 <modulo> [-logs]     Sobe um modulo especifico e aguarda logs (opcional)"
  echo "  $0 -all                 Sobe todos os modulos de aplicacao"
  echo ""
  echo "Modulos disponiveis: $VALID_MODULES"
  echo ""
  echo "Variaveis de ambiente aceitas:"
  echo "  FINLUMIABACK_HOME       Raiz do projeto (padrao: diretorio do script)"
  echo "  FINLUMIA_DB_USER        Usuario do banco (padrao: papadopoulos)"
  echo "  FINLUMIA_DB_PASS        Senha do banco"
  echo "  FINLUMIA_DB_NAME        Nome do banco (padrao: finlumia_transactions)"
  echo ""
  echo "Backup do banco:"
  echo "  Coloque um unico arquivo *.backup em docker/backup/. Ele e montado"
  echo "  no container e restaurado automaticamente na primeira inicializacao"
  echo "  do volume de dados (nao roda de novo se o volume ja existir)."
}

if [ $# -eq 0 ]; then
  show_usage
  exit 1
fi

# --- Subcomando bd ---
if [ "${1:-}" = "bd" ]; then
  RESET_DATA=false
  if [ "${2:-}" = "-reset" ]; then
    RESET_DATA=true
  elif [ $# -gt 1 ] && [ "${2:-}" != "-reset" ]; then
    echo "ERRO: use $0 bd ou $0 bd -reset"
    exit 1
  fi
  deploy_db "$RESET_DATA"
  exit 0
fi

# --- Flags dos modulos ---
FLAG_ALL=false
FLAG_LOGS=false
MODULE=""

while [ $# -gt 0 ]; do
  case "$1" in
    -all)  FLAG_ALL=true ;;
    -logs) FLAG_LOGS=true ;;
    -h|--help) show_usage; exit 0 ;;
    -*)
      echo "ERRO: flag desconhecida: $1"
      show_usage
      exit 1
      ;;
    *)
      if [ -n "$MODULE" ]; then
        echo "ERRO: informe apenas um modulo por vez, ou use -all."
        exit 1
      fi
      MODULE="$1"
      ;;
  esac
  shift
done

if [ "$FLAG_ALL" = true ] && [ -n "$MODULE" ]; then
  echo "ERRO: use modulo unico ou -all, nao os dois."
  exit 1
fi

if [ "$FLAG_ALL" = false ] && [ -z "$MODULE" ]; then
  echo "ERRO: informe um modulo ou use -all."
  show_usage
  exit 1
fi

if [ "$FLAG_LOGS" = true ] && [ "$FLAG_ALL" = true ]; then
  echo "ERRO: -logs so pode ser usado com um modulo especifico."
  exit 1
fi

TARGET_MODULES=""
if [ "$FLAG_ALL" = true ]; then
  TARGET_MODULES="$VALID_MODULES"
else
  MODULE_OK=false
  for M in $VALID_MODULES; do
    if [ "$M" = "$MODULE" ]; then MODULE_OK=true; break; fi
  done
  if [ "$MODULE_OK" = false ]; then
    echo "ERRO: modulo invalido '$MODULE'. Use: $VALID_MODULES"
    exit 1
  fi
  TARGET_MODULES="$MODULE"
fi

# ---------------------------------------------------------------------------
# Execucao
# ---------------------------------------------------------------------------

for CURRENT_MODULE in $TARGET_MODULES; do
  deploy_module "$CURRENT_MODULE"
done

if [ "$FLAG_LOGS" = true ]; then
  docker logs -f "${CONTAINER_NAMES[$MODULE]}"
fi
