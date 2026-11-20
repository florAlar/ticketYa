#!/usr/bin/env bash
# Muestra el estado de los contenedores
#
# Uso:
#   scripts/status.sh                # estado de los contenedores (rápido, no usa red)
#   scripts/status.sh --http         # además hace un smoke HTTP a cada servicio publicado
#   scripts/status.sh --watch        # refresca cada 5s (Ctrl+C para salir)
#   scripts/status.sh --watch 2      # refresca cada 2s
#
# Código de salida: 0 = todo arriba (y los smoke HTTP responden);
#                   1 = falta/algo no está corriendo o un smoke HTTP falló;
#                   2 = Docker no está disponible (daemon apagado o sin permisos).
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

HTTP=false
WATCH=0

usage() {
  sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'
}

for arg in "$@"; do
  case "$arg" in
    --http) HTTP=true ;;
    -w|--watch) WATCH=5 ;;
    --watch=*) WATCH="${arg#*=}" ;;
    -h|--help) usage; exit 0 ;;
    *)
      if [[ "$arg" =~ ^[0-9]+$ && "$WATCH" -ne 0 ]]; then
        WATCH="$arg"
      else
        echo "Opción desconocida: $arg" >&2
        usage >&2
        exit 1
      fi
      ;;
  esac
done

if [ "$WATCH" != "0" ] && ! [[ "$WATCH" =~ ^[1-9][0-9]*$ ]]; then
  echo "Intervalo inválido para --watch: $WATCH (usá segundos enteros, ej. --watch 2)" >&2
  exit 1
fi

# Estado de cada servicio esperado según los compose incluidos.
EXPECTED="$(docker compose config --services 2>/dev/null | sort)"

if ! docker info >/dev/null 2>&1; then
  echo "No se pudo hablar con Docker." >&2
  echo "Verificá que el daemon esté corriendo y que tu usuario tenga permiso sobre /var/run/docker.sock" >&2
  echo "(por ejemplo: sudo systemctl start docker && sudo usermod -aG docker \$USER, luego reabrí la sesión)." >&2
  exit 2
fi

if [ -t 1 ]; then
  C_OK=$'\033[32m'; C_WARN=$'\033[33m'; C_BAD=$'\033[31m'; C_DIM=$'\033[2m'; C_OFF=$'\033[0m'
else
  C_OK=""; C_WARN=""; C_BAD=""; C_DIM=""; C_OFF=""
fi

# URL de smoke por servicio. Los que no tienen HTTP (DBs, RabbitMQ) se omiten.
http_url_for() {
  case "$1" in
    ms-gateway)       echo "http://localhost:8080/" ;;
    ms-usuarios)      echo "http://localhost:8081/" ;;
    ms-eventos)       echo "http://localhost:8082/" ;;
    ms-ventas)        echo "http://localhost:8083/" ;;
    ms-entradas)      echo "http://localhost:8084/" ;;
    ms-preferencias)  echo "http://localhost:8085/" ;;
    ms-notificaciones) echo "http://localhost:8086/" ;;
    ms-frontend)      echo "http://localhost:3000/" ;;
    mailhog)          echo "http://localhost:8025/" ;;
    *) echo "" ;;
  esac
}

paint_state() {
  local state="$1" cell
  printf -v cell '%-12s' "$state"
  case "$state" in
    running)     printf '%s%s%s' "$C_OK" "$cell" "$C_OFF" ;;
    restarting|paused|created) printf '%s%s%s' "$C_WARN" "$cell" "$C_OFF" ;;
    exited|dead) printf '%s%s%s' "$C_BAD" "$cell" "$C_OFF" ;;
    *) printf '%s' "$cell" ;;
  esac
}

row_line() {
  if $HTTP; then
    printf '%-24s %-22s %s %-34s %s' "$1" "$2" "$3" "$4" "$5"
  else
    printf '%-24s %-22s %s %-34s' "$1" "$2" "$3" "$4"
  fi
}

render() {
  # Nombre | Servicio | Estado | Detalle (status con health) | HTTP
  local rows=() name svc state status missing=0 not_running=0 http_fail=0 absent
  local seen=" "

  while IFS=$'\t' read -r name svc state status; do
    [ -z "$name" ] && continue
    seen+="$svc "

    local http_cell=""
    if $HTTP; then
      local url
      url="$(http_url_for "$svc")"
      if [ -z "$url" ]; then
        http_cell="-"
      elif [ "$state" != "running" ]; then
        http_cell="skip"
      else
        local code
        code="$(curl -s -o /dev/null -m 3 -w '%{http_code}' "$url" 2>/dev/null || true)"
        if [ -n "$code" ] && [ "$code" != "000" ]; then
          http_cell="${code}"
        else
          http_cell="sin respuesta"
          http_fail=$((http_fail + 1))
        fi
      fi
    fi

    [ "$state" != "running" ] && not_running=$((not_running + 1))
    rows+=("$(row_line "$name" "$svc" "$(paint_state "$state")" "$status" "$http_cell")")
  done < <(docker compose ps -a --format '{{.Name}}	{{.Service}}	{{.State}}	{{.Status}}' 2>/dev/null)

  local total=0
  while read -r svc; do
    [ -z "$svc" ] && continue
    total=$((total + 1))
    if [[ "$seen" != *" $svc "* ]]; then
      missing=$((missing + 1))
      printf -v absent '%-12s' "ausente"
      rows+=("$(row_line "$svc" "$svc" "$(printf '%s%s%s' "$C_BAD" "$absent" "$C_OFF")" "sin contenedor" "-")")
    fi
  done <<< "$EXPECTED"

  echo "TicketYa · estado de contenedores · $(date '+%Y-%m-%d %H:%M:%S')"
  echo
  if [ "${#rows[@]}" -eq 0 ]; then
    echo "  (no hay contenedores creados todavía)"
  else
    printf '%-24s %-22s %-12s %-34s' "CONTENEDOR" "SERVICIO" "ESTADO" "DETALLE"
    $HTTP && printf ' %s' "HTTP"
    echo
    printf '%s\n' "${rows[@]}"
  fi
  echo

  local up=$((total - missing - not_running))
  local summary="Servicios: ${up}/${total} corriendo"
  [ "$missing" -gt 0 ] && summary+=" · ${missing} sin crear"
  [ "$not_running" -gt 0 ] && summary+=" · ${not_running} detenidos"
  $HTTP && [ "$http_fail" -gt 0 ] && summary+=" · ${http_fail} sin respuesta HTTP"

  if [ $((missing + not_running)) -eq 0 ] && [ "$http_fail" -eq 0 ]; then
    echo "${C_OK}${summary}${C_OFF}"
    return 0
  fi
  echo "${C_WARN}${summary}${C_OFF}"
  return 1
}

if [ "$WATCH" -gt 0 ]; then
  trap 'printf "\033[?25h"; exit 0' INT TERM
  printf '\033[?25l'
  while true; do
    printf '\033[H\033[2J'
    render || true
    echo
    echo "${C_DIM}Refrescando cada ${WATCH}s · Ctrl+C para salir${C_OFF}"
    sleep "$WATCH"
  done
fi

render
