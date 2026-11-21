# Endpoints — msvc-ventas

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/specs/endpoints.md`
**Contexto del servicio:** [`README.md`](README.md) — alcance, agregados, modelo, eventos y operación.

Paths públicos vía gateway: prefijo `/api`. Abajo paths **internos** (sin `/api`; GW StripPrefix=1).

---

## Estado implementación E1 (2026-11-06)

| Endpoint | Estado | Notas |
| --- | --- | --- |
| GET /eventos/disponibilidad?ids= | **hecho** | Batch catálogo (máx 50 ids). 1 query `cupo_sector`. Sin filas → ítem agotado total 0 |
| GET /eventos/{id}/disponibilidad | **hecho** | Cupos `cupo_sector`; 404 si no hay filas. DTO: disponibleTotal + nivelCupo + por sector `asientosOcupados[{fila,numero,estado}]` (solo sectores numerados; el resto del layout está libre) |
| POST /eventos/{id}/cupos | **hecho** | Abre stock al publicar (body `{sectores:[{idSector,capacidadTotal≥1}]}`). Idempotente. Feign desde msvc-eventos; GW `/api/eventos/{id}/cupos` (solo ADMIN) |
| POST /eventos/{id}/cupos/liberacion | **hecho** | Devuelve stock por devolución aprobada. Body `{idSector, cantidad≥1}` o, en sector numerado, `{idSector, asientos:[{fila,numero}]}` (deben figurar VENDIDO; `cantidad` se ignora). Lock pesimista; `-vendida` `+disponible`. Feign msvc-entradas; GW order 9 (solo ADMIN) |
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
| Reportes del organizador | **hecho** | E2-REP-01. `GET /reportes/eventos/{id}` (+ `/ventas`, `/sectores`, `/ocupacion`, `/recaudacion`). ORGANIZADOR dueño o ADMIN (403 otro rol; dueño verificado por Feign a eventos). Ventas y capacidad de la base de ventas; devueltas, usadas y a reintegrar por Feign a entradas (`GET /eventos/{id}/entradas/resumen`); si eventos o entradas fallan → 503 con el cuerpo de error estándar (`ServiceUnavailableException`). |
| Fila virtual | **hecho** | FIL-01 (ingreso, posición, estado), FIL-02 (tandas) y FIL-03 (token + gate de reserva) implementados. Ver sección abajo y `specs/fila-virtual.md`. |

### Fila virtual (FIL-01 a FIL-03)

Solo eventos `PUBLICADO` con `esAltaDemanda = true`; el flag se lee por Feign a msvc-eventos con caché en memoria de `ticketya.fila.evento-cache-ttl-seconds` (10 s). Todo el estado de la fila vive en Redis (`ticketya.fila.key-prefix`, default `fila`), sin tablas MySQL nuevas.

| Endpoint (path interno) | Rol | Respuesta |
| --- | --- | --- |
| `POST /eventos/{id}/fila/ingreso` | CLIENTE (JWT, `X-User-Id`) | 201 primera vez / 200 reingreso con la misma `posicion`; 409 `FILA_EVENTO_NO_ELEGIBLE` o `FILA_TURNO_USADO` |
| `GET /eventos/{id}/fila/posicion` | CLIENTE (JWT) | 200 con `posicion`, `posicionHabilitadaHasta`, `delante`, `estado` (`EN_ESPERA`/`HABILITADO`/`CON_TOKEN`/`TURNO_USADO`); 404 `FILA_NO_INGRESADO` |
| `POST /eventos/{id}/fila/validacion` | CLIENTE (JWT, `X-User-Id`) | 200 `{idEvento, token, expiraEn, posicion, delante:0}`; mismo token mientras esté vigente; 404 `FILA_NO_INGRESADO`; 409 `FILA_FUERA_DE_TANDA` (con `posicion`, `posicionHabilitadaHasta`, `delante`) / `FILA_TURNO_USADO`; 503 |
| `GET /eventos/{id}/fila/estado` | **Público** (sin JWT, sin identidades) | 200 con `totalEnFila`, `posicionHabilitadaHasta`, `abierta` |
| `POST /eventos/{id}/fila/habilitacion` | **ADMIN** (JWT; gateway y controller verifican el rol) | 200 `{idEvento, posicionHabilitadaHasta, modo:"MANUAL"}`; 400 si `hastaPosicion` < 1; 403 otro rol; 409 `FILA_TANDA_NO_REDUCIBLE` |
| `POST /internas/fila/{id}/invalidacion` | **msvc-eventos** (Feign, `X-Internal-Token`; no expuesto por gateway) | 204; descarta la caché del flag/estado. Lo llama eventos al publicar, suspender/cancelar o cambiar `esAltaDemanda` |

`delante = max(0, posicion − posicionHabilitadaHasta)`: la posición es el turno inmutable asignado por un `INCR` atómico. Si Redis o msvc-eventos fallan sin dato en caché → **503 `FILA_NO_DISPONIBLE`** (fail-closed, nunca fallback a MySQL). El `code` viaja en el cuerpo de error (`GlobalExceptionHandler`). Config: `ticketya.fila.ttl-hours` (24), `token-ttl-minutes` (10) y el prefijo citado; las cuatro claves por evento comparten TTL. Tests: `FilaServiceTest`, `FilaControllerTest`, `FilaEventoCacheTest`, `FilaRedisStoreIT` (perfil `it`, Testcontainers) y gateway `JwtGatewayIntegrationTest`.

**Tandas (FIL-02):** la tanda es acumulativa y nunca decrece; el ADMIN puede fijarla por encima del total con `habilitacion`. En paralelo, `FilaTandaScheduler` (`ticketya.fila.auto.*`) avanza cada `intervalo-ms` en `min(tamanio, max(0, SUM(cantidad_disponible) − activos))`, topeado por el último turno asignado (`seq`). Solo una instancia corre cada tick gracias al lock `fila:auto:lock` (NX + PX); un evento no PUBLICADO se omite y cualquier error de Redis/Feign se registra sin propagarse. Cada apertura queda en bitácora con `modo=AUTO|MANUAL`, tanda anterior y nueva.

**Gate de reserva (FIL-03):** `POST /reservas` acepta el tercer campo opcional `tokenFila` (constructor de 2 argumentos conservado). En eventos de alta demanda, después de la idempotencia y de validar PUBLICADO, ventas consume el token con un compare-and-delete atómico **antes de `lockCupo`**: sin token → 409 `FILA_TOKEN_REQUERIDO`, inválido/vencido/ajeno → 409 `FILA_TOKEN_INVALIDO`, Redis caído → 503 `FILA_NO_DISPONIBLE`; en ninguno de esos casos se descuenta cupo. Un `TransactionSynchronization` restaura el token si la transacción no hace commit (límite por cliente, unique key de asientos o excepción) y registra `FILA_TOKEN_CONSUMIDO` en `afterCommit`. Los eventos normales no consultan Redis. La secuencia completa, con la rama de rollback, está en `specs/diagramas/diagramas-secuencia.md` §9.

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
- Volúmenes viejos: recrear el volumen (ver `infrastructure/mysql/README.md`).

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

### Endpoints internos (Feign, fuera del gateway)

- `GET /compras/evento/{idEvento}` — ids de clientes con compra aprobada (notificaciones).
- `GET /internas/compras?desde=&hasta=` — compras `APROBADA` con `idCompra`, `idCliente`,
  `idEvento`, `fechaCompra`, `total`, `moneda` y `cantidadEntradas`. Uso: backfill del
  historial en `msvc-preferencias` (`POST /internas/backfill`).
- `POST /internas/fila/{idEvento}/invalidacion` — lo invoca `msvc-eventos` (vía
  `VentasClient.invalidarCacheFila`) para pausar/reanudar la fila cuando cambia el estado
  del evento o el flag de alta demanda. Responde 204.

El path `/internas/**` **no** está expuesto por el gateway y exige `X-Internal-Token`
(`TICKETYA_INTERNAL_TOKEN`, default `ticketya_dev_internal`).
