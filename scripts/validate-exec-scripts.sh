#!/usr/bin/env bash
# Valida sintaxe e regras basicas dos scripts de execucao (sem build Docker completo).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
FAIL=0

fail() { echo "FALHA: $1"; FAIL=1; }
ok() { echo "OK: $1"; }

bash -n finlumia.sh && ok "finlumia.sh sintaxe"
bash -n finlumiadev.sh && ok "finlumiadev.sh sintaxe"

MODULES="configurator identify movement docs"
for m in $MODULES; do
  df="docker/scripts/${m}.Dockerfile"
  [ -f "$df" ] || fail "Dockerfile ausente: $df"
done
ok "Dockerfiles dos 4 modulos"

for m in $MODULES; do
  ./gradlew ":${m}:bootJar" -q >/dev/null 2>&1 || fail "Gradle bootJar falhou: $m"
done
ok "Gradle bootJar (todos os modulos)"

OUT="$(./finlumia.sh identity -t -dev 2>&1 || true)"
echo "$OUT" | grep -q "modulo invalido" && ok "finlumia.sh rejeita modulo 'identity'" || fail "finlumia.sh deveria rejeitar 'identity'"

OUT="$(./finlumia.sh identify -t -c -dev 2>&1 || true)"
echo "$OUT" | grep -q "use -t ou -c" && ok "finlumia.sh rejeita -t e -c juntos" || fail "finlumia.sh deveria rejeitar -t e -c"

grep -q 'identify.*:identify:bootJar' finlumia.sh && ok "finlumia.sh task identify" || fail "finlumia.sh sem :identify:bootJar"
grep -q 'identify.*:identify:bootJar' finlumia.ps1 && ok "finlumia.ps1 task identify" || fail "finlumia.ps1 sem :identify:bootJar"
grep -q '28081' finlumia.ps1 && ok "finlumia.ps1 porta configurator 28081" || fail "finlumia.ps1 portas desatualizadas"

if [ "$FAIL" -eq 0 ]; then
  echo ""
  echo "Todos os testes de validacao passaram."
  exit 0
fi
echo ""
echo "Alguns testes falharam."
exit 1
