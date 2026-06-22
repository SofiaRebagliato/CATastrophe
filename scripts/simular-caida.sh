#!/usr/bin/env bash
#
# simular-caida.sh — Simula una caída de la API de OpenWeatherMap para la demo.
#
# Relanza SOLO el servicio analytics apuntando su base-url a un puerto muerto
# (localhost:9999), de modo que las llamadas al clima fallen al instante y se
# active el patrón Circuit Breaker + fallback a la caché Redis.
#
# No recompila nada ni toca application.yml: sobreescribe la propiedad por
# línea de comandos al arrancar el jar (Spring Boot lo permite).
#
# Uso:                       ./scripts/simular-caida.sh
# Volver a la normalidad:    ./scripts/restaurar.sh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT" || exit 1

LOG_DIR="$ROOT/logs"
mkdir -p "$LOG_DIR"

JAR="$ROOT/catastrophe-analytics/target/catastrophe-analytics-0.0.1-SNAPSHOT.jar"
PORT=8085
DEAD_URL="http://localhost:9999"   # nadie escucha aquí → conexión rechazada al instante

c_ok=$'\033[32m'; c_warn=$'\033[33m'; c_err=$'\033[31m'; c_dim=$'\033[2m'; c_bold=$'\033[1m'; c_off=$'\033[0m'
ok()   { printf '%s✓%s %s\n' "$c_ok" "$c_off" "$*"; }
warn() { printf '%s!%s %s\n' "$c_warn" "$c_off" "$*"; }
die()  { printf '%s✗ %s%s\n' "$c_err" "$*" "$c_off" >&2; exit 1; }

command -v curl >/dev/null 2>&1 || die "Falta curl."
[ -f "$JAR" ] || die "No existe el jar de analytics: $JAR (arranca antes la demo con ./scripts/start-demo.sh)."

printf '🔌 Simulando caída de OpenWeatherMap…\n'

# Matar la instancia actual de analytics
if pkill -f catastrophe-analytics 2>/dev/null; then ok "Servicio analytics detenido."; else warn "analytics no estaba corriendo (lo arranco igualmente)."; fi
sleep 2

# Relanzar apuntando a un endpoint muerto → la llamada al clima falla al instante
java --enable-preview -jar "$JAR" \
  --catastrophe.openweather.base-url="$DEAD_URL" \
  > "$LOG_DIR/analytics.log" 2>&1 &

printf '%s→ esperando a que analytics esté UP…%s\n' "$c_dim" "$c_off"
tries=60
while [ "$tries" -gt 0 ]; do
  if curl -fs "http://localhost:$PORT/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    ok "analytics listo (en modo 'API caída')."
    break
  fi
  sleep 1; tries=$((tries-1))
done
[ "$tries" -gt 0 ] || die "analytics no llegó a UP. Revisa $LOG_DIR/analytics.log"

echo
printf '%s════════════════════════════════════════════════════════%s\n' "$c_bold" "$c_off"
printf '%s  🌩️  OpenWeatherMap "caída" — modo demo de resiliencia%s\n' "$c_bold" "$c_off"
printf '%s════════════════════════════════════════════════════════%s\n' "$c_bold" "$c_off"
printf '  En "Consultar humor" verás ahora el badge  %s⚠️ Datos en caché%s\n' "$c_bold" "$c_off"
printf '  Estado del circuito:  %shttp://localhost:%s/actuator/circuitbreakers%s\n' "$c_bold" "$PORT" "$c_off"
printf '  (pulsa 5-6 veces para verlo pasar de CLOSED a OPEN)\n'
printf '  Restaurar:            %s./scripts/restaurar.sh%s\n' "$c_bold" "$c_off"
printf '%s════════════════════════════════════════════════════════%s\n' "$c_bold" "$c_off"
echo
warn "Recuerda: para que el clima salga de la caché, antes de la 'caída' consulta el humor una vez con la API normal (así queda guardado en Redis)."
