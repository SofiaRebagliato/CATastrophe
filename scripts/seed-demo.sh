#!/usr/bin/env bash
#
# seed-demo.sh — Siembra datos de demostración en CATastrophe a través de la API pública.
#
# Crea: una cuenta (demo / Demo1234!), dos gatos, varios meows, una aventura activa,
# una aventura completada (que otorga una insignia y aparece en el historial) y un reto pendiente.
#
# Uso:   ./scripts/seed-demo.sh [BASE_URL]
#        ./scripts/seed-demo.sh                       # usa http://localhost:8080
#        ./scripts/seed-demo.sh https://xxx.trycloudflare.com
#
# Es idempotente en lo esencial: si la cuenta ya existe, continúa con el login.

set -uo pipefail

BASE="${1:-http://localhost:8080}"
COOKIE="$(mktemp -t catastrophe-cookies.XXXXXX)"
trap 'rm -f "$COOKIE"' EXIT

c_ok=$'\033[32m'; c_warn=$'\033[33m'; c_off=$'\033[0m'
ok()   { printf '%s✓%s %s\n' "$c_ok" "$c_off" "$*"; }
warn() { printf '%s!%s %s\n' "$c_warn" "$c_off" "$*"; }

# Extrae un campo de un objeto JSON por stdin:  echo "$json" | json_field id
json_field() {
  local field="$1"
  if command -v python3 >/dev/null 2>&1; then
    python3 -c '
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get(sys.argv[1], "") if isinstance(d, dict) else "")
except Exception:
    print("")' "$field"
  else
    sed -n 's/.*"'"$field"'"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1
  fi
}
# Primer "id" de un array JSON por stdin
first_array_id() {
  if command -v python3 >/dev/null 2>&1; then
    python3 -c '
import sys, json
try:
    a = json.load(sys.stdin); print(a[0]["id"] if a else "")
except Exception:
    print("")'
  else
    sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1
  fi
}

api() { # api METHOD PATH [JSON_BODY] [X-Cat-Id]
  local method=$1 path=$2 body=${3:-} cat=${4:-}
  local args=(-s -c "$COOKIE" -b "$COOKIE" -X "$method" "$BASE$path" -H 'Content-Type: application/json')
  [ -n "$cat" ]  && args+=(-H "X-Cat-Id: $cat")
  [ -n "$body" ] && args+=(-d "$body")
  curl "${args[@]}"
}

echo "→ Sembrando en $BASE"

# ── 1. Cuenta ──
# Registro (si ya existe, el login posterior lo resuelve igualmente)
api POST /api/v1/auth/register \
  '{"username":"demo","email":"demo@catastrophe.cat","displayName":"Sofía (demo)","password":"Demo1234!"}' >/dev/null
api POST /api/v1/auth/login '{"username":"demo","password":"Demo1234!"}' >/dev/null
if ! curl -fs -b "$COOKIE" "$BASE/api/v1/auth/me" >/dev/null 2>&1; then
  warn "No se pudo iniciar sesión como 'demo'. ¿Está el gateway arriba? Abortando siembra."
  exit 1
fi
ok "Sesión iniciada (demo)."

# ── 2. Gatos ──
make_cat() { # nombre raza bio  → imprime el id
  api POST /api/v1/cats "{\"name\":\"$1\",\"breed\":\"$2\",\"bio\":\"$3\"}" | json_field id
}
CAT1=$(make_cat "Pelusa"   "Común europeo" "Cazadora de puntos rojos y profesional de la siesta.")
CAT2=$(make_cat "Bigotes"  "Siamés"        "Estratega de cajas de cartón. Maúlla en tres idiomas.")
[ -n "$CAT1" ] && ok "Gato creado: Pelusa ($CAT1)"   || warn "No pude crear a Pelusa."
[ -n "$CAT2" ] && ok "Gato creado: Bigotes ($CAT2)"  || warn "No pude crear a Bigotes."
[ -z "$CAT1" ] && { warn "Sin gato principal no puedo seguir."; exit 1; }

# ── 3. Meows ──
post_meow() { api POST /api/v1/posts "{\"content\":\"$1\",\"imageUrl\":null,\"postType\":\"MEOW\"}" "" "$CAT1" >/dev/null; }
post_meow "Hoy he conquistado el alféizar de la ventana. La vista es excelente. 🐾"
post_meow "Reunión de gatos en el tejado a medianoche. Agenda: maullar."
post_meow "He decidido que la caja nueva es mía. El sofá caro puede esperar."
ok "Meows publicados."

# ── 4. Aventuras: una completada (historial + insignia) y otra activa ──
ADVS_JSON=$(curl -s "$BASE/api/v1/adventures")
ADV1=$(echo "$ADVS_JSON" | first_array_id)
ADV2=$(echo "$ADVS_JSON" | python3 -c 'import sys,json;a=json.load(sys.stdin);print(a[1]["id"] if len(a)>1 else "")' 2>/dev/null)

if [ -n "$ADV1" ]; then
  CA1=$(api POST /api/v1/adventures/start "{\"adventureId\":\"$ADV1\"}" "" "$CAT1" | json_field id)
  if [ -n "$CA1" ]; then
    api POST "/api/v1/adventures/$CA1/complete" "" "" "$CAT1" >/dev/null
    ok "Aventura completada (historial + insignia)."
  fi
fi
if [ -n "$ADV2" ]; then
  CA2=$(api POST /api/v1/adventures/start "{\"adventureId\":\"$ADV2\"}" "" "$CAT1" | json_field id)
  if [ -n "$CA2" ]; then
    api PATCH "/api/v1/adventures/$CA2/progress" '{"progressPct":25}' "$CAT1" >/dev/null 2>&1 \
      || api PATCH "/api/v1/adventures/$CA2/progress" '{"progressPct":25}' "" "$CAT1" >/dev/null 2>&1
    ok "Aventura activa al 25%."
  fi
fi

# ── 5. Reto pendiente ──
CH1=$(curl -s "$BASE/api/v1/challenges" | first_array_id)
if [ -n "$CH1" ]; then
  api POST /api/v1/challenges/create "{\"challengeId\":\"$CH1\"}" "" "$CAT1" >/dev/null
  ok "Reto creado (pendiente de oponente)."
fi

echo
ok "Siembra completada. Entra con  usuario: demo  /  contraseña: Demo1234!"
