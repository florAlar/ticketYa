#!/usr/bin/env bash
set -euo pipefail

PLAN=${1:-compra-con-fila.jmx}      # o compra-normal.jmx
CANT_USERS=${2:-25}
CSV="$PWD/usuarios.csv"

if [ "$PLAN" = compra-con-fila.jmx ]; then
  EVENTO="d0000000-0000-4000-8000-000000000001"
  SECTOR="c0000000-0000-4000-8000-000000000011"
else
  EVENTO="d0000000-0000-4000-8000-000000000006"
  SECTOR="c0000000-0000-4000-8000-000000000015"
fi

OUT="$CSV" BASE="http://localhost:8080" \
  ./generar-usuarios.sh "$CANT_USERS" "$(( $(date +%s) % 1000000 ))"

rm -rf report corrida.jtl     # JMeter no pisa un reporte existente

jmeter -n -t "$PLAN" \
  -Jhost=localhost -Jport=8080 \
  -JidEvento="$EVENTO" -JidSector="$SECTOR" \
  -Jusuarios="$CANT_USERS" -Jramp="$(( CANT_USERS / 2 + 1 ))" \
  -Jiteraciones=1 -JloopForever=false \
  -Jcsv="$CSV" \
  -l corrida.jtl -e -o report
