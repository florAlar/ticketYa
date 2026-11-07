# Endpoints — msvc-entradas

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/docs/endpoints.md`

Paths internos (sin `/api`; GW StripPrefix=1).

---

## Estado E1 (2026-11-06)

| Endpoint | Estado | Notas |
| --- | --- | --- |
| POST /entradas/emision | **hecho** | Body `{idCompra,idCliente,idEvento,items[{idSector,cantidad,precioUnitario}]}`. Idempotente por idCompra. Llamado por msvc-ventas Feign post-pago. |
| GET /entradas | **hecho** | Propias (`X-User-Id`). Enriquece `eventoNombre`/`lugar*`/`sectorNombre` vía Feign msvc-eventos (best-effort). |
| GET /entradas/{id} | **hecho** | Dueño **o ADMIN** (mismo método). Cliente: solo propias; ADMIN: cualquier id. |
| GET /entradas/{id}/qr | **hecho** | Dueño; no si DEVUELTA/A_REINTEGRAR |
| POST /entradas/{id}/devoluciones | **hecho** | 72h desde emisión; solo VENDIDA no usada |
| GET /devoluciones | **hecho** | Cliente: propias; ADMIN: pendientes |
| GET /devoluciones/{id} | **hecho** | |
| POST /devoluciones/{id}/aprobacion\|rechazo | **hecho** | ADMIN; aprueba → entrada DEVUELTA (reintegro mock) |
| POST /eventos/{id}/reintegro-cancelacion | **hecho** | E1-ENT-05 contrato; ADMIN; VENDIDA→A_REINTEGRAR |
| POST /eventos/{id}/validaciones | pendiente | E2-ACC — entity `ValidacionAcceso` existe, sin service/controller |
| GET /eventos/{id}/validaciones | pendiente | E2 |

### Limitaciones conocidas (deuda natural E1 — documentada, no “olvidada”)

Estas deudas **existen a propósito** en el estado actual del monorepo (bounded contexts sin saga completa). No reabrir E1-ENT como `pendiente`: el contrato E1 se cumple; son matices de consistencia/calidad a cerrar cuando exista el diseño o la feature que las desbloquea.

| Deuda | Por qué es natural ahora | Cuándo cerrarla |
| --- | --- | --- |
| Emisión Feign ventas→entradas **best-effort** | Compra y QR viven en BCs distintos; sin outbox/saga | ARQ-05 / compensación post-pago |
| **Precio en RAM** (mapa proceso; 0 tras restart) | Schema `entrada` sin columna precio; no inventar DDL sin pedido | Persistir precio al emitir o copiar de ventas/eventos |
| Approve devolución **no reabre cupo** en ventas | Devolución termina en entradas; ventas no se entera | API/evento de dominio ventas + E1-ENT-04 follow-up |
| `ValidacionAcceso` sin endpoints | Entity lista; API = E2 | **E2-ACC-01** |

### Cerrado (higiene E1-ENT-03)
GET emisión/listado/detalle: Feign `GET /eventos/{id}` → `eventoNombre`, `lugarNombre`, `lugarCiudad`, `sectorNombre`.
- Cache por `idEvento` dentro del request (listado sin N× Feign al mismo evento).
- Si eventos cae o 404: campos **null** (GET entradas igual 200) — best-effort consciente, no silent bug.
- URI: `ticketya.uri.eventos` / `TICKETYA_URI_EVENTOS` (Docker: `http://ms-eventos:8082`).

### Estados entrada
`VENDIDA` | `USADA` | `DEVUELTA` | `A_REINTEGRAR`

### Emisión
QR: `QR-` + 16 hex. UK `codigo_qr`.

### Config
`ticketya.entradas.plazo-devolucion-horas` (72)  
`ticketya.uri.eventos` (Feign enriquecimiento)

### Shape FE
```json
{ "id", "codigoQR", "idCompra", "idCliente", "idEvento", "idSector", "estado", "fechaEmision", "fechaIngreso", "precio", "eventoNombre", "sectorNombre", "lugarNombre", "lugarCiudad" }
```
