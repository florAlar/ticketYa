# Endpoints — msvc-notificaciones

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/docs/endpoints.md`

Paths públicos vía gateway: prefijo `/api`. Abajo se listan como en el global.

---

## notificaciones — email/push y admin de envíos

Servicio: msvc-notificaciones | Prefijo: /notificaciones | disparos de negocio vía broker; REST para consulta/admin

- GET /notificaciones/me — Listar notificaciones propias
- GET /notificaciones/me/{id} — Detalle de notificación
- PATCH /notificaciones/me/{id}/leida — Marcar como leída
- GET /notificaciones/me/preferencias — Canales habilitados
- PUT /notificaciones/me/preferencias — Actualizar canales
- POST /notificaciones — Crear notificación administrativa (ADMIN)
- GET /notificaciones/{id}/estado — Estado de envío (ADMIN)

Eventos broker (no REST, ambos docs): CompraConfirmada; EventoCancelado; EventoModificado; NuevoEventoArtistaFavorito; DevolucionAprobada.

### Enrutamiento gateway (relacionado)

- /notificaciones/** → msvc-notificaciones
