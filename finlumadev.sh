#!/usr/bin/env bash
set -euo pipefail

show_usage() {
  echo "Uso: ./finlumadev.sh <up|rebuild|shell|down|status|logs>"
}

if [ $# -ne 1 ]; then
  show_usage
  exit 1
fi

CMD="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE_FILE="docker-compose.dev.yml"
BASE_IMAGE="finlumia/dev:almalinux10module_java21"
BASE_IMAGE_TAR="docker/bases/finlumia-dev-almalinux10module_java21.tar"

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "ERRO: arquivo $COMPOSE_FILE nao encontrado."
  exit 1
fi

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

case "$CMD" in
  up)
    ensure_base_image
    docker compose -f "$COMPOSE_FILE" up -d
    ;;
  rebuild)
    ensure_base_image
    docker compose -f "$COMPOSE_FILE" up -d --force-recreate
    ;;
  shell)
    docker compose -f "$COMPOSE_FILE" exec --user finlumia dev bash
    ;;
  down)
    docker compose -f "$COMPOSE_FILE" down
    ;;
  status)
    docker compose -f "$COMPOSE_FILE" ps
    ;;
  logs)
    docker compose -f "$COMPOSE_FILE" logs -f dev
    ;;
  *)
    show_usage
    exit 1
    ;;
esac
