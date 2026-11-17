#!/usr/bin/env bash
# Aplica índices + seed de preferencias sobre un volumen YA existente.
# Uso: scripts/seed-preferencias.sh
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

MONGO_USER="${MONGO_USER:-ticketya}"
MONGO_PASSWORD="${MONGO_PASSWORD:-mongo_dev}"
CONTENEDOR="${MONGO_CONTENEDOR:-db-mongo-preferencias}"

docker exec -i "$CONTENEDOR" mongosh \
  "mongodb://${MONGO_USER}:${MONGO_PASSWORD}@localhost:27017/preferencias?authSource=admin" \
  --quiet < infrastructure/mongodb/preferencias/init.js
