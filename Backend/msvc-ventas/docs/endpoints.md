# Endpoints — msvc-ventas

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/docs/endpoints.md`

Paths públicos vía gateway: prefijo `/api`. Abajo se listan como en el global.

---

## ventas — disponibilidad, reservas, compras, pagos, reportes, fila

Servicio: msvc-ventas | Prefijos: /reservas, /compras, /pagos, /reportes + /eventos/{id}/disponibilidad y /eventos/{id}/fila/**

### Disponibilidad y reservas

- GET /eventos/{id}/disponibilidad — Consultar disponibilidad comercial (cupos/asientos libres)

/EL RECURSO ES EVENTOS PERO LE PEGA A ESTE SERVICIO, HAY QUE RUTEARLO DESDE GATEWAY.

- POST /reservas — Crear reserva temporal (TTL p.ej. 10 min; atómica; cliente desde identidad gateway)
- GET /reservas/{id} — Consultar reserva propia
- POST /reservas/{id}/confirmacion — Confirmar intención de compra
- POST /reservas/{id}/cancelacion — Cancelar reserva y liberar cupo/asientos
- POST /reservas/{id}/renovacion — Extender reserva (no automática/ilimitada)

### Compras y pagos

- POST /compras — Crear compra desde reserva válida (Idempotency-Key)
- GET /compras/{id} — Consultar compra propia
- GET /compras — Listar compras propias
- POST /compras/{id}/pagos — Iniciar pago
- GET /compras/{id}/pagos — Consultar pagos de la compra
- POST /pagos/{id}/reintentos — Reintentar pago permitido
- POST /pagos/webhooks/{proveedor} — Webhook pasarela (firma; no confiar solo en browser)

### Reportes

- GET /reportes/eventos/{id}/ventas — Ventas por evento (ORGANIZADOR dueño)
- GET /reportes/eventos/{id}/sectores — Ventas/devoluciones por sector (ORGANIZADOR)
- GET /reportes/eventos/{id}/ocupacion — Porcentaje de ocupación (ORGANIZADOR)
- GET /reportes/eventos/{id}/recaudacion — Recaudación del evento (ORGANIZADOR)

### Fila virtual

- POST /eventos/{id}/fila/ingreso — Ingresar a fila virtual (posición asignada en servidor)
- GET /eventos/{id}/fila/posicion — Consultar posición/estado propio
- POST /eventos/{id}/fila/validacion — Validar token temporal de acceso a compra
- GET /eventos/{id}/fila/estado — Estado general de la venta/fila (público)
- POST /eventos/{id}/fila/habilitacion — Habilitar tanda de clientes (ADMIN/Sistema)

### Enrutamiento gateway (relacionado)

- /reservas/** → msvc-ventas
- /compras/** → msvc-ventas
- /pagos/** → msvc-ventas
- /reportes/** → msvc-ventas
- /eventos/{id}/disponibilidad | /eventos/{id}/fila/** → msvc-ventas (antes del catch-all de eventos)

### Headers relevantes (global)

- Idempotency-Key: <UUID> — compras, pagos y reservas
- Authorization: Bearer ***
- X-Correlation-Id: <ID>
