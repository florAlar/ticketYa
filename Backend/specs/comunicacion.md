# Comunicación

> **Estado:** `hecho` · **Fecha:** 2026-11-22 · **Alcance:** cliente → gateway, gateway → msvcs,
> Feign, RabbitMQ, headers de correlación e idempotencia · **No incluye:** contrato detallado por
> endpoint (ver `endpoints.md` y `docs/openapi/`).

## Cliente → backend

- Protocolo HTTP + JSON.
- Único punto de entrada externo: **API Gateway** (`http://localhost:8080` en local; prefijo de producto `https://ticketya.com.ar/api/`).
- El frontend (`Frontend/`) habla **solo** con el gateway (`VITE_API_BASE_URL`). No llama puertos de msvc (8081–8086).

## Gateway → microservicios

- Comunicación principal: **REST síncrono**.
- Enrutado por prefijo (detalle en `endpoints.md`):

| Prefijo (tras gateway) | Servicio |
| --- | --- |
| `/auth/**`, `/usuarios/**` | msvc-usuarios |
| `/eventos/**` (catálogo/CRUD), `/lugares/**`, `/sectores/**`, `/participantes/**`, `/organizadores/**` | msvc-eventos |
| `/reservas/**`, `/compras/**`, `/pagos/**`, `/reportes/**` | msvc-ventas |
| `/entradas/**`, `/devoluciones/**` | msvc-entradas |
| `/preferencias/**`, `/recomendaciones/**`, `/clientes/**` | msvc-preferencias |
| `/notificaciones/**` | msvc-notificaciones |

### Solapes de `/eventos/**` (rutear antes del catch-all a eventos)

| Ruta | Servicio |
| --- | --- |
| `/eventos/{id}/disponibilidad`, `/eventos/{id}/fila/**` | msvc-ventas |
| `/eventos/{idEvento}/validaciones` | msvc-entradas |
| `/eventos/{id}/recomendaciones` | msvc-preferencias |

### Síncrono entre microservicios (Feign)

Además del gateway, los servicios se llaman por **REST síncrono** con OpenFeign. Todas las llamadas
propagan `X-Correlation-Id`; las rutas internas (`/internas/**`, `/fila/**`) exigen
`X-Internal-Token` (`ticketya.interno.token`), que agrega el cliente Feign y sella el gateway.

| Origen → Destino | Endpoint | Cuándo / para qué | Ante fallo |
| --- | --- | --- | --- |
| eventos → ventas | `POST /eventos/{id}/cupos` | publicar/alta publicada: abrir stock por sector | **fail-hard**: rollback, no se publica |
| eventos → ventas | `POST /internas/fila/{idEvento}/invalidacion` | cambio de estado o flag de alta demanda: pausar/reanudar fila | best-effort (log) |
| eventos → entradas | `POST /eventos/{id}/reintegro-cancelacion` | cancelar evento: marcar entradas `A_REINTEGRAR` | best-effort |
| ventas → eventos | `GET /eventos/{id}` | reserva: precio por sector y `esAltaDemanda` | **503** (reintentable) |
| ventas → entradas | `POST /entradas/emision` | pago aprobado: emitir entradas/QR | claim idempotente; reintentos + job; `503` si otra emisión está en curso |
| ventas → entradas | `GET /eventos/{id}/entradas/resumen` | reportes: devueltas/usadas/a reintegrar | **503** sin reporte parcial |
| entradas → ventas | `POST /eventos/{id}/cupos/liberacion` | aprobar devolución: liberar 1 cupo | **fail-hard**: rollback y no publica |
| entradas → eventos | `GET /eventos/{id}` | enriquecer entradas y validar QR | best-effort (campos `null`) / estado del evento |
| preferencias → eventos | `GET /eventos`, `GET /eventos/{id}` | matching, historial y recomendaciones | best-effort |
| preferencias → ventas | `GET /internas/compras` | backfill de compras al historial | best-effort |
| notificaciones → ventas | `GET /compras/evento/{id}` | fan-out a compradores de un evento | sin destinatarios, no genera avisos |
| notificaciones → preferencias | `POST /preferencias/clientes/matching`, `GET /preferencias/clientes/{id}/notificaciones` | elegir destinatarios y canales | fallback permisivo (envía igual) |
| notificaciones → usuarios | `GET /usuarios/{id}` | resolver el email del destinatario | usa `destino-default` |

### Fila virtual → reserva (FIL-04)

El flujo de acceso a un evento de alta demanda es **REST síncrono** y no introduce mensajes nuevos:

