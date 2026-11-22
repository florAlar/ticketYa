#!/usr/bin/env bash
# TicketYa — inicio completo del stack.
#
# Uso:
#   ./start.sh              usa las imágenes existentes; si falta alguna, hace build
#   ./start.sh --rebuild    fuerza el rebuild de todas las imágenes
#
# No borra volúmenes: los datos existentes se conservan.
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/Backend"

# ---------------------------------------------------------------------------
# Colores y presentación
# ---------------------------------------------------------------------------
if [ -t 1 ] && [ -z "${NO_COLOR:-}" ]; then
  C_RESET=$'\033[0m'
  C_BOLD=$'\033[1m'
  C_DIM=$'\033[2m'
  C_CYAN=$'\033[36m'
  C_GREEN=$'\033[32m'
  C_YELLOW=$'\033[33m'
  C_RED=$'\033[31m'
else
  C_RESET=""
  C_BOLD=""
  C_DIM=""
  C_CYAN=""
  C_GREEN=""
  C_YELLOW=""
  C_RED=""
fi

BAR_ACTIVE=false
CURRENT_PCT=0
PROGRESS_FRAME=0

clear_bar() {
  if $BAR_ACTIVE; then
    printf '\r\033[K'
    BAR_ACTIVE=false
  fi
}

progress() {
  local pct="${1:-0}" label="${2:-}"
  [ -t 1 ] || return 0
  if [ "$pct" -lt 0 ]; then pct=0; fi
  if [ "$pct" -gt 100 ]; then pct=100; fi

  local width=30 filled empty bar rest
  filled=$((pct * width / 100))
  empty=$((width - filled))
  printf -v bar '%*s' "$filled" ''
  bar=${bar// /#}
  printf -v rest '%*s' "$empty" ''
  rest=${rest// /-}

  # Pequeño pulso que recorre la parte completada de la barra.
  if [ "$filled" -gt 0 ] && [ "$pct" -lt 100 ]; then
    local frame=$((PROGRESS_FRAME % filled))
    bar="${bar:0:frame}>${bar:frame+1}"
  fi
  PROGRESS_FRAME=$((PROGRESS_FRAME + 1))

  printf '\r%s[%s%s]%s %s%3d%%%s %s' \
    "$C_CYAN" "$bar" "$rest" "$C_RESET" "$C_BOLD" "$pct" "$C_RESET" "$label"
  BAR_ACTIVE=true
}

progress_done() {
  if $BAR_ACTIVE; then
    printf '\n'
    BAR_ACTIVE=false
  fi
}

info()  { clear_bar; printf '%s[ .. ]%s %s\n' "$C_CYAN" "$C_RESET" "$*"; }
ok()    { clear_bar; printf '%s[ OK ]%s %s\n' "$C_GREEN" "$C_RESET" "$*"; }
warn()  { clear_bar; printf '%s[WARN]%s %s\n' "$C_YELLOW" "$C_RESET" "$*"; }
err()   { clear_bar; printf '%s[FAIL]%s %s\n' "$C_RED" "$C_RESET" "$*" >&2; }
step()  { clear_bar; printf '\n%s%s==> %s%s\n' "$C_BOLD" "$C_CYAN" "$*" "$C_RESET"; }
die()   { err "$*"; exit 1; }

print_banner() {
  printf '%s%s' "$C_CYAN" "$C_BOLD"
  cat <<'EOF'
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║      T I C K E T Y A   ·   inicio del stack                  ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
EOF
  printf '%s' "$C_RESET"
}

clear_terminal() {
  if [ -t 1 ]; then
    if command -v clear >/dev/null 2>&1; then
      clear
    else
      printf '\033[H\033[2J'
    fi
  fi
}

on_error() {
  local code=$?
  clear_bar
  echo
  err "No se pudo completar el inicio (código $code)."
  if [ -d "$BACKEND_DIR" ]; then
    echo
    info "Estado de contenedores:"
    ( cd "$BACKEND_DIR" && docker compose ps ) || true
    echo
    info "Últimas líneas de logs:"
    ( cd "$BACKEND_DIR" && docker compose logs --tail=40 --no-color ) || true
  fi
  exit "$code"
}

trap on_error ERR
trap 'clear_bar; echo; info "Interrumpido por el usuario."; exit 130' INT

clear_terminal
print_banner

# ---------------------------------------------------------------------------
# Parámetros: solo --rebuild
# ---------------------------------------------------------------------------
REBUILD=false
if [ "$#" -gt 1 ]; then
  die "Uso: ./start.sh [--rebuild]"
fi
if [ "$#" -eq 1 ]; then
  case "$1" in
    --rebuild) REBUILD=true ;;
    *) die "Parámetro desconocido: $1. Uso: ./start.sh [--rebuild]" ;;
  esac
