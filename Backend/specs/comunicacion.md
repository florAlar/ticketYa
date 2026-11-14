# Comunicación

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

## Async (broker)

Broker: **RabbitMQ** (servicio `rabbitmq`, panel en `:15672`). Implementado para avisos de cancelación.

### Contrato `EventoCancelado`

| Elemento | Valor |
|---|---|
| Exchange | `eventos.exchange` (topic, durable=true, autoDelete=false) — lo declara `msvc-eventos` |
| Routing key | `evento.cancelado` (`evento.suspendido` reservado) |
| Cola consumidora | `notificaciones.eventos` → binding `evento.#`, DLQ `notificaciones.eventos.dlq` vía `notificaciones.dlx` |
| Semántica | at-least-once; consumidor idempotente por (`eventoId`, `idUsuario`, `tipo`). `messageId` es trazabilidad (uuid por publicación), no clave de deduplicación |

Payload (JSON):

```json
{
  "messageId": "uuid",
  "eventoId": "uuid",
  "tipo": "EventoCancelado",
  "nombre": "Rock in Rio",
  "fecha": "2026-12-20T21:00:00",
  "cambios": null,
  "occurredAt": "2026-11-13T19:20:00Z"
}
```

`tipo` usa el vocabulario de `plantilla_notificacion.tipo_evento`. `fecha` es `fechaInicio` del evento.

Formatos: `fecha` es ISO-8601 **sin** zona (LocalDateTime del evento); `occurredAt` es ISO-8601 **UTC con `Z`** y puede traer nanosegundos (`2026-11-14T04:49:57.150570083Z`). Ambos son aceptados por `Instant`/`LocalDateTime` de Java sin configuración extra.

### Fan-out al cliente

`EventoCancelado` no lleva destinatarios: quien los conoce es `msvc-ventas`.

```
msvc-eventos --evento.cancelado--> msvc-notificaciones
                                          |
                                          +--> GET /compras/evento/{id} (interno, ventas)
                                          +--> una notificación PUSH por cliente con compra APROBADA
```

- Canal: **PUSH** (`plantilla_notificacion.tipo_evento = 'EventoCancelado'`).
- Los clientes con compra `PENDIENTE_PAGO` no reciben aviso de reintegro.

### Resto de efectos de dominio

- Fila virtual / tokens temporales: apoyo Redis en msvc-ventas (E2).
- `EventoModificado`, `CompraConfirmada`, `DevolucionAprobada`: pendientes.

## Headers de correlación / idempotencia

| Header | Uso |
| --- | --- |
| `Authorization: Bearer …` | JWT (E2); E1 puede usar id por header/param según diseño |
| `Content-Type: application/json` | Cuerpos JSON |
| `Idempotency-Key` | Compras/pagos/reservas (anti reintento duplicado) |
| `X-Correlation-Id` | Seguimiento entre servicios; el gateway puede generarlo |

Contrato de rutas: `specs/endpoints.md`. Vista front: `Frontend/docs/endpoints-front.md`.
