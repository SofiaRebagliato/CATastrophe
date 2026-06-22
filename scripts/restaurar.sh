#!/usr/bin/env bash
#
# restaurar.sh — Restaura el servicio analytics a su funcionamiento normal,
# leyendo de nuevo la API key real de OpenWeatherMap desde application.yml.
#
# Deshace lo que hace ./scripts/simular-caida.sh. Al reiniciar, el estado del
# circuit breaker se resetea (vuelve a CLOSED) automáticamente.
#
# Uso:   ./scripts/restaurar.sh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT" || exit 1

LOG_DIR="$ROOT/logs"
mkdir -p "$LOG_DIR"

JAR="$ROOT/catastrophe-analytics/target/catastrophe-analytics-0.0.1-SNAPSHOT.jar"
PORT=8085

c_ok=$'\033[32m'; c_warn=$'\033[33m'; c_err=$'\033[31m'; c_dim=$'\033[2m'; c_bold=$'\033[1m'; c_off=$'\033[0m'
ok()   { printf '%s✓%s %s\n' "$c_ok" "$c_off" "$*"; }
warn() { printf '%s!%s %s\n' "$c_warn" "$c_off" "$*"; }
die()  { printf '%s✗ %s%s\n' "$c_err" "$*" "$c_off" >&2; exit 1; }

command -v curl >/dev/null 2>&1 || die "Falta curl."
[ -f "$JAR" ] || die "No existe el jar de analytics: $JAR"

printf '🔄 Restaurando OpenWeatherMap a la normalidad…\n'
if pkill -f catastrophe-analytics 2>/dev/null; then ok "Servicio analytics detenido."; else warn "analytics no estaba corriendo."; fi
sleep 2

# Relanzar SIN override → usa la base-url y la API key reales del application.yml
java --enable-preview -jar "$JAR" > "$LOG_DIR/analytics.log" 2>&1 &

printf '%s→ esperando a que analytics esté UP…%s\n' "$c_dim" "$c_off"
tries=60
while [ "$tries" -gt 0 ]; do
  if curl -fs "http://localhost:$PORT/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    ok "analytics restaurado (clima en directo de nuevo)."
    break
  fi
  sleep 1; tries=$((tries-1))
done
[ "$tries" -gt 0 ] || die "analytics no llegó a UP. Revisa $LOG_DIR/analytics.log"

echo
printf '  En "Consultar humor" volverás a ver el badge  %s● En directo%s\n' "$c_bold" "$c_off"
