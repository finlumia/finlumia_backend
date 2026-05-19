#!/usr/bin/env bash
set -euo pipefail

FLAG_T=false
FLAG_C=false
FLAG_S=false
FLAG_PRO=false
FLAG_DEV=false
FLAG_ALL=false
MODULE=""

if [ $# -eq 0 ]; then
  echo "Uso: ./finlumia.sh <modulo>|-all [-t|-c] [-pro|-dev] [-s]"
  exit 1
fi

while [ $# -gt 0 ]; do
  case "$1" in
    -t) FLAG_T=true ;;
    -c) FLAG_C=true ;;
    -s) FLAG_S=true ;;
    -pro) FLAG_PRO=true ;;
    -dev) FLAG_DEV=true ;;
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

BASE_IMAGE="finlumia/base:almalinux10-zulu21"
BASE_IMAGE_TAR="docker/bases/finlumia-base-almalinux10-zulu21.tar"
DEV_CONTAINER="finlumiadev"
VALID_MODULES="configurator identity movement docs"

declare -A HOST_PORTS_DEV=(
  ["configurator"]=40571
  ["identity"]=40572
  ["movement"]=40573
  ["docs"]=40574
)
declare -A HOST_PORTS_PRO=(
  ["configurator"]=40571
  ["identity"]=40572
  ["movement"]=40573
  ["docs"]=40574
)
declare -A CONTAINER_PORTS=(
  ["configurator"]=40571
  ["identity"]=40572
  ["movement"]=40573
  ["docs"]=40574
)
declare -A GRADLE_TASKS=(
  ["configurator"]=":configurator:bootJar"
  ["identity"]=":identity:bootJar"
  ["movement"]=":movement:bootJar"
  ["docs"]=":docs:bootJar"
)

if [ "$FLAG_T" = true ] && [ "$FLAG_C" = true ]; then echo "ERRO: use -t ou -c."; exit 1; fi
if [ "$FLAG_T" = false ] && [ "$FLAG_C" = false ]; then echo "ERRO: informe -t ou -c."; exit 1; fi
if [ "$FLAG_S" = true ] && [ "$FLAG_T" = false ]; then echo "ERRO: -s so com -t."; exit 1; fi
if [ "$FLAG_PRO" = true ] && [ "$FLAG_DEV" = true ]; then echo "ERRO: use -pro ou -dev."; exit 1; fi
if [ "$FLAG_PRO" = false ] && [ "$FLAG_DEV" = false ]; then echo "ERRO: informe -pro ou -dev."; exit 1; fi
if [ "$FLAG_ALL" = true ] && [ -n "$MODULE" ]; then echo "ERRO: use modulo unico ou -all."; exit 1; fi
if [ "$FLAG_ALL" = false ] && [ -z "$MODULE" ]; then echo "ERRO: informe modulo ou -all."; exit 1; fi

PROFILE="dev"
if [ "$FLAG_PRO" = true ]; then PROFILE="pro"; fi

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

for CURRENT_MODULE in $TARGET_MODULES; do
  GRADLE_TASK="${GRADLE_TASKS[$CURRENT_MODULE]}"
  DOCKERFILE="docker/scripts/${CURRENT_MODULE}.Dockerfile"
  MODULE_IMAGE="finlumia/${CURRENT_MODULE}:latest"
  CONTAINER_PORT="${CONTAINER_PORTS[$CURRENT_MODULE]}"
  if [ "$PROFILE" = "pro" ]; then
    HOST_PORT="${HOST_PORTS_PRO[$CURRENT_MODULE]}"
  else
    HOST_PORT="${HOST_PORTS_DEV[$CURRENT_MODULE]}"
  fi
  CONTAINER_TEST="test-${CURRENT_MODULE}-${PROFILE}"

  if [ ! -f "$DOCKERFILE" ]; then
    echo "ERRO: Dockerfile nao encontrado em $DOCKERFILE"
    exit 1
  fi

  if docker ps --format "{{.Names}}" | grep -qx "$DEV_CONTAINER"; then
    EXEC_USER="$(resolve_exec_user "$DEV_CONTAINER")"
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
    docker run -d --name "$CONTAINER_TEST" -p "${HOST_PORT}:${CONTAINER_PORT}" -e "SPRING_PROFILES_ACTIVE=$PROFILE" --restart unless-stopped "$MODULE_IMAGE"
  fi

  echo "$CURRENT_MODULE | profile=$PROFILE | host=$HOST_PORT | container=$CONTAINER_PORT | image=$MODULE_IMAGE"
done

if [ "$FLAG_T" = true ] && [ "$FLAG_ALL" = false ]; then
  docker logs -f "test-${MODULE}-${PROFILE}"
fi
