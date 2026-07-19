#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE_FILE="docker-compose.dev.yml"
DB_CONTAINER="finlumiadb"
DB_IMAGE="finlumia/db:postgres18"
DB_HOST_PORT="28079"
DB_VOLUME_SUFFIX="finlumia-postgres-data"

remove_db_volumes() {
  local volume_name=""

  while IFS= read -r volume_name; do
    if [ -n "$volume_name" ]; then
      echo "Removendo volume: $volume_name"
      docker volume rm "$volume_name" >/dev/null
    fi
  done < <(docker volume ls --format '{{.Name}}' | grep "${DB_VOLUME_SUFFIX}$" || true)
}

restart_finlumia_db() {
  local reset_data="${1:-false}"

  if [ ! -f "$COMPOSE_FILE" ]; then
    echo "ERRO: arquivo $COMPOSE_FILE nao encontrado."
    exit 1
  fi

  if docker ps -a --format "{{.Names}}" | grep -qx "$DB_CONTAINER"; then
    echo "Parando container: $DB_CONTAINER"
    docker stop "$DB_CONTAINER" >/dev/null 2>&1 || true
    echo "Removendo container: $DB_CONTAINER"
    docker rm "$DB_CONTAINER" >/dev/null
  fi

  if [ "$reset_data" = true ]; then
    remove_db_volumes
  fi

  if docker images --format "{{.Repository}}:{{.Tag}}" | grep -qx "$DB_IMAGE"; then
    echo "Removendo imagem: $DB_IMAGE"
    docker rmi "$DB_IMAGE" >/dev/null
  fi

  echo "Construindo imagem do banco..."
  docker compose -f "$COMPOSE_FILE" build db

  echo "Subindo container do banco..."
  docker compose -f "$COMPOSE_FILE" up -d db

  echo "Aguardando banco ficar saudavel..."
  for _ in $(seq 1 30); do
    if docker inspect --format='{{.State.Health.Status}}' "$DB_CONTAINER" 2>/dev/null | grep -qx "healthy"; then
      if [ "$reset_data" = true ]; then
        echo "Banco finlumia | container=$DB_CONTAINER | image=$DB_IMAGE | port=$DB_HOST_PORT | status=healthy | reset=sim"
      else
        echo "Banco finlumia | container=$DB_CONTAINER | image=$DB_IMAGE | port=$DB_HOST_PORT | status=healthy | reset=nao"
      fi
      return 0
    fi
    sleep 2
  done

  echo "AVISO: container iniciado, mas healthcheck ainda nao reportou healthy."
  docker compose -f "$COMPOSE_FILE" ps db
}

if [ "${1:-}" = "bd" ]; then
  RESET_DATA=false

  if [ $# -eq 1 ]; then
    restart_finlumia_db "$RESET_DATA"
    exit 0
  fi

  if [ $# -eq 2 ] && [ "${2:-}" = "-reset" ]; then
    RESET_DATA=true
    restart_finlumia_db "$RESET_DATA"
    exit 0
  fi

  echo "ERRO: use ./finlumia.sh bd ou ./finlumia.sh bd -reset"
  exit 1
fi

FLAG_T=false
FLAG_C=false
FLAG_S=false
FLAG_ALL=false
MODULE=""
PROFILE=""

if [ $# -eq 0 ]; then
  echo "Uso: ./finlumia.sh bd [-reset]"
  echo "     ./finlumia.sh <modulo>|-all [-t|-c] -local|-hom|-prod [-s]"
  exit 1
fi

while [ $# -gt 0 ]; do
  case "$1" in
    -t) FLAG_T=true ;;
    -c) FLAG_C=true ;;
    -s) FLAG_S=true ;;
    -local) PROFILE="local" ;;
    -hom)   PROFILE="hom" ;;
    -prod)  PROFILE="prod" ;;
    -all) FLAG_ALL=true ;;
    *)
      if [[ "$1" == -* ]]; then
        echo "ERRO: flag desconhecida: $1"
        exit 1
      fi
      MODULE="$1"
      ;;
  esac
  shift
done

