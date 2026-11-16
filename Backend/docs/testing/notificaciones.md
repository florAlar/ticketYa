# Testing completo — notificaciones (E2-NOT)

Guía para verificar el backend de notificaciones de punta a punta:
compra confirmada, cancelación/suspensión/modificación de evento y artista favorito + ciudad.

> Atajo: si querés empezar de cero con datos seed nuevos, desde `Backend/`:
> `docker compose down -v && docker compose up -d --build`
> (destructivo: borra los volúmenes). Si conservás volúmenes viejos, aplicá la migración del final.

---

## 0. Requisitos y arranque

Desde `Backend/`:

```bash
docker compose up -d --build
```

Verificar:

```bash
curl http://localhost:8080/                       # api gateway ok!
curl -u ticketya:ticketya_dev http://localhost:15672/api/queues/%2F/notificaciones.eventos
curl -u ticketya:ticketya_dev http://localhost:15672/api/queues/%2F/notificaciones.compras
```

La segunda y tercera consulta deben devolver la cola (si el servicio todavía está arrancando,
esperar 1–2 minutos). Panel Rabbit: <http://localhost:15672> (`ticketya` / `ticketya_dev`).

MailHog (bandeja de email real de prueba): <http://localhost:8025>

```bash
curl -s http://localhost:8025/api/v2/messages | head -c 300
```

---

## 1. Tests automáticos (sin Docker)

```bash
cd Backend

(cd msvc-notificaciones && mvn test)   # 31 tests: fan-out, idempotencia, envío, API/contrato, SMTP
(cd msvc-eventos        && mvn test)   # 193 tests: publicación/modificación/cancelación
(cd msvc-ventas         && mvn test)   # 161 tests: compra/pago + evento compra.confirmada
(cd msvc-preferencias   && mvn test)   # 12 tests: preferencias + matching
(cd APIGateway          && mvn test)   # 44 tests: autorización de /api/notificaciones
```

Cobertura específica de esta feature:

| Test | Qué fija |
|---|---|
| `NotificacionServiceImplTest` | cancelación, suspensión, modificación, artista favorito, compra, preferencias de canal, idempotencia, **dual PUSH + EMAIL** |
| `NotificacionEnvioServiceTest` | `PENDIENTE → ENVIADA/FALLIDA`, reintentos, máximo, canal sin proveedor |
| `SmtpEmailProviderTest` | envío SMTP al email resuelto y fallback al destinatario por defecto |
| `NotificacionController` (vía API e2e) | `X-User-Id`, dueño, paginación, `noLeidas`, marcar leída |
| `EventChangePublisherTest` | routing keys `evento.cancelado/suspendido/modificado/publicado` |
| `CompraEventPublisherTest` | routing key `compra.confirmada` y payload |
| `PreferenciaServiceImplTest` | defaults, alta/edición y matching por ids |
| `EventoCambioContratoTest` | wire con `ciudad` y `artistas` para `EventoPublicado` |

---

## 2. Preparar tokens

```bash
GATEWAY=${GATEWAY:-http://localhost:8080}

login() {
  local resp body code
  resp=$(curl -s -w '\n%{http_code}' -X POST "$GATEWAY/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$2\"}")
  body=${resp%$'\n'*}
  code=${resp##*$'\n'}

  if [ "$code" != "200" ]; then
    echo "Login falló para $1 (HTTP $code): $body" >&2
    return 1
  fi

  printf '%s' "$body" | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])'
}

TOKEN_CLI=$(login demo@ticketya.com demo1234) || exit 1  # CLIENTE (dueño de las preferencias seed)
TOKEN_ORG=$(login org@ticketya.com org1234)     || exit 1  # ORGANIZADOR
TOKEN_ADM=$(login admin@ticketya.com admin1234) || exit 1  # ADMIN
```

### Canal EMAIL: MailHog vs mock

En Docker Compose, `msvc-notificaciones` usa `TICKETYA_NOTIF_EMAIL_PROVIDER=smtp` y
`SPRING_MAIL_HOST=mailhog`, por lo que el correo sale de verdad hacia MailHog.
Sin Docker (tests locales o ejecución directa) el default es `mock` y solo se loguea.