fi

# ---------------------------------------------------------------------------
# Permisos
# ---------------------------------------------------------------------------
step "Comprobando permisos"

if [ ! -x "$0" ]; then
  err "El script no tiene permiso de ejecución."
  info "Ejecutá: chmod +x \"$0\""
  exit 1
fi
ok "Script ejecutable"

if ! command -v docker >/dev/null 2>&1; then
  die "Docker no está instalado o no está en el PATH."
fi

if ! docker info >/dev/null 2>&1; then
  err "No se pudo hablar con Docker."
  info "Verificá que el daemon esté corriendo y que tu usuario tenga permiso sobre /var/run/docker.sock."
  info "Opciones: sudo systemctl start docker · sudo usermod -aG docker \$USER · cerrar sesión y volver a entrar."
  exit 1
fi
ok "Docker accesible"

if ! docker compose version >/dev/null 2>&1; then
  die "Docker Compose v2 no está disponible (probá: docker compose version)."
fi
ok "Docker Compose v2 disponible"

# ---------------------------------------------------------------------------
# Requisitos del proyecto
# ---------------------------------------------------------------------------
step "Comprobando requisitos"

if ! command -v curl >/dev/null 2>&1; then
  die "Falta curl (necesario para verificar los servicios por HTTP)."
fi
ok "curl disponible"

if ! command -v ss >/dev/null 2>&1 && ! command -v lsof >/dev/null 2>&1; then
  die "Falta ss o lsof (necesario para comprobar puertos ocupados)."
fi
ok "Herramienta de puertos disponible"

if [ ! -f "$BACKEND_DIR/docker-compose.yml" ]; then
  die "No se encontró Backend/docker-compose.yml."
fi
if ! ( cd "$BACKEND_DIR" && docker compose config >/dev/null 2>&1 ); then
  die "Backend/docker-compose.yml no es válido para Docker Compose."
fi
ok "Compose válido"

# Disco y memoria: avisa o falla antes de construir/levantar.
available_kb="$(df -Pk "$ROOT_DIR" | awk 'NR==2 {print $4}')"
if [ -n "${available_kb:-}" ]; then
  if [ "$available_kb" -lt $((2 * 1024 * 1024)) ]; then
    die "Espacio libre insuficiente en disco (mínimo 2 GB; hay $((available_kb / 1024)) MB)."
  elif [ "$available_kb" -lt $((5 * 1024 * 1024)) ]; then
    warn "Quedan $((available_kb / 1024)) MB libres; el build puede necesitar más espacio."
  else
    ok "Espacio en disco: $((available_kb / 1024)) MB libres"
  fi
fi

if [ -r /proc/meminfo ]; then
  available_ram_kb="$(awk '/^MemAvailable:/ {print $2}' /proc/meminfo)"
  if [ -n "${available_ram_kb:-}" ]; then
    if [ "$available_ram_kb" -lt $((512 * 1024)) ]; then
      die "Memoria disponible insuficiente (mínimo 512 MB; hay $((available_ram_kb / 1024)) MB)."
    elif [ "$available_ram_kb" -lt $((1536 * 1024)) ]; then
      warn "Quedan $((available_ram_kb / 1024)) MB de RAM; el stack puede quedar justo."
    else
      ok "Memoria disponible: $((available_ram_kb / 1024)) MB"
    fi
  fi
fi

step "Limpiando contenedores previos"

# down sin -v: elimina contenedores/redes viejos pero conserva los volúmenes.
# Evita conflictos de nombres fijos (ms-*, db-*) y deja los puertos libres.
( cd "$BACKEND_DIR" && docker compose down --remove-orphans >/dev/null 2>&1 ) || true
ok "Entorno de contenedores limpio"

port_in_use() {
  local port="$1"
  if command -v ss >/dev/null 2>&1; then
    ss -ltn 2>/dev/null | awk 'NR > 1 {print $4}' | grep -Eq "[:.]${port}$"
  else
    lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
  fi
}

if [ -n "$(cd "$BACKEND_DIR" && docker compose ps -q 2>/dev/null)" ]; then
  warn "Ya hay contenedores de TicketYa; se reutilizan y se omite el chequeo de puertos."
