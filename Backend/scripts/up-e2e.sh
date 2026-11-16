#!/usr/bin/env bash
# Levanta el mínimo necesario para probar eventos -> RabbitMQ -> notificaciones.
#
# Uso:
#   scripts/up-e2e.sh          # ms-eventos + ms-notificaciones + ms-ventas
#   scripts/up-e2e.sh --down   # detiene esos servicios
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

DOWN=false
for arg in "$@"; do
  case "$arg" in
    --down) DOWN=true ;;
    -h|--help) sed -n '2,6p' "$0"; exit 0 ;;
    *) echo "Opción desconocida: $arg" >&2; exit 1 ;;
  esac
done

SERVICES=(ms-eventos)

if $DOWN; then
  docker compose stop "${SERVICES[@]}"
  exit 0
fi

# Apaga el resto del stack para quedarnos solo con lo necesario (no borra volúmenes).
docker compose down

docker compose up -d --build "${SERVICES[@]}"

echo
echo "Esperando a que ms-eventos y ms-notificaciones respondan (1-3 min en frío)..."
READY=false
for _ in $(seq 1 90); do
  if curl -fsS -m 2 http://localhost:8082/ >/dev/null 2>&1 \;  then
    READY=true
    echo "  listos."
    break
  fi
  sleep 2
done

echo
echo "Colas en RabbitMQ:"
RABBITMQ_USER="${RABBITMQ_USER:-ticketya}"
RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-ticketya_dev}"
curl -fsS -m 5 -u "${RABBITMQ_USER}:${RABBITMQ_PASSWORD}" http://localhost:15672/api/queues \
  | python3 -c 'import json, sys
data = json.load(sys.stdin)
rows = data if isinstance(data, list) else [data]
if not rows:
    print("  (todavía no hay colas)")
for q in rows:
    if isinstance(q, dict):
        print("  %s  consumers=%s  msgs=%s" % (q.get("name", "?"), q.get("consumers", "?"), q.get("messages", "?")))' \
  || echo "  (no se pudo consultar la API; abrí http://localhost:15672 con ${RABBITMQ_USER})"

if $READY; then
  echo
  echo "Cancelando evento de prueba (d0000000-0000-4000-8000-000000000001)..."
  docker exec db-mysql-eventos mysql -uticketya -pticketya_dev eventos -e "update evento set estado='PUBLICADO' where id='d0000000-0000-4000-8000-000000000001';"
  docker stop ms-notificaciones
  curl -sS -m 15 -i -X PATCH http://localhost:8082/eventos/d0000000-0000-4000-8000-000000000001/estado \
    -H 'Content-Type: application/json' \
    -H 'X-User-Id: b0000000-0000-4000-8000-000000000003' \
    -H 'X-User-Role: ADMIN' \
    -d '{"estado":"CANCELADO"}'

  echo
  echo "Mensaje encolado"
  curl -u "${RABBITMQ_USER}:${RABBITMQ_PASSWORD}" -X POST \
  http://localhost:15672/api/queues/%2F/notificaciones.eventos/get \
  -H "content-type: application/json" \
  -d '{"count":5,"ackmode":"ack_requeue_true","encoding":"auto"}'
else
  echo
  echo "Los servicios no respondieron a tiempo; no se ejecuta la cancelación de prueba."
  echo "Revisá: docker compose logs --tail 80 ms-eventos ms-notificaciones"
fi