Los **avisos de evento** se generan por ambos canales si existen las plantillas `PUSH` y `EMAIL`:
vas a ver dos notificaciones por usuario y dos correos en MailHog. La compra confirmada usa solo `EMAIL`.

Si querés forzar el mock dentro de Docker:

```bash
TICKETYA_NOTIF_EMAIL_PROVIDER=mock docker compose up -d ms-notificaciones
```

IDs seed útiles:

| Variable | Valor |
|---|---|
| Evento Metallica | `d0000000-0000-4000-8000-000000000001` |
| Lugar Monumental (Buenos Aires) | `c0000000-0000-4000-8000-000000000001` |
| Sector Campo | `c0000000-0000-4000-8000-000000000011` |
| Participante Metallica | `p0000000-0000-4000-8000-000000000001` |

El cliente demo tiene en Mongo `artistas = [Metallica, Iron Maiden, Gojira]` y
`ciudades = [Buenos Aires]`.

### Preferencias (msvc-preferencias / Mongo)

Requiere `ms-preferencias` arriba. Si cambiaste código, rebuild:

```bash
docker compose up -d --build ms-preferencias ms-notificaciones
```

Leer las preferencias propias:

```bash
curl -s "$GATEWAY/api/preferencias/me" \
  -H "Authorization: Bearer $TOKEN_CLI" | python3 -m json.tool
```

Actualizar artistas/ciudades y canales:

```bash
curl -s -X PUT "$GATEWAY/api/preferencias/me" \
  -H "Authorization: Bearer $TOKEN_CLI" \
  -H 'Content-Type: application/json' \
  -d '{
        "ciudades":["Buenos Aires"],
        "artistas":["Metallica","Gojira"],
        "notificaciones":{"push":true,"email":true,"tipos":[]}
      }' | python3 -m json.tool
```

Actualizar solo canales (PUT parcial):

```bash
curl -s -X PUT "$GATEWAY/api/preferencias/me/notificaciones" \
  -H "Authorization: Bearer $TOKEN_CLI" \
  -H 'Content-Type: application/json' \
  -d '{"push":true,"email":false}' | python3 -m json.tool
```

Esperado: `email=false`, `push` conserva su valor anterior. `tipos: []` significa “todos”.

| Preferencia | Efecto al **crear** avisos |
|---|---|
| `push=false` | Los eventos no generan notificación PUSH; sí EMAIL si `email=true` |
| `email=false` | Los eventos no generan EMAIL; la compra confirmada (solo EMAIL) no genera notificación |
| `tipos=["EventoCancelado"]` | Solo se crean avisos de cancelación; se ignoran suspensión, modificación y favorito |
| `tipos=[]` | Todos los tipos |

Ojo: las preferencias se evalúan **al crear** la notificación. Si ya está `PENDIENTE`, el job la envía
aunque después desactives el canal.

Probar el matching interno:

```bash
curl -s -X POST http://localhost:8085/preferencias/clientes/matching \
  -H 'Content-Type: application/json' \
  -d '{"ciudad":"Buenos Aires","artistas":["Metallica"]}' | python3 -m json.tool
```

Esperado: el id del cliente demo (`b0000000-0000-4000-8000-000000000001`).

Probar que el matching afecta al aviso:
1. Poné `ciudades=["Córdoba"]` o `artistas=["Iron Maiden"]`.
2. Publicá un evento **nuevo** de Metallica en Buenos Aires (usá el flujo del Escenario C).
3. No debe generarse `NuevoEventoArtistaFavorito`.
4. Restaurá `Buenos Aires` + `Metallica`, publicá otro evento nuevo y sí debe generarse.

Verificar el documento en Mongo:

```bash
docker compose exec -T db-mongo-preferencias mongosh \
  -u ticketya -p mongo_dev --authenticationDatabase admin preferencias --quiet \
  --eval 'db.preferencias_clientes.findOne(
    {idCliente:"b0000000-0000-4000-8000-000000000001"},
    {idCliente:1,artistas:1,ciudades:1,notificaciones:1})'
```

