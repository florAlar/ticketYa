# Endpoints — msvc-entradas

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/specs/endpoints.md`

Paths internos (sin `/api`; GW StripPrefix=1).

---

## Estado E1 (2026-11-06)

| Endpoint | Estado | Notas |
| --- | --- | --- |
| POST /entradas/emision | **hecho** | Body `{idCompra,idCliente,idEvento,items[{idSector,cantidad,precioUnitario,asientos?}]}`. Sector numerado: `asientos:[{fila,numero}]` (una entrada por asiento; `cantidad` debe coincidir, si no 400); las entradas devuelven `fila` y `asiento`. Idempotente por idCompra. Persiste `entrada.precio`, `fila`, `numero`. Llamado por msvc-ventas Feign post-pago. |
| GET /entradas | **hecho** | Propias (`X-User-Id`). Enriquece `eventoNombre`/`lugar*`/`sectorNombre` vía Feign msvc-eventos (best-effort). |
| GET /entradas/{id} | **hecho** | Dueño **o ADMIN** (mismo método). Cliente: solo propias; ADMIN: cualquier id. |
| GET /entradas/{id}/qr | **hecho** | Dueño; **solo VENDIDA** y evento no CANCELADO. USADA/DEVUELTA/A_REINTEGRAR → 409. |
| POST /entradas/{id}/devoluciones | **hecho** | 72h desde emisión; solo VENDIDA no usada |
| GET /devoluciones | **hecho** | Cliente: propias; ADMIN: pendientes |
| GET /devoluciones/{id} | **hecho** | |
| POST /devoluciones/{id}/aprobacion\|rechazo | **hecho** | ADMIN; aprueba → entrada DEVUELTA + **invalida codigo_qr** (INV-*) + Feign ventas `POST …/cupos/liberacion` fail-hard (1 unidad del sector) |
| POST /eventos/{id}/reintegro-cancelacion | **hecho** | E1-ENT-05; VENDIDA→A_REINTEGRAR + invalida QR + crea `devolucion` **EN_CURSO** (motivo cancelación; visible al cliente, no cola admin) |
| POST /eventos/{id}/validaciones | pendiente | E2-ACC — entity `ValidacionAcceso` existe, sin service/controller |
| GET /eventos/{id}/validaciones | pendiente | E2 |

### Limitaciones conocidas (deuda natural E1 — documentada, no “olvidada”)

Estas deudas **existen a propósito** en el estado actual del monorepo (bounded contexts sin saga completa). No reabrir E1-ENT como `pendiente`: el contrato E1 se cumple; son matices de consistencia/calidad a cerrar cuando exista el diseño o la feature que las desbloquea.

| Deuda | Por qué es natural ahora | Cuándo cerrarla |
| --- | --- | --- |
| Emisión Feign ventas→entradas | **Cerrado TP 2026-11-08** | Ventas: reintentos inmediatos + `compra.emision_estado` + job; idempotente por idCompra. Outbox opcional. |
| **~~Precio en RAM~~** | **Cerrado 2026-11-07** | Columna `entrada.precio`; emisión persiste; DTO/devo/reintegro leen DB. Volúmenes viejos: `migrations/02-add-entrada-precio.sql` |
| ~~Approve devolución no reabre cupo~~ | **Cerrado 2026-11-07** | Feign `POST /eventos/{id}/cupos/liberacion` fail-hard al approve |
| `ValidacionAcceso` sin endpoints | Entity lista; API = E2 | **E2-ACC-01** |

### Cerrado (higiene E1-ENT-03)
GET emisión/listado/detalle: Feign `GET /eventos/{id}` → `eventoNombre`, `lugarNombre`, `lugarCiudad`, `sectorNombre`.
- Cache por `idEvento` dentro del request (listado sin N× Feign al mismo evento).
- Si eventos cae o 404: campos **null** (GET entradas igual 200) — best-effort consciente, no silent bug.
- URI: `ticketya.uri.eventos` / `TICKETYA_URI_EVENTOS` (Docker: `http://ms-eventos:8082`).
- URI ventas (liberar cupo): `ticketya.uri.ventas` / `TICKETYA_URI_VENTAS` (Docker: `http://ms-ventas:8083`).

### Estados entrada
`VENDIDA` | `USADA` | `DEVUELTA` | `A_REINTEGRAR`

### Política QR (exposición)
- `codigoQR` en list/detalle **solo** si entrada `VENDIDA` y evento (Feign) **no** `CANCELADO`.
- Si no aplica → campo `null` (el secreto no viaja al cliente).
- `GET …/qr` misma regla; si no → conflicto de negocio.
- Al aprobar devolución o reintegro por cancelación: se **rota** `codigo_qr` a `INV-*` (pantallazo previo deja de coincidir con DB).
- Validación de puerta (aceptar/rechazar ingreso) = **E2-ACC** (sigue pendiente); esta política es exposición + invalidación de secreto.

### Emisión
QR: `QR-` + 16 hex. UK `codigo_qr`.

### Config
`ticketya.entradas.plazo-devolucion-horas` (72)  
`ticketya.uri.eventos` (Feign enriquecimiento)

### Shape FE
```json
{ "id", "codigoQR", "idCompra", "idCliente", "idEvento", "idSector", "estado", "fechaEmision", "fechaIngreso", "precio", "eventoNombre", "eventoEstado", "sectorNombre", "lugarNombre", "lugarCiudad" }
```
