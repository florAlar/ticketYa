#!/usr/bin/env bash
# Importa las compras APROBADAS de msvc-ventas al historial de msvc-preferencias.
# Es idempotente: se puede ejecutar las veces que haga falta.
# Uso: scripts/backfill-preferencias.sh
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

PREFERENCIAS_URL="${PREFERENCIAS_URL:-http://localhost:8085}"
INTERNAL_TOKEN="${TICKETYA_INTERNAL_TOKEN:-ticketya_dev_internal}"

echo "Backfill de compras -> ${PREFERENCIAS_URL}/internas/backfill"
curl -fsS -m 120 -X POST \
  -H "X-Internal-Token: ${INTERNAL_TOKEN}" \
  "${PREFERENCIAS_URL}/internas/backfill"
echo
