#!/usr/bin/env bash
#
# start-demo.sh — Levanta CATastrophe en local para la defensa del proyecto.
#
#   Infra (Docker)  →  6 servicios Spring Boot  →  (opcional) datos demo  →  (opcional) túnel público
#
# Uso:
#   ./scripts/start-demo.sh                 # build si falta + infra + servicios
#   ./scripts/start-demo.sh --seed          # además, siembra datos de demostración
#   ./scripts/start-demo.sh --tunnel        # además, abre un túnel Cloudflare (URL pública)
#   ./scripts/start-demo.sh --build         # fuerza recompilar los jars
#   ./scripts/start-demo.sh --all           # build + seed + tunnel
#
# Para parar todo:  ./scripts/stop-demo.sh
#
# Claves de APIs externas (opcionales): si quieres tiempo real en personalidad/humor y
# avatares de TheCatAPI, exporta antes:  export OPENWEATHER_KEY=...  CATAPI_KEY=...
# Si no, los circuit breakers usan valores de reserva y la demo funciona igual.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT" || exit 1

LOG_DIR="$ROOT/logs"
PID_FILE="$ROOT/.demo-pids"
mkdir -p "$LOG_DIR"

# ── Opciones ──
DO_BUILD=0; DO_SEED=0; DO_TUNNEL=0
for arg in "$@"; do
  case "$arg" in
    --build)  DO_BUILD=1 ;;
    --seed)   DO_SEED=1 ;;
    --tunnel) DO_TUNNEL=1 ;;
    --all)    DO_BUILD=1; DO_SEED=1; DO_TUNNEL=1 ;;
    -h|--help)
      awk 'NR==1{next} /^#/{sub(/^# ?/,""); print; next} {exit}' "$0"; exit 0 ;;
    *) echo "Opción desconocida: $arg (usa --help)"; exit 1 ;;
  esac
done

# ── Estética ──
c_ok=$'\033[32m'; c_warn=$'\033[33m'; c_err=$'\033[31m'; c_dim=$'\033[2m'; c_bold=$'\033[1m'; c_off=$'\033[0m'
log()  { printf '%s\n' "$*"; }
ok()   { printf '%s✓%s %s\n' "$c_ok" "$c_off" "$*"; }
warn() { printf '%s!%s %s\n' "$c_warn" "$c_off" "$*"; }
die()  { printf '%s✗ %s%s\n' "$c_err" "$*" "$c_off" >&2; exit 1; }

# ── docker compose v2 vs v1 ──
if docker compose version >/dev/null 2>&1; then DC="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then DC="docker-compose"
else die "No encuentro 'docker compose'. ¿Está Docker Desktop instalado y arrancado?"; fi

# ── Prerrequisitos ──
command -v java >/dev/null 2>&1 || die "Falta Java (se necesita JDK 21)."
java -version 2>&1 | head -1 | grep -q '"21' || warn "Java no parece la 21: $(java -version 2>&1 | head -1)"
docker info >/dev/null 2>&1 || die "Docker no responde. Arranca Docker Desktop y reintenta."
command -v curl >/dev/null 2>&1 || die "Falta curl."

# Microservicios (arrancan primero) y gateway (al final, hace de proxy).
MICRO="profiles 8081
social 8082
adventures 8083
notifications 8084
analytics 8085"
GATEWAY_PORT=8080

jar_of() { echo "$ROOT/catastrophe-$1/target/catastrophe-$1-0.0.1-SNAPSHOT.jar"; }

# ── 1. Build ──
need_build=$DO_BUILD
for n in profiles social adventures notifications analytics gateway; do
  [ -f "$(jar_of "$n")" ] || need_build=1
done
if [ "$need_build" = "1" ]; then
  log "🔨 Compilando jars (./mvnw -q -DskipTests package)… (esto tarda un poco la primera vez)"
  ./mvnw -q -DskipTests package || die "Falló la compilación. Revisa la salida de Maven."
  ok "Jars compilados."
else
  ok "Jars ya presentes (usa --build para recompilar)."
fi

# ── 2. Infraestructura ──
log "🐳 Levantando infraestructura (Postgres, Redis, Kafka)…"
if $DC up -d --wait >/dev/null 2>&1; then
  ok "Infraestructura saludable (healthchecks)."