else
  PORTS=(80 3000 8080 8081 8082 8083 8084 8085 8086 3307 3308 3309 3310 3311 5672 6380 1025 8025 15672 27018)
  ocupados=()
  for port in "${PORTS[@]}"; do
    if port_in_use "$port"; then
      ocupados+=("$port")
    fi
  done
  if [ "${#ocupados[@]}" -gt 0 ]; then
    err "Puertos ocupados: ${ocupados[*]}"
    info "Liberá esos puertos o bajá el proceso que los está usando antes de iniciar TicketYa."
    exit 1
  fi
  ok "Puertos libres"
fi

# ---------------------------------------------------------------------------
# Imágenes: build solo si falta alguna o si se pidió --rebuild
# ---------------------------------------------------------------------------
step "Comprobando imágenes"

IMAGES=()
while IFS= read -r image; do
  [ -n "$image" ] && IMAGES+=("$image")
done < <(cd "$BACKEND_DIR" && docker compose config --images 2>/dev/null | sort -u)

missing=false
for image in "${IMAGES[@]}"; do
  if ! docker image inspect "$image" >/dev/null 2>&1; then
    missing=true
    info "Falta la imagen: $image"
  fi
done

if $REBUILD || $missing; then
  if $REBUILD; then
    info "--rebuild: reconstruyendo todas las imágenes..."
  else
    info "Faltan imágenes: construyendo..."
  fi
  progress 20 "Construyendo imágenes..."
  progress_done
  ( cd "$BACKEND_DIR" && docker compose build ) || die "Falló el build de las imágenes."
  progress 25 "Imágenes construidas"
  ok "Imágenes construidas"
else
  progress 25 "Imágenes existentes"
  ok "Imágenes existentes"
fi

# ---------------------------------------------------------------------------
# Funciones de espera
# ---------------------------------------------------------------------------
wait_healthy() {
  local service="$1"
  local container status=""
  local start_pct="${CURRENT_PCT:-0}"
  local span=2 pct
  local unhealthy_ticks=0

  # Se resuelve el ID en cada chequeo: Compose puede crear/recrear el contenedor
  # mientras MySQL corre el init.sql. Se anima a 10 fps y se consulta cada 1 s.
  local ticks=6000
  for i in $(seq 1 "$ticks"); do
    pct=$((start_pct + (i * span / ticks)))
    progress "$pct" "Esperando $service..."
    if [ $((i % 10)) -eq 0 ]; then
      container="$(cd "$BACKEND_DIR" && docker compose ps -q "$service" 2>/dev/null || true)"
      if [ -z "$container" ]; then
        status="missing"
      else
        status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || echo "missing")"
      fi

      case "$status" in
        healthy|running)
          progress $((start_pct + span)) "$service listo"
          ok "$service ($status)"
          return 0
          ;;
        restarting|exited|dead|unhealthy)
          unhealthy_ticks=$((unhealthy_ticks + 1))
          if [ "$unhealthy_ticks" -ge 10 ]; then
            die "$service está en estado '$status'. Revisá 'docker compose logs $service'; si el volumen quedó corrupto, recreá las bases con: docker compose down -v (borra los datos)."
          fi
          ;;
        *)
          unhealthy_ticks=0
          ;;
      esac
    fi
    sleep 0.1
  done
  die "Timeout esperando $service (último estado: ${status:-desconocido})."
}

wait_http() {
  local name="$1"
  local url="$2"
  shift 2
  local code=""
  local start_pct="${CURRENT_PCT:-0}"
  local span=4 pct
  local ticks=6000
  for i in $(seq 1 "$ticks"); do
    pct=$((start_pct + (i * span / ticks)))
    progress "$pct" "Esperando $name..."
    if [ $((i % 10)) -eq 0 ]; then
      code="$(curl -s -o /dev/null -m 3 -w '%{http_code}' "$url" 2>/dev/null || true)"
      for expected in "$@"; do
        if [ "$code" = "$expected" ]; then
          progress $((start_pct + span)) "$name respondió"
          ok "$name HTTP $code"
          return 0
        fi
      done
    fi
    sleep 0.1
  done
  die "Timeout esperando $name en $url (último HTTP: ${code:-000})."
}

check_http() {
  local pct="$1" name="$2" url="$3"
  shift 3
  CURRENT_PCT="$pct"
  wait_http "$name" "$url" "$@"
}