1. `POST /eventos/{id}/fila/ingreso` → turno en Redis.
2. `POST /eventos/{id}/fila/validacion` → `token` con TTL de 10 min (config `ticketya.fila.token-ttl-minutes`).
3. `POST /reservas` con `tokenFila`: ventas consume el token **antes** de `lockCupo`. Si el token falta o no sirve → 409 y no se descuenta cupo; si Redis falla → 503. El consumo es compare-and-delete atómico, así que un token solo habilita una reserva.
4. Si la transacción de la reserva hace rollback, un `TransactionSynchronization` devuelve el token (`restaurarToken`). Ninguno de estos pasos publica eventos Rabbit: la bitácora local (`Bitacora.registrar`) cubre la observabilidad.

## Async (broker)

Broker: **RabbitMQ** (servicio `rabbitmq`, panel en `:15672`). Exchange compartido: `eventos.exchange`
(topic, durable=true, autoDelete=false), declarado por `msvc-eventos`, `msvc-ventas`,
`msvc-entradas`, `msvc-preferencias` y `msvc-notificaciones` con los mismos atributos.

### Colas consumidoras de msvc-notificaciones

| Cola | Binding | Routing keys | DLQ |
|---|---|---|---|
| `notificaciones.eventos` | `evento.#` | `evento.cancelado`, `evento.suspendido`, `evento.modificado`, `evento.publicado` | `notificaciones.eventos.dlq` |
| `notificaciones.compras` | `compra.#` | `compra.confirmada` | `notificaciones.compras.dlq` |
| `notificaciones.devoluciones` | `devolucion.#` | `devolucion.aprobada` | `notificaciones.devoluciones.dlq` |

### Cola consumidora de msvc-preferencias

| Cola | Binding | Routing keys | DLQ |
|---|---|---|---|
| `preferencias.compras` | `compra.confirmada` | `compra.confirmada` | `preferencias.compras.dlq` (DLX `preferencias.dlx`) |

Semántica **at-least-once**; `messageId` identifica la ocurrencia y sirve para deduplicar
reentregas. La deduplicación es por (`idUsuario`, `clave_idempotencia`), donde la clave es
`tipo:canal:eventoId:messageId` para eventos (el mismo mensaje no se duplica al reintentar, pero dos
modificaciones distintas del mismo evento sí generan avisos separados),
`CompraConfirmada:EMAIL:compraId` para compras (permite dos compras del mismo evento) y
`DevolucionAprobada:<canal>:<devolucionId>` para devoluciones (una devolución se aprueba una sola vez).

Las filas `PUSH` y `EMAIL` de un mismo mensaje comparten `clave_mensaje` (`tipo:eventoId:messageId`,
`CompraConfirmada:compraId` o `DevolucionAprobada:devolucionId`). La bandeja
`GET /notificaciones/me` muestra **una sola entrada por mensaje** (prefiere la fila PUSH y expone
`canales` con todos los canales usados); el envío sigue siendo dual.

### Contrato `evento.#` (msvc-eventos)

Payload (JSON):

```json
{
  "messageId": "uuid",
  "eventoId": "uuid",
  "tipo": "EventoCancelado",
  "nombre": "Rock in Rio",
  "fecha": "2026-12-20T21:00:00",
  "cambios": null,
  "occurredAt": "2026-11-13T19:20:00Z",
  "ciudad": "Buenos Aires",
  "artistas": ["Metallica"]
}
```

| `tipo` | Routing key | Cuándo | Destinatarios |
|---|---|---|---|
| `EventoCancelado` | `evento.cancelado` | `PATCH /eventos/{id}/estado` → `CANCELADO` | compradores con compra APROBADA (PUSH + EMAIL) |
| `EventoSuspendido` | `evento.suspendido` | `PATCH .../estado` → `SUSPENDIDO` | compradores con compra APROBADA (PUSH + EMAIL) |
| `EventoModificado` | `evento.modificado` | `PUT /eventos/{id}`, `PATCH .../fecha` con cambios reales | compradores con compra APROBADA (PUSH + EMAIL) |
| `EventoPublicado` | `evento.publicado` | `POST /eventos/{id}/publicacion` o alta con `estado=PUBLICADO` | clientes con artista favorito + ciudad (PUSH + EMAIL) |

El envío es **dual** para eventos: se crea una entrega por cada canal que tenga plantilla
activa (`PUSH` y/o `EMAIL`), pero ambas comparten `clave_mensaje` y la bandeja las muestra como
una sola notificación. Las plantillas EMAIL de eventos están en
`infrastructure/mysql/notificaciones/init.sql` (y en la migración manual
`04-plantillas-email-eventos.sql` para volúmenes ya creados).