BASE_IMAGE="finlumia/base:finlumia-dev-almalinux10module_java21"
BASE_IMAGE_TAR="docker/bases/finlumia-dev-almalinux10module_java21.tar"
DEV_CONTAINER="finlumiadev"
VALID_MODULES="configurator identify movement docs document"

declare -A HOST_PORTS=(
  ["configurator"]=28081
  ["identify"]=28083
  ["movement"]=28084
  ["docs"]=28082
  ["document"]=28085
)
declare -A CONTAINER_PORTS=(
  ["configurator"]=28081
  ["identify"]=28083
  ["movement"]=28084
  ["docs"]=28082
  ["document"]=28085
)
declare -A GRADLE_TASKS=(
  ["configurator"]=":configurator:bootJar"
  ["identify"]=":identify:bootJar"
  ["movement"]=":movement:bootJar"
  ["docs"]=":docs:bootJar"
  ["document"]=":document:bootJar"
)

if [ "$FLAG_T" = true ] && [ "$FLAG_C" = true ]; then echo "ERRO: use -t ou -c."; exit 1; fi
if [ "$FLAG_T" = false ] && [ "$FLAG_C" = false ]; then echo "ERRO: informe -t ou -c."; exit 1; fi
if [ "$FLAG_S" = true ] && [ "$FLAG_T" = false ]; then echo "ERRO: -s so com -t."; exit 1; fi
if [ -z "$PROFILE" ]; then echo "ERRO: informe -local, -hom ou -prod."; exit 1; fi
if [ "$FLAG_ALL" = true ] && [ -n "$MODULE" ]; then echo "ERRO: use modulo unico ou -all."; exit 1; fi
if [ "$FLAG_ALL" = false ] && [ -z "$MODULE" ]; then echo "ERRO: informe modulo ou -all."; exit 1; fi

ensure_base_image() {
  if docker images --format "{{.Repository}}:{{.Tag}}" | grep -qx "$BASE_IMAGE"; then
    return
  fi
  if [ ! -f "$BASE_IMAGE_TAR" ]; then
    echo "ERRO: arquivo da imagem base nao encontrado: $BASE_IMAGE_TAR"
    exit 1
  fi
  LOAD_OUTPUT="$(docker load -i "$BASE_IMAGE_TAR" 2>&1)"
  echo "$LOAD_OUTPUT"
  if docker images --format "{{.Repository}}:{{.Tag}}" | grep -qx "$BASE_IMAGE"; then
    return
  fi
  LOADED_IMAGE="$(echo "$LOAD_OUTPUT" | sed -n 's/^Loaded image: //p' | awk 'NR==1')"
  if [ -z "$LOADED_IMAGE" ]; then
    echo "ERRO: nao foi possivel identificar a imagem carregada para retag."
    exit 1
  fi
  docker tag "$LOADED_IMAGE" "$BASE_IMAGE"
}

TARGET_MODULES=""
if [ "$FLAG_ALL" = true ]; then
  TARGET_MODULES="$VALID_MODULES"
else
  MODULE_OK=false
  for M in $VALID_MODULES; do
    if [ "$M" = "$MODULE" ]; then MODULE_OK=true; break; fi
  done
  if [ "$MODULE_OK" = false ]; then
    echo "ERRO: modulo invalido. Use: $VALID_MODULES"
    exit 1
  fi
  TARGET_MODULES="$MODULE"
fi

if [ "$FLAG_T" = true ] && [ "$FLAG_S" = true ]; then
  for CURRENT_MODULE in $TARGET_MODULES; do
    CURRENT_CONTAINER="test-${CURRENT_MODULE}-${PROFILE}"
    if docker ps -a --format "{{.Names}}" | grep -qx "$CURRENT_CONTAINER"; then
      docker stop "$CURRENT_CONTAINER" >/dev/null
      docker rm "$CURRENT_CONTAINER" >/dev/null
      echo "Container removido: $CURRENT_CONTAINER"
    fi
  done
  exit 0
fi

ensure_base_image

resolve_exec_user() {
  local container_name="$1"
  if docker exec "$container_name" getent passwd finlumia >/dev/null 2>&1; then
    echo "finlumia"
  else
    echo "root"
  fi
}

