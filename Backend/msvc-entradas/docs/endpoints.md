# Endpoints — msvc-entradas

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/docs/endpoints.md`

Paths internos (sin `/api`; GW StripPrefix=1).

---

## Estado E1 (2026-11-06)

| Endpoint | Estado | Notas |
| --- | --- | --- |
| POST /entradas/emision | **hecho** | Body `{idCompra,idCliente,idEvento,items[{idSector,cantidad,precioUnitario}]}`. Idempotente por idCompra. Llamado por msvc-ventas Feign post-pago. |
| GET /entradas | **hecho** | Propias (`X-User-Id`) |
| GET /entradas/{id} | **hecho** | Dueño |
| GET /entradas/{id}/qr | **hecho** | Dueño; no si DEVUELTA/A_REINTEGRAR |
| POST /entradas/{id}/devoluciones | **hecho** | 72h desde emisión; solo VENDIDA no usada |
| GET /devoluciones | **hecho** | Cliente: propias; ADMIN: pendientes |
| GET /devoluciones/{id} | **hecho** | |
| POST /devoluciones/{id}/aprobacion\|rechazo | **hecho** | ADMIN; aprueba → entrada DEVUELTA (reintegro mock) |
| POST /eventos/{id}/reintegro-cancelacion | **hecho** | E1-ENT-05 contrato; ADMIN; VENDIDA→A_REINTEGRAR |
| POST /eventos/{id}/validaciones | pendiente | E2-ACC |

### Estados entrada
`VENDIDA` | `USADA` | `DEVUELTA` | `A_REINTEGRAR`

### Emisión
QR: `QR-` + 16 hex. UK `codigo_qr`.

### Config
`ticketya.entradas.plazo-devolucion-horas` (72)

### Shape FE
```json
{ "id", "codigoQR", "idCompra", "idCliente", "idEvento", "idSector", "estado", "fechaEmision", "fechaIngreso", "precio" }
```