compose_up() {
  local attempt
  for attempt in 1 2 3; do
    if ( cd "$BACKEND_DIR" && docker compose up -d "$@" ); then
      return 0
    fi
    warn "Falló el arranque (intento $attempt/3); reintentando..."
    ( cd "$BACKEND_DIR" && docker compose rm -sf "$@" >/dev/null 2>&1 ) || true
    sleep 2
  done
  return 1
}

# ---------------------------------------------------------------------------
# Arranque: primero infraestructura, después aplicaciones
# ---------------------------------------------------------------------------
step "Levantando bases e infraestructura"

progress 28 "Levantando bases e infraestructura..."
progress_done
compose_up \
  db-mysql-usuarios db-mysql-eventos db-mysql-ventas db-mysql-entradas db-mysql-notificaciones \
  db-mongo-preferencias db-redis-ventas rabbitmq mailhog \
  || die "Falló el arranque de las bases/infraestructura."

CURRENT_PCT=30
for service in db-mysql-usuarios db-mysql-eventos db-mysql-ventas db-mysql-entradas \
    db-mysql-notificaciones db-mongo-preferencias db-redis-ventas rabbitmq; do
  wait_healthy "$service"
  CURRENT_PCT=$((CURRENT_PCT + 2))
done
CURRENT_PCT=46
wait_http "mailhog" "http://localhost:8025/api/v2/messages" 200
CURRENT_PCT=50
wait_http "rabbitmq (management)" "http://localhost:15672/" 200

step "Levantando gateway, microservicios y frontend"

progress 56 "Levantando aplicaciones..."
progress_done
compose_up \
  ms-gateway ms-usuarios ms-eventos ms-ventas ms-entradas ms-preferencias ms-notificaciones ms-frontend \
  || die "Falló el arranque de las aplicaciones."

check_http 60 "ms-gateway" "http://localhost:8080/" 200
check_http 64 "ms-usuarios" "http://localhost:8081/" 200
check_http 68 "ms-eventos" "http://localhost:8082/" 200
check_http 72 "ms-ventas" "http://localhost:8083/" 200
check_http 76 "ms-entradas" "http://localhost:8084/" 200
check_http 80 "ms-preferencias" "http://localhost:8085/preferencias/me" 200 401 403
check_http 84 "ms-notificaciones" "http://localhost:8086/" 200
check_http 88 "ms-frontend (:80)" "http://localhost/" 200
check_http 92 "ms-frontend (:3000)" "http://localhost:3000/" 200

# ---------------------------------------------------------------------------
# Resumen
# ---------------------------------------------------------------------------
progress 100 "Verificación completa"
progress_done
echo
printf '%s%s  ✔ TicketYa está arriba%s\n' "$C_GREEN" "$C_BOLD" "$C_RESET"
printf '  %s→%s http://localhost\n' "$C_CYAN" "$C_RESET"
printf '  %s→%s http://localhost:3000\n' "$C_CYAN" "$C_RESET"
printf '  %s→%s Gateway: http://localhost:8080\n' "$C_CYAN" "$C_RESET"
echo
printf '%sLos 17 contenedores respondieron correctamente.%s\n' "$C_DIM" "$C_RESET"

if [ ! -t 0 ] || [ ! -t 1 ]; then
  echo
  info "Modo no interactivo: el stack queda levantado."
  info "Para detenerlo: cd Backend && docker compose down"
  exit 0
fi

echo
printf '  %sOpciones:%s\n' "$C_BOLD" "$C_RESET"
printf '    %sd%s  Detener TicketYa y apagar los contenedores (conserva los datos)\n' "$C_CYAN" "$C_RESET"
printf '\n  Presioná %s"d"%s para detener: ' "$C_BOLD" "$C_RESET"

while true; do
  if ! IFS= read -rsn1 key; then
    echo
    info "Sin entrada; el stack queda levantado."
    exit 0
  fi
  case "$key" in
    d|D)
      echo
      step "Deteniendo TicketYa"
      ( cd "$BACKEND_DIR" && docker compose down --remove-orphans ) \
        || die "No se pudieron apagar los contenedores."
      ok "Contenedores apagados. Los datos quedaron guardados."
      exit 0
      ;;
    *)
      printf '\r\033[K  Opción no válida. Presioná %s"d"%s para detener: ' "$C_BOLD" "$C_RESET"
      ;;
  esac
done