else
  $DC up -d || die "Falló 'docker compose up'."
  # Espera de reserva por si la versión de compose no soporta --wait
  wait_tcp() {
    local host=$1 port=$2 name=$3 tries=${4:-60}
    while [ "$tries" -gt 0 ]; do
      if (exec 3<>"/dev/tcp/$host/$port") 2>/dev/null; then exec 3>&- 3<&-; ok "$name accesible."; return 0; fi
      sleep 1; tries=$((tries-1))
    done
    die "$name no respondió en $host:$port."
  }
  wait_tcp localhost 5432 "Postgres"
  wait_tcp localhost 9092 "Kafka"
  sleep 5
fi

# ── 3. Servicios Spring Boot ──
: > "$PID_FILE"
start_service() {
  local name=$1 port=$2 jar; jar="$(jar_of "$name")"
  [ -f "$jar" ] || die "No existe el jar de $name: $jar"
  printf '%s→ arrancando %s (puerto %s)…%s\n' "$c_dim" "$name" "$port" "$c_off"
  java --enable-preview -jar "$jar" > "$LOG_DIR/$name.log" 2>&1 &
  echo "$! $name" >> "$PID_FILE"
}
wait_health() {
  local name=$1 port=$2 tries=90
  while [ "$tries" -gt 0 ]; do
    if curl -fs "http://localhost:$port/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
      ok "$name listo."; return 0
    fi
    sleep 2; tries=$((tries-1))
  done
  die "$name no llegó a estado UP. Mira $LOG_DIR/$name.log"
}

log "🚀 Arrancando microservicios…"
while read -r name port; do
  [ -z "$name" ] && continue
  start_service "$name" "$port"
done <<EOF
$MICRO
EOF
while read -r name port; do
  [ -z "$name" ] && continue
  wait_health "$name" "$port"
done <<EOF
$MICRO
EOF

log "🚪 Arrancando gateway…"
start_service gateway "$GATEWAY_PORT"
wait_health gateway "$GATEWAY_PORT"

# ── 4. Datos de demostración (opcional) ──
SEEDED=0
if [ "$DO_SEED" = "1" ]; then
  log "🌱 Sembrando datos de demostración…"
  if "$SCRIPT_DIR/seed-demo.sh" "http://localhost:$GATEWAY_PORT"; then SEEDED=1; else warn "La siembra terminó con avisos (revisa la salida)."; fi
fi

# ── 5. Túnel público (opcional) ──
TUNNEL_URL=""
if [ "$DO_TUNNEL" = "1" ]; then
  if ! command -v cloudflared >/dev/null 2>&1; then
    warn "Falta 'cloudflared'. Instálalo con:  brew install cloudflared   (lo demás sigue corriendo en local)."
  else
    log "🌐 Abriendo túnel Cloudflare…"
    cloudflared tunnel --url "http://localhost:$GATEWAY_PORT" > "$LOG_DIR/tunnel.log" 2>&1 &
    echo "$! tunnel" >> "$PID_FILE"
    for _ in $(seq 1 30); do
      TUNNEL_URL="$(grep -oE 'https://[a-z0-9.-]+\.trycloudflare\.com' "$LOG_DIR/tunnel.log" | head -1)"
      [ -n "$TUNNEL_URL" ] && break
      sleep 1
    done
    [ -n "$TUNNEL_URL" ] && ok "Túnel público listo." || warn "No pude leer la URL del túnel; revisa $LOG_DIR/tunnel.log"
  fi
fi

# ── Resumen ──
echo
printf '%s════════════════════════════════════════════════════════%s\n' "$c_bold" "$c_off"
printf '%s  🐱 CATastrophe en marcha%s\n' "$c_bold" "$c_off"
printf '%s════════════════════════════════════════════════════════%s\n' "$c_bold" "$c_off"
printf '  App local:    %shttp://localhost:%s%s\n' "$c_bold" "$GATEWAY_PORT" "$c_off"
[ -n "$TUNNEL_URL" ] && printf '  URL pública:  %s%s%s\n' "$c_bold" "$TUNNEL_URL" "$c_off"
if [ "$SEEDED" = "1" ]; then
  printf '  Acceso demo:  usuario %sdemo%s  /  contraseña %sDemo1234!%s\n' "$c_bold" "$c_off" "$c_bold" "$c_off"
fi
printf '  Logs:         %s/\n' "$LOG_DIR"
printf '  Parar todo:   %s./scripts/stop-demo.sh%s\n' "$c_bold" "$c_off"
printf '%s════════════════════════════════════════════════════════%s\n' "$c_bold" "$c_off"
echo
log "Consejo: durante la defensa proyecta http://localhost:$GATEWAY_PORT (no depende de la wifi)."