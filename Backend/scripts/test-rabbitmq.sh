#!/usr/bin/env bash
# Uso:
#   chmod +x test-rabbitmq.sh
#   scripts/test-rabbitmq.sh          # ms-eventos + ms-notificaciones 
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."


SERVICIOS=(ms-eventos ms-notificaciones)

# Apaga todos los contenedores que pueden interfir y usuar solo lo necesaro
docker compose down

docker compose up -d --build "${SERVICIOS[@]}"


echo
echo "Esperando a que ms-eventos responda (1-3 min de espera)"
READY=false
for _ in $(seq 1 90); do
    if curl -sS -m 2 -o /dev/null http://localhost:8082/ 2>/dev/null; then
    READY=true
    echo "  listo."
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
  docker exec db-mysql-eventos mysql -uticketya -pticketya_dev eventos \
    -e "update evento set estado='PUBLICADO' where id='d0000000-0000-4000-8000-000000000001';"

  # Se detiene el consumidor para que el mensaje quede encolado.
  docker stop ms-notificaciones

  curl -sS -m 15 -i -X PATCH http://localhost:8082/eventos/d0000000-0000-4000-8000-000000000001/estado \
    -H 'Content-Type: application/json' \
    -H 'X-User-Id: b0000000-0000-4000-8000-000000000003' \
    -H 'X-User-Role: ADMIN' \
    -d '{"estado":"CANCELADO"}'

  echo
  echo
  echo "Mensaje encolado:"
  curl -sS -u "${RABBITMQ_USER}:${RABBITMQ_PASSWORD}" -X POST \
    http://localhost:15672/api/queues/%2F/notificaciones.eventos/get \
    -H "content-type: application/json" \
    -d '{"count":5,"ackmode":"ack_requeue_true","encoding":"auto"}' \
    | python3 -m json.tool

  echo
  echo "ms-notificaciones quedó detenido. Para que consuma el mensaje: docker start ms-notificaciones"
else
  echo
  echo "Los servicios no respondieron a tiempo; no se ejecuta la cancelacioon de prueba" 
fi
