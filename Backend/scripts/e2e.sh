#!/usr/bin/env bash
# E2-TST-01 · Nivel 2 — escenarios E2E F1-F5 contra el stack Docker Compose.
#
# Uso:
#   scripts/e2e.sh                 # limpia, levanta, corre los E2E y limpia al final
#   scripts/e2e.sh --keep          # deja el stack levantado para inspeccionar
#   scripts/e2e.sh -Dit.test=CompraConfirmadaE2E   # un escenario (acepta también -Dtest=)
#
# Requisitos: Docker accesible por el usuario y Maven. El script borra los volúmenes
# (docker compose down -v) antes y después: no usar sobre datos que se quieran conservar.
#
# Si el grupo docker no está activo en la sesión:
#   newgrp docker            # o cerrar sesión y volver a entrar
#   sg docker -c './scripts/e2e.sh'
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

SERVICES=(
  ms-gateway
  ms-usuarios
  ms-eventos
  ms-ventas
  ms-entradas
  ms-preferencias
  ms-notificaciones
)

KEEP=false
MVN_ARGS=()
for arg in "$@"; do
  case "$arg" in
    --keep) KEEP=true ;;
    -h|--help)
      sed -n '2,16p' "$0"
      exit 0
      ;;
    -Dtest=*)
      # Failsafe usa it.test; aceptamos la forma de Surefire por comodidad.
      MVN_ARGS+=("-Dit.test=${arg#-Dtest=}")
      ;;
    *) MVN_ARGS+=("$arg") ;;
  esac
done

if [ -n "${E2E_BASE_URL:-}" ]; then
  MVN_ARGS+=("-De2e.baseUrl=${E2E_BASE_URL}")
fi
if [ -n "${E2E_MAILHOG_URL:-}" ]; then
  MVN_ARGS+=("-De2e.mailhogUrl=${E2E_MAILHOG_URL}")
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker no está accesible para el usuario actual." >&2
  echo "  Opción 1: cerrar sesión y volver a entrar (el grupo docker debe estar activo)." >&2
  echo "  Opción 2: newgrp docker" >&2
  echo "  Opción 3: sg docker -c './scripts/e2e.sh'" >&2
  exit 1
fi

cleanup() {
  if $KEEP; then
    echo "==> --keep: el stack queda levantado (limpiar con: docker compose down -v)"
  else
    echo "==> Bajando el stack y borrando volúmenes"
    docker compose down -v --remove-orphans || true
  fi
}
trap cleanup EXIT

dump_fallo() {
  echo "==> Falló el arranque; estado y últimas líneas de cada contenedor:" >&2
  docker compose ps -a >&2 || true
  docker compose logs --tail 40 --no-color >&2 || true
}

wait_healthy() {
  local nombre=$1
  local estado=""
  for _ in $(seq 1 160); do
    estado=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$nombre" 2>/dev/null || echo ausente)
    if [ "$estado" = "healthy" ]; then
      echo "  OK  $nombre"
      return 0
    fi
    sleep 3
  done
  echo "  TIMEOUT $nombre (estado: ${estado:-desconocido})" >&2
  return 1
}

wait_http() {
  local url=$1
  shift
  local code=""
  local ok
  for _ in $(seq 1 120); do
    code=$(curl -s -o /dev/null -w '%{http_code}' -m 3 "$url" 2>/dev/null || true)
    ok=false
    for esperado in "$@"; do
      [ "$code" = "$esperado" ] && ok=true
    done
    if $ok; then
      echo "  OK  $url ($code)"
      return 0
    fi
    sleep 3
  done
  echo "  TIMEOUT $url (último código: ${code:-000})" >&2
  return 1
}

echo "==> Base limpia (down -v)"
docker compose down -v --remove-orphans

echo "==> Construyendo imágenes del gateway y los 6 microservicios"
if ! docker compose build "${SERVICES[@]}"; then
  dump_fallo
  exit 1
fi

# Bases e infraestructura primero. El entrypoint de MySQL levanta un servidor temporal sin red
# mientras corre init.sql: el healthcheck por TCP falla en esa ventana y, si arrancamos los 17
# contenedores a la vez, el init compite por I/O y puede agotar los retries.
INFRA=(
  db-mysql-usuarios
  db-mysql-eventos
  db-mysql-ventas
  db-mysql-entradas
  db-mysql-notificaciones
  db-mongo-preferencias
  db-redis-ventas
  rabbitmq
  mailhog
)

echo "==> Levantando bases e infraestructura"
if ! docker compose up -d "${INFRA[@]}"; then
  dump_fallo
  exit 1
fi

echo "==> Esperando bases e infraestructura"
for contenedor in db-mysql-usuarios db-mysql-eventos db-mysql-ventas db-mysql-entradas \
    db-mysql-notificaciones db-mongo-preferencias db-redis-ventas rabbitmq; do
  wait_healthy "$contenedor"
done
wait_http http://localhost:8025/api/v2/messages 200

echo "==> Levantando gateway + 6 microservicios"
if ! docker compose up -d "${SERVICES[@]}"; then
  dump_fallo
  exit 1
fi

echo "==> Esperando servicios"
wait_http http://localhost:8080/ 200
wait_http http://localhost:8081/ 200
wait_http http://localhost:8082/ 200
wait_http http://localhost:8083/ 200
wait_http http://localhost:8084/ 200
# preferencias no expone "/": sin token responde 401/403, con token 200.
wait_http http://localhost:8085/preferencias/me 401 403 200
wait_http http://localhost:8086/ 200

echo "==> Escenarios E2E (F1-F5)"
mvn -f e2e-tests/pom.xml -Pe2e verify "${MVN_ARGS[@]}"
