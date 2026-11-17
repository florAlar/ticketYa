# Endpoints — msvc-notificaciones

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/specs/endpoints.md`

Paths públicos vía gateway: prefijo `/api`. Abajo se listan como en el global.

---

## notificaciones — email/push y admin de envíos

Servicio: msvc-notificaciones | Prefijo: /notificaciones | disparos de negocio vía broker; REST para consulta/admin

- `GET /notificaciones/me` — Listar notificaciones propias. Query: `page` (0), `size` (20, máx 100),
  `leida` (opcional). Respuesta paginada + `noLeidas`.
- `GET /notificaciones/me/{id}` — Detalle (solo dueño).
- `PATCH /notificaciones/me/{id}/leida` — Marcar como leída (setea `fecha_lectura`).
- `POST /notificaciones` — Crear notificación administrativa (ADMIN). Queda `PENDIENTE`.
- `GET /notificaciones/{id}/estado` — Estado de envío (ADMIN).

Identidad: `X-User-Id` (obligatorio en `/me`); el gateway lo deriva del JWT y no confía en el header
del cliente. Las preferencias de canal viven en `msvc-preferencias` (`/preferencias/me/notificaciones`).

### Eventos broker consumidos

| Routing key | Tipo | Fan-out |
|---|---|---|
| `evento.cancelado` | `EventoCancelado` | compradores APROBADOS (PUSH + EMAIL) |
| `evento.suspendido` | `EventoSuspendido` | compradores APROBADOS (PUSH + EMAIL) |
| `evento.modificado` | `EventoModificado` | compradores APROBADOS (PUSH + EMAIL) |
| `evento.publicado` | `EventoPublicado` | artistas favoritos + ciudad (PUSH + EMAIL) |
| `compra.confirmada` | `CompraConfirmada` | cliente de la compra (EMAIL) |
| `devolucion.aprobada` | `DevolucionAprobada` | cliente dueño de la entrada devuelta (PUSH + EMAIL) |

Contrato completo en `Backend/specs/comunicacion.md`.

### Entrega

Job `@Scheduled` (30 s, configurable con `TICKETYA_NOTIF_ENVIO_INTERVALO_MS`): toma `PENDIENTE` con
lock `SKIP LOCKED`, registra `intento_envio` y pasa a `ENVIADA`/`FALLIDA` (3 intentos).
`PUSH` usa `MockPushProvider` (sin Firebase). `EMAIL` usa SMTP real: en Docker Compose apunta a
**MailHog** (`http://localhost:8025`) con `TICKETYA_NOTIF_EMAIL_PROVIDER=smtp`; sin esa variable usa
`MockEmailProvider`. Los avisos de evento y la devolución aprobada se generan por **ambos canales**
si hay plantilla activa para cada uno; la compra usa solo EMAIL. La bandeja `GET /notificaciones/me` devuelve **una sola
entrada por mensaje** (fila `PUSH` representativa + `canales: ["PUSH","EMAIL"]`) y `noLeidas` cuenta
mensajes, no entregas. El destinatario se resuelve con `GET /usuarios/{id}` y se
guarda en `notificacion.destinatario`; si falla, se usa `TICKETYA_NOTIF_EMAIL_DESTINO_DEFAULT`.

### Enrutamiento gateway (relacionado)

- /notificaciones/** → msvc-notificaciones (cualquier rol autenticado; el msvc valida dueño/ADMIN)