Sin `X-User-Id`, el servicio directo responde `401`:

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8085/preferencias/me
```

Para un cliente sin documento, `GET /preferencias/me` devuelve defaults:
`push=true`, `email=true`, `tipos=[]`, listas vacías.

---

## 3. Escenario A — Compra confirmada → Email

Con `TOKEN_CLI`:

```bash
# 3.1 Reservar 2 entradas del evento Metallica
RESERVA=$(curl -s -X POST "$GATEWAY/api/reservas" \
  -H "Authorization: Bearer $TOKEN_CLI" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $(python3 -c 'import uuid;print(uuid.uuid4())')" \
  -d '{"idEvento":"d0000000-0000-4000-8000-000000000001","items":[{"idSector":"c0000000-0000-4000-8000-000000000011","cantidad":2}]}')
RESERVA_ID=$(echo "$RESERVA" | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')

# 3.2 Confirmar reserva
curl -s -X POST "$GATEWAY/api/reservas/$RESERVA_ID/confirmacion" \
  -H "Authorization: Bearer $TOKEN_CLI"

# 3.3 Crear compra
COMPRA=$(curl -s -X POST "$GATEWAY/api/compras" \
  -H "Authorization: Bearer $TOKEN_CLI" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $(python3 -c 'import uuid;print(uuid.uuid4())')" \
  -d "{\"idReserva\":\"$RESERVA_ID\"}")
COMPRA_ID=$(echo "$COMPRA" | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')

# 3.4 Pagar (APROBAR) -> publica compra.confirmada
curl -s -X POST "$GATEWAY/api/compras/$COMPRA_ID/pagos" \
  -H "Authorization: Bearer $TOKEN_CLI" \
  -H 'Content-Type: application/json' \
  -d '{"resultado":"APROBAR"}'
```

Esperar hasta 30 s (job de envío; se puede bajar con
`TICKETYA_NOTIF_ENVIO_INTERVALO_MS=5000` en `msvc-notificaciones/docker-compose.yml`) y consultar:

```bash
curl -s "$GATEWAY/api/notificaciones/me?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN_CLI" | python3 -m json.tool
```

Esperado: una notificación `tipo=CompraConfirmada`, `canal=EMAIL`, `estado=ENVIADA`,
`fechaEnvio` no nula y `noLeidas` incrementado. En **MailHog** (<http://localhost:8025>) debe
aparecer un correo con destino `demo@ticketya.com`, asunto “Tu compra en TicketYa” y el detalle
de la compra. En la DB, `notificacion.destinatario` debe tener ese email.

Marcar leída:

```bash
NOTIF_ID=$(curl -s "$GATEWAY/api/notificaciones/me?page=0&size=1" \
  -H "Authorization: Bearer $TOKEN_CLI" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["content"][0]["id"])')

curl -s -X PATCH "$GATEWAY/api/notificaciones/me/$NOTIF_ID/leida" \
  -H "Authorization: Bearer $TOKEN_CLI" | python3 -m json.tool
```

Esperado: `leida=true`, `fechaLectura` no nula y `noLeidas` decrementado.

---

## 4. Escenario B — Cambio y cancelación de evento

Usar el mismo evento (el cliente demo ya tiene una compra APROBADA). Podés usar `TOKEN_ORG`
(dueño del evento) o `TOKEN_ADM`.

```bash
# 4.1 Modificación de fecha -> EventoModificado
curl -s -X PATCH "$GATEWAY/api/eventos/d0000000-0000-4000-8000-000000000001/fecha" \
  -H "Authorization: Bearer $TOKEN_ORG" \
  -H 'Content-Type: application/json' \
  -d '{"fechaInicio":"2027-04-01T21:00:00","fechaFin":"2027-04-02T01:00:00"}'

# 4.2 Suspensión -> EventoSuspendido
curl -s -X PATCH "$GATEWAY/api/eventos/d0000000-0000-4000-8000-000000000001/estado" \
  -H "Authorization: Bearer $TOKEN_ADM" \
  -H 'Content-Type: application/json' \
  -d '{"estado":"SUSPENDIDO"}'

# 4.3 Cancelación -> EventoCancelado
curl -s -X PATCH "$GATEWAY/api/eventos/d0000000-0000-4000-8000-000000000001/estado" \
  -H "Authorization: Bearer $TOKEN_ADM" \
  -H 'Content-Type: application/json' \
  -d '{"estado":"CANCELADO"}'
```

Después de cada paso (o esperando el job) consultar `GET /api/notificaciones/me` con `TOKEN_CLI`.
Esperado: `EventoModificado`, `EventoSuspendido` y `EventoCancelado`, cada uno **dos veces**:
una notificación `PUSH` y una `EMAIL`, ambas `ENVIADA`. Los emails deben verse en MailHog.
La idempotencia es por canal: `EventoCancelado:PUSH:<eventoId>` y `EventoCancelado:EMAIL:<eventoId>`.

> Orden importante: modificar → suspender → cancelar. Un evento cancelado ya no se puede editar.

Verificar idempotencia: volver a ejecutar el `PATCH` de cancelación no debe crear otra notificación
(el estado ya es `CANCELADO` y además la clave de idempotencia es
`EventoCancelado:PUSH:<eventoId>` / `EventoCancelado:EMAIL:<eventoId>`).

---

## 5. Escenario C — Artista favorito publica en la ciudad

Crear un evento nuevo en Buenos Aires con Metallica, publicarlo y verificar que el cliente demo
recibe el aviso (usa las preferencias seed: Metallica + Buenos Aires).

```bash
# 5.1 Crear borrador
EVENTO=$(curl -s -X POST "$GATEWAY/api/eventos" \
  -H "Authorization: Bearer $TOKEN_ORG" \
  -H 'Content-Type: application/json' \
  -d '{
        "nombre":"Metallica Test Notificaciones",
        "tipo":"RECITAL",
        "fechaInicio":"2027-12-15T21:00:00",
        "estado":"BORRADOR",
        "idLugar":"c0000000-0000-4000-8000-000000000001",
        "sectores":[{"id":"c0000000-0000-4000-8000-000000000011","precio":75000,"capacidadTotal":50}]
      }')
EVENTO_ID=$(echo "$EVENTO" | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')

# 5.2 Asociar Metallica
curl -s -X PUT "$GATEWAY/api/eventos/$EVENTO_ID/participantes" \
  -H "Authorization: Bearer $TOKEN_ORG" \
  -H 'Content-Type: application/json' \
  -d '{"ids":["p0000000-0000-4000-8000-000000000001"]}'

# 5.3 Publicar -> evento.publicado (ciudad + artistas)
curl -s -X POST "$GATEWAY/api/eventos/$EVENTO_ID/publicacion" \
  -H "Authorization: Bearer $TOKEN_ORG"
```

Esperar el job y consultar con `TOKEN_CLI`:

```bash
curl -s "$GATEWAY/api/notificaciones/me?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN_CLI" | python3 -m json.tool
```

Esperado: `tipo=NuevoEventoArtistaFavorito` **dos veces** (`PUSH` y `EMAIL`), `ENVIADA`, con el
nombre del evento y “Buenos Aires” en el contenido. El email aparece en MailHog.

---

## 6. Prueba aislada de consumidores (sin pasar por las APIs)

Útil para probar idempotencia y DLQ rápido. Publicar directo en Rabbit:

```bash
PAYLOAD='{"messageId":"test-1","compraId":"test-compra-1","clienteId":"b0000000-0000-4000-8000-000000000001","eventoId":"d0000000-0000-4000-8000-000000000001","total":1500.00,"moneda":"ARS","cantidadEntradas":2,"occurredAt":"2026-11-16T21:24:00Z"}'

BODY=$(python3 - "$PAYLOAD" <<'PY'
import json,sys
print(json.dumps({"properties":{},"routing_key":"compra.confirmada","payload":sys.argv[1],"payload_encoding":"string"}))
PY
)

curl -s -u ticketya:ticketya_dev \
  -H 'Content-Type: application/json' \
  -X POST 'http://localhost:15672/api/exchanges/%2F/eventos.exchange/publish' \
  -d "$BODY"
```

Para `evento.publicado` cambiá `routing_key` y el payload:

```json
{
  "messageId": "test-2",
  "eventoId": "d0000000-0000-4000-8000-000000000001",
  "tipo": "EventoPublicado",
  "nombre": "Metallica — M72 World Tour",
  "fecha": "2027-03-15T21:00:00",
  "cambios": null,
  "occurredAt": "2026-11-16T21:00:00Z",
  "ciudad": "Buenos Aires",
  "artistas": ["Metallica"]
}
```

Repetir el mismo `compraId` no debe duplicar la notificación.

---

## 7. Front (campanita y bandeja)

Con el stack levantado, abrí <http://localhost:3000> y logueate con
`demo@ticketya.com` / `demo1234`.

1. En el header debe aparecer la campanita con el badge de no leídas.
2. Al abrirla, muestra las últimas 6 notificaciones y el link “Ver todas”.
3. `/notificaciones` muestra la bandeja completa, filtros **Todas / No leídas**,
   “Marcar leídas” y el link “Ver evento”.
4. El panel de **Canales** permite activar/desactivar Push y Email; al cambiar
   uno se guarda contra `PUT /api/preferencias/me/notificaciones`.
5. El badge se actualiza cada 45 s y al marcar una notificación como leída.

Si cambiás código del front, rebuild:

```bash
cd Backend
docker compose build ms-frontend
docker compose up -d --no-deps --force-recreate ms-frontend
```

## 8. Verificación en base, logs y colas

```bash
# Notificaciones e intentos
docker compose exec -T db-mysql-notificaciones mysql -uticketya -pticketya_dev notificaciones -e "
  SELECT tipo, canal, estado, leida, destinatario, clave_idempotencia, fecha_envio
  FROM notificacion ORDER BY fecha_creacion DESC LIMIT 20;
  SELECT numero_intento, resultado, detalle_error
  FROM intento_envio ORDER BY fecha_intento DESC LIMIT 20;"

# Logs del consumidor y del job
docker compose logs -f ms-notificaciones | grep -E "PUSH-MOCK|EMAIL-MOCK|notificación|Evento publicado|Compra confirmada"

# Colas
curl -s -u ticketya:ticketya_dev http://localhost:15672/api/queues/%2F/notificaciones.eventos | python3 -m json.tool
curl -s -u ticketya:ticketya_dev http://localhost:15672/api/queues/%2F/notificaciones.compras | python3 -m json.tool
```

También podés mirar `notificaciones.eventos.dlq` y `notificaciones.compras.dlq` en el panel de Rabbit.

---

## 9. Migración en volúmenes existentes

Si **no** hiciste `down -v`, el esquema viejo no tiene `leida`, `fecha_lectura` ni
`clave_idempotencia`. Aplicar una vez desde `Backend/`:

```bash
docker compose exec -T db-mysql-notificaciones \
  mysql -uroot -proot_dev notificaciones \
  < infrastructure/mysql/notificaciones/migrations/02-leida-y-plantillas.sql

docker compose exec -T db-mysql-notificaciones \
  mysql -uroot -proot_dev notificaciones \
  < infrastructure/mysql/notificaciones/migrations/03-email-destinatario.sql

docker compose exec -T db-mysql-notificaciones \
  mysql -uroot -proot_dev notificaciones \
  < infrastructure/mysql/notificaciones/migrations/04-plantillas-email-eventos.sql
```

Es idempotente: se puede correr varias veces.

Para MongoDB, si el volumen ya existía, el documento seed no tendrá `notificaciones`, pero el
servicio devuelve defaults (`push=true`, `email=true`, `tipos=[]`); el matching de artistas/ciudad
sigue funcionando porque `artistas` y `ciudades` ya estaban en el seed.

---

## 10. Checklist de aceptación

- [ ] `CompraConfirmada` aparece como `EMAIL` y pasa a `ENVIADA` con `fecha_envio`.
- [ ] El correo se ve en MailHog (`localhost:8025`) con destino `demo@ticketya.com`.
- [ ] `EventoCancelado` aparece como `PUSH` y `EMAIL` para el cliente con compra aprobada.
- [ ] Los emails de cancelación/suspensión/modificación se ven en MailHog.
- [ ] `EventoSuspendido` y `EventoModificado` aparecen para el mismo cliente.
- [ ] `NuevoEventoArtistaFavorito` aparece como `PUSH` y `EMAIL` para el cliente demo al publicar
      Metallica en Buenos Aires.
- [ ] `GET /api/notificaciones/me` devuelve `noLeidas` y `PATCH .../leida` lo baja.
- [ ] `intento_envio` tiene filas `OK` por cada envío.
- [ ] Reenviar el mismo mensaje de Rabbit no duplica la notificación.
- [ ] Sin `Authorization` la API responde `401`; un cliente no puede leer notificaciones ajenas (`404`).
