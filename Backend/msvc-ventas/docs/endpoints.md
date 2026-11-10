# Endpoints — msvc-ventas

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/specs/endpoints.md`

Paths públicos vía gateway: prefijo `/api`. Abajo paths **internos** (sin `/api`; GW StripPrefix=1).

---

## Estado implementación E1 (2026-11-06)

| Endpoint | Estado | Notas |
| --- | --- | --- |
| GET /eventos/disponibilidad?ids= | **hecho** | Batch catálogo (máx 50 ids). 1 query `cupo_sector`. Sin filas → ítem agotado total 0 |
| GET /eventos/{id}/disponibilidad | **hecho** | Cupos `cupo_sector`; 404 si no hay filas. DTO: disponibleTotal + nivelCupo + por sector `asientosOcupados[{fila,numero,estado}]` (solo sectores numerados; el resto del layout está libre) |
| POST /eventos/{id}/cupos | **hecho** | Abre stock al publicar (body `{sectores:[{idSector,capacidadTotal≥1}]}`). Idempotente. Feign desde msvc-eventos; GW `/api/eventos/{id}/cupos` |
| POST /eventos/{id}/cupos/liberacion | **hecho** | Devuelve stock por devolución aprobada. Body `{idSector, cantidad≥1}` o, en sector numerado, `{idSector, asientos:[{fila,numero}]}` (deben figurar VENDIDO; `cantidad` se ignora). Lock pesimista; `-vendida` `+disponible`. Feign msvc-entradas; GW order 9 |
| POST /reservas | **hecho** | TTL 10 min; lock pesimista cupo; precio vía Feign msvc-eventos; `X-User-Id`; opcional `Idempotency-Key` |
| GET /reservas/{id} | **hecho** | Solo dueño; expira lazy si TTL vencido |
| POST /reservas/{id}/confirmacion | **hecho** | PENDIENTE → CONFIRMADA |
| POST /reservas/{id}/cancelacion | **hecho** | Libera cupo + Redis hold |
| POST /reservas/{id}/renovacion | **hecho** | Extiende TTL MySQL + Redis |
| POST /compras | **hecho** | Body `{idReserva}`; **Idempotency-Key obligatorio**; límite 6 entr/cliente/evento |
| GET /compras/{id} | **hecho** | Dueño |
| GET /compras | **hecho** | Listado propio |
| POST /compras/{id}/pagos | **hecho** | Mock: body `{ "resultado": "APROBAR"\|"RECHAZAR" }` (default APROBAR). Tras APROBAR: compra APROBADA + emisión QR con reintentos + estado `emisionEstado` |
| GET /compras/{id}/pagos | **hecho** | |
| POST /pagos/{id}/reintentos | **hecho** | Solo si pago RECHAZADO y se puede reabrir; tras rechazo con liberación de cupo suele exigir nueva reserva |
| Reportes / fila virtual | pendiente | E2 |

### Estados

- Reserva: `PENDIENTE` → `CONFIRMADA` → `COMPRADA` \| `CANCELADA` \| `EXPIRADA`
- Compra: `PENDIENTE_PAGO` → `APROBADA` \| `RECHAZADA`
- Pago: `APROBADO` \| `RECHAZADO`
- **Emisión QR** (campo `compra.emision_estado`, no confunde con estado de compra):
  - `NO_REQUIERE` — aún no hay pago aprobado
  - `PENDIENTE` — pago OK, QR aún no confirmado en entradas
  - `EMITIDA` — QR creados (Feign OK; idempotente por idCompra)
  - `FALLIDA` — superó máx. reintentos (job deja de insistir)

### Emisión post-pago (2026-11-08)

- Tras commit del pago: hasta N intentos inmediatos (`emision-intentos-inmediatos`, default 3).
- Si falla: queda `PENDIENTE`; job cada `emision-job-ms` (30s) reprocesa.
- Tope total `emision-max-intentos` (20) → `FALLIDA`.
- **No** revierte la compra ni el cupo vendido (política TP).
- Outbox formal: **no** implementado (opcional; ver `specs/reglas-de-negocio.md`).
- DTO compra: `emisionEstado`, `emisionIntentos`.
- Volúmenes viejos: `infrastructure/mysql/ventas/migrations/02-add-compra-emision.sql`.

### Anti doble venta

- `SELECT … FOR UPDATE` (JPA `PESSIMISTIC_WRITE`) sobre `cupo_sector`
- UK `(id_evento, id_sector)`
- Hold Redis `ventas:reserva:{id}` TTL (no sustituye MySQL)

### Config

- `ticketya.uri.eventos` / `TICKETYA_URI_EVENTOS` (Docker: `http://ms-eventos:8082`)
- `ticketya.uri.entradas` / `TICKETYA_URI_ENTRADAS` (Docker: `http://ms-entradas:8084`)
- `ticketya.ventas.reserva-ttl-minutes` (10)
- `ticketya.ventas.max-entradas-cliente-evento` (6)
- Job expire cada 30s
- `ticketya.ventas.emision-intentos-inmediatos` (3), `emision-delay-ms` (250), `emision-max-intentos` (20), `emision-job-ms` (30000)

### Shape breve

**Disponibilidad** (número real + clasificación DTO)
```json
{
  "idEvento": "…",
  "disponibleTotal": 42,
  "agotado": false,
  "nivelCupo": "MENOS_DE_50",
  "nivelCupoLabel": "Menos de 50",
  "sectores": [{
    "idSector": "…",
    "capacidadTotal": 100,
    "cantidadDisponible": 42,
    "cantidadReservada": 0,
    "cantidadVendida": 58,
    "agotado": false,
    "nivelCupo": "MENOS_DE_50",
    "nivelCupoLabel": "Menos de 50"
  }]
}
```
Umbrales: 0→AGOTADO; &lt;20→ULTIMOS_LUGARES; &lt;50→MENOS_DE_50; &lt;100→MENOS_DE_100; else OK.

**Batch** `GET /eventos/disponibilidad?ids=a&ids=b` → `{ "items": [ /* Disponibilidad… */ ] }`

**POST /reservas**
```json
{ "idEvento": "d000…001", "items": [{ "idSector": "c000…011", "cantidad": 1 }] }
```
Sector **numerado** (asientos virtuales; no se persisten asientos libres): se piden asientos concretos y `cantidad` se ignora (vale `asientos.length`).
```json
{ "idEvento": "d000…001", "items": [{ "idSector": "c000…012", "asientos": [{ "fila": "C", "numero": 14 }, { "fila": "C", "numero": 15 }] }] }
```
Errores: 400 si el sector numerado no trae asientos, un asiento no existe en el layout (`filas` x `asientosPorFila`), hay repetidos, o un sector general trae asientos; 409 si algún asiento ya está tomado (RESERVADO/VENDIDO).

**POST /compras** + header `Idempotency-Key: <uuid>`
```json
{ "idReserva": "…" }
```

**POST /compras/{id}/pagos**
```json
{ "resultado": "APROBAR", "proveedor": "MOCK_PASARELA" }
```

### Enrutamiento gateway

- `/api/eventos/disponibilidad` (GET batch) → ventas (order 9)
- `/api/eventos/{id}/disponibilidad` → ventas (order 10)
- `/api/reservas/**`, `/api/compras/**`, `/api/pagos/**` → ventas
- Headers: `Authorization` (GW) → `X-User-Id`; `Idempotency-Key`
