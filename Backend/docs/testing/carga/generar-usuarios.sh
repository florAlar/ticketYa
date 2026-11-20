#!/usr/bin/env bash
# Genera el pool `email,token` que consumen los planes JMeter de esta carpeta.
#
# Registra (si no existen) las cuentas cargaNN@ejemplo.local contra el gateway,
# hace login y escribe los JWT en usuarios.csv.
#
# Uso:
#   ./generar-usuarios.sh [cantidad] [desde]
#   ./generar-usuarios.sh 25 51      -> carga51..carga75
#
# Requiere: stack arriba, curl y jq. Los JWT vencen a la hora (exp = iat + 3600),
# así que hay que regenerar el archivo antes de cada corrida.
set -euo pipefail

BASE=${BASE:-http://localhost:8080}
CANTIDAD=${1:-25}
DESDE=${2:-1}
PASSWORD=${PASSWORD:-Carga1234}
OUT=${OUT:-"$(cd "$(dirname "$0")" && pwd)/usuarios.csv"}

echo "email,token" >"$OUT"
ok=0
fail=0
for i in $(seq "$DESDE" $((DESDE + CANTIDAD - 1))); do
    email="carga${i}@ejemplo.local"
    curl -s -o /dev/null -X POST "$BASE/api/auth/register" \
        -H 'Content-Type: application/json' \
        -d "{\"nombre\":\"Carga ${i}\",\"apellido\":\"Test\",\"email\":\"${email}\",\"password\":\"${PASSWORD}\"}"
    token=$(curl -s -X POST "$BASE/api/auth/login" \
        -H 'Content-Type: application/json' \
        -d "{\"email\":\"${email}\",\"password\":\"${PASSWORD}\"}" \
        | jq -r '.token // empty')
    if [ -n "$token" ]; then
        echo "${email},${token}" >>"$OUT"
        ok=$((ok + 1))
    else
        echo "FALLO login ${email}" >&2
        fail=$((fail + 1))
    fi
done

echo "tokens ok=${ok} fail=${fail} -> ${OUT}"
