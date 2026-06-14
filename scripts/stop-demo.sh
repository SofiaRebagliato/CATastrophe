#!/usr/bin/env bash
#
# stop-demo.sh — Para CATastrophe limpiamente.
#
# Uso:
#   ./scripts/stop-demo.sh           # para servicios + túnel y detiene la infra (conserva datos)
#   ./scripts/stop-demo.sh --down    # además, elimina los contenedores (conserva volúmenes)
#   ./scripts/stop-demo.sh --clean   # elimina contenedores Y volúmenes (borra los datos sembrados)

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT" || exit 1
PID_FILE="$ROOT/.demo-pids"

c_ok=$'\033[32m'; c_off=$'\033[0m'
ok() { printf '%s✓%s %s\n' "$c_ok" "$c_off" "$*"; }

if docker compose version >/dev/null 2>&1; then DC="docker compose"; else DC="docker-compose"; fi

# ── Parar procesos Java + túnel ──
if [ -f "$PID_FILE" ]; then
  while read -r pid name; do
    [ -z "$pid" ] && continue
    if kill "$pid" >/dev/null 2>&1; then ok "Parado: $name (pid $pid)"; fi
  done < "$PID_FILE"
  rm -f "$PID_FILE"
else
  # Plan B: matar por puerto si no hay registro de PIDs
  for port in 8080 8081 8082 8083 8084 8085; do
    pid=$(lsof -ti tcp:"$port" 2>/dev/null || true)
    [ -n "$pid" ] && kill "$pid" 2>/dev/null && ok "Parado proceso en puerto $port"
  done
  pkill -f 'cloudflared tunnel' 2>/dev/null && ok "Túnel cerrado" || true
fi

# ── Infraestructura ──
case "${1:-}" in
  --clean) $DC down -v >/dev/null 2>&1 && ok "Contenedores y volúmenes eliminados (datos borrados)." ;;
  --down)  $DC down    >/dev/null 2>&1 && ok "Contenedores eliminados (volúmenes conservados)." ;;
  *)       $DC stop    >/dev/null 2>&1 && ok "Infraestructura detenida (datos conservados)." ;;
esac

ok "Todo parado."
