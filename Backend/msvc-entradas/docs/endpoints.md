# Endpoints — msvc-entradas

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/docs/endpoints.md`

Paths públicos vía gateway: prefijo `/api`. Abajo se listan como en el global.

---

## entradas — emisión, QR, validación, devoluciones

Servicio: msvc-entradas | Prefijos: /entradas, /devoluciones + /eventos/{idEvento}/validaciones

### Entradas y validación

- GET /entradas — Listar entradas propias (CLIENTE)
- GET /entradas/{id} — Consultar entrada propia
- GET /entradas/{id}/qr — Obtener QR de la entrada
- POST /eventos/{idEvento}/validaciones — Validar QR en ingreso (PERSONAL_ACCESO; atómica)
- GET /eventos/{idEvento}/validaciones — Consultar validaciones del evento (ORGANIZADOR/ADMIN)

### Devoluciones

- POST /entradas/{id}/devoluciones — Solicitar devolución (CLIENTE; p.ej. plazo 72h, no usada)
- GET /devoluciones/{id} — Consultar solicitud propia
- GET /devoluciones — Listar solicitudes pendientes (ADMIN)
- POST /devoluciones/{id}/aprobacion — Aprobar devolución + reintegro (ADMIN)
- POST /devoluciones/{id}/rechazo — Rechazar devolución (ADMIN)

### Enrutamiento gateway (relacionado)

- /entradas/** → msvc-entradas
- /devoluciones/** → msvc-entradas
- /eventos/{idEvento}/validaciones → msvc-entradas (antes del catch-all de eventos)

### Notas del global

- Rol PERSONAL_ACCESO: valida entradas y registra ingresos.
- Cancelación de evento (PATCH estado en msvc-eventos) dispara reintegros/notifs que impactan este contexto.