prepare_gradle_env() {
  local container_name="$1"
  docker exec "$container_name" bash -c "
    mkdir -p /workspace/.gradle
    chown -R finlumia:finlumia /home/finlumia/.gradle /workspace/.gradle
    find /workspace -maxdepth 3 -name 'build' -type d \
      -exec chown -R finlumia:finlumia {} + 2>/dev/null || true
  "
}

for CURRENT_MODULE in $TARGET_MODULES; do
  GRADLE_TASK="${GRADLE_TASKS[$CURRENT_MODULE]}"
  DOCKERFILE="docker/scripts/${CURRENT_MODULE}.Dockerfile"
  MODULE_IMAGE="finlumia/${CURRENT_MODULE}:${PROFILE}"
  CONTAINER_PORT="${CONTAINER_PORTS[$CURRENT_MODULE]}"
  HOST_PORT="${HOST_PORTS[$CURRENT_MODULE]}"
  CONTAINER_TEST="test-${CURRENT_MODULE}-${PROFILE}"

  if [ ! -f "$DOCKERFILE" ]; then
    echo "ERRO: Dockerfile nao encontrado em $DOCKERFILE"
    exit 1
  fi

  if docker ps --format "{{.Names}}" | grep -qx "$DEV_CONTAINER"; then
    EXEC_USER="$(resolve_exec_user "$DEV_CONTAINER")"
    prepare_gradle_env "$DEV_CONTAINER"
    docker exec -u "$EXEC_USER" "$DEV_CONTAINER" bash -lc "./gradlew $GRADLE_TASK --no-daemon -Dspring.profiles.active=$PROFILE"
  else
    ./gradlew "$GRADLE_TASK" --no-daemon "-Dspring.profiles.active=$PROFILE"
  fi

  docker buildx build --load -t "$MODULE_IMAGE" -f "$DOCKERFILE" --build-arg SPRING_PROFILE="$PROFILE" .

  if [ "$FLAG_T" = true ]; then
    OUTPUT_DIR="docker/test"
  else
    OUTPUT_DIR="docker/build"
  fi
  mkdir -p "$OUTPUT_DIR"
  OUTPUT_FILE="${OUTPUT_DIR}/${CURRENT_MODULE}-${PROFILE}.tar"
  docker save -o "$OUTPUT_FILE" "$MODULE_IMAGE"

  if [ "$FLAG_T" = true ]; then
    if docker ps -a --format "{{.Names}}" | grep -qx "$CONTAINER_TEST"; then
      docker stop "$CONTAINER_TEST" >/dev/null
      docker rm "$CONTAINER_TEST" >/dev/null
    fi
    DOCKER_RUN_ARGS=(
      run -d
      --name "$CONTAINER_TEST"
      -p "${HOST_PORT}:${CONTAINER_PORT}"
      -e "SPRING_PROFILES_ACTIVE=$PROFILE"
      --restart unless-stopped
    )

    DOCKER_RUN_ARGS+=(
      -e "SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:${DB_HOST_PORT}/finlumia_transactions"
    )

    if [ "$CURRENT_MODULE" = "docs" ]; then
      DOCKER_RUN_ARGS+=(
        -e "DOCS_MODULES_BASE_URL_CONFIGURATOR=http://host.docker.internal:28081"
        -e "DOCS_MODULES_BASE_URL_IDENTIFY=http://host.docker.internal:28083"
        -e "DOCS_MODULES_BASE_URL_MOVEMENT=http://host.docker.internal:28084"
      )
    fi

    DOCKER_RUN_ARGS+=("$MODULE_IMAGE")

    docker "${DOCKER_RUN_ARGS[@]}"
  fi

  echo "$CURRENT_MODULE | profile=$PROFILE | host=$HOST_PORT | container=$CONTAINER_PORT | image=$MODULE_IMAGE"
done

if [ "$FLAG_T" = true ] && [ "$FLAG_ALL" = false ]; then
  docker logs -f "test-${MODULE}-${PROFILE}"
fi