`ciudad` y `artistas` solo se completan en `EventoPublicado`. `fecha` es `fechaInicio` (ISO-8601 sin
zona); `occurredAt` es UTC con `Z` y puede traer nanosegundos.

### Contrato `compra.confirmada` (msvc-ventas)

Se publica después del commit (`@TransactionalEventListener(AFTER_COMMIT)`) al aprobarse el pago.

```json
{
  "messageId": "uuid",
  "compraId": "uuid",
  "clienteId": "uuid",
  "eventoId": "uuid",
  "total": 1500.00,
  "moneda": "ARS",
  "cantidadEntradas": 2,
  "occurredAt": "2026-11-13T19:20:00Z"
}
```

### Contrato `devolucion.aprobada` (msvc-entradas)

Se publica después del commit (`@TransactionalEventListener(AFTER_COMMIT)`) al aprobarse una
devolución (`POST /devoluciones/{id}/aprobacion`). Si falla la liberación de cupo en ventas, la
aprobación hace rollback y **no** se publica.

```json
{
  "messageId": "uuid",
  "devolucionId": "uuid",
  "entradaId": "uuid",
  "clienteId": "uuid",
  "eventoId": "uuid",
  "monto": 95000.00,
  "moneda": "ARS",
  "occurredAt": "2026-11-17T12:00:00Z"
}
```

### Fan-out y matching

```
msvc-eventos --evento.*--> notificaciones.eventos --+
                                                    +--> GET /compras/evento/{id} (ventas)
                                                    +--> una notificación PUSH y una EMAIL por
                                                         comprador (si ambas plantillas están activas)

msvc-eventos --evento.publicado--> notificaciones.eventos --+
                                                            +--> POST /preferencias/clientes/matching
                                                            +--> PUSH + EMAIL por cliente que sigue
                                                                 un artista y vive en la ciudad

msvc-ventas --compra.confirmada--> notificaciones.compras --> una notificación EMAIL por compra

msvc-entradas --devolucion.aprobada--> notificaciones.devoluciones --> PUSH + EMAIL al cliente dueño
                                                                       de la entrada devuelta
```

Antes de crear cada aviso, notificaciones consulta `GET /preferencias/clientes/{id}/notificaciones`
(canales y tipos habilitados). Si preferencias no responde, se envía igual (fallback permisivo).

### Entrega

Un job `@Scheduled` (30 s por defecto) toma hasta 50 notificaciones `PENDIENTE` con lock
`SKIP LOCKED`, registra `intento_envio` y las pasa a `ENVIADA` o, tras 3 fallos, `FALLIDA`.

- `PUSH`: adaptador mock (sin Firebase), deja evidencia en logs.
- `EMAIL`: **SMTP real** con `spring-boot-starter-mail`. En local/Docker apunta a **MailHog**
  (`mailhog:1025`, bandeja web en <http://localhost:8025>). Se activa con
  `ticketya.notificaciones.email.provider=smtp`; con `mock` (default sin Docker) solo loguea.
- El email de destino se resuelve al crear la notificación con `GET /usuarios/{id}` (Feign a
  msvc-usuarios) y se guarda en `notificacion.destinatario`. Si no se puede resolver, el proveedor
  SMTP usa `ticketya.notificaciones.email.destino-default`.


### Resto de efectos de dominio

- Fila virtual / tokens temporales: apoyo Redis en msvc-ventas (E2).


## Headers de correlación / idempotencia

| Header | Uso |
| --- | --- |
| `Authorization: Bearer …` | JWT obligatorio en el gateway; la identidad sale del token verificado |
| `X-User-Id`, `X-User-Role`, `X-User-Roles` | Los inyecta el gateway desde el JWT; si el cliente los manda, se descartan |
| `X-Internal-Token` | Sello para rutas internas (`/internas/**`, `/fila/**`); lo agrega el gateway/Feign y se descarta el del cliente |
| `Content-Type: application/json` | Cuerpos JSON |
| `Idempotency-Key` | Reservas (opcional) y compras (obligatoria); evita duplicados por reintento |
| `X-Correlation-Id` | El gateway lo valida o genera, lo devuelve en la respuesta y se propaga a logs y llamadas Feign |

Contrato de rutas: `specs/endpoints.md`. Vista front: `Frontend/docs/endpoints-front.md`.
