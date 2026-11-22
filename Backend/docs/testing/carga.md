# Prueba de carga — flujo de compra (E2-TST-03)

**Estado: CORRIDA COMPLETA (2026-11-21) — hecha.**
Se corrió la matriz A/B con 25/50/100 usuarios. B (fila) cerró con **0 errores** en los tres puntos;
A tuvo un fallo en 50 usuarios y la integridad detectó una emisión duplicada. Ambos defectos se
corrigieron y se validaron con los reintentos A25/A50 y B25, todos con 0 errores y sin duplicados.
Ver `Hallazgos` y `Reintento post-fix`.

Plan de ejecución formal (matriz A/B, integridad SQL/Redis y evidencia):
[`specs/carga-e2-tst-03.md`](../../specs/carga-e2-tst-03.md).

Diseño: `Backend/specs/fila-virtual.md` §15. Antes de cada corrida: `cd Backend && docker compose down -v && docker compose up -d --build` (**`down -v` borra los datos de todas las bases**).

---

## Entorno

| Item | Valor |
|---|---|
| Gateway | `http://localhost:8080` (todas las requests van por él; **no** enviar `X-User-Id`, lo inyecta el gateway desde el JWT) |
| Servicios | 17 contenedores del compose raíz, todos arriba y bases `healthy` |
| Smoke | `curl -sf http://localhost:8080/actuator/health` **[V]** |
| MySQL ventas | `db-mysql-ventas` |
| Redis ventas | `db-redis-ventas` |
| JMeter | 5.6.x, ejecución en modo no GUI |
| Limitación | La carga y los contenedores corren en la misma máquina: las latencias incluyen contención de CPU/RAM |

**[V]** Credenciales de MySQL en el compose: `db-mysql-ventas` con usuario `ticketya` y `MYSQL_PASSWORD=${MYSQL_PASSWORD:-ticketya_dev}` (root: `${MYSQL_ROOT_PASSWORD:-root_dev}`, ver `.env.example` raíz), usadas para los snapshots de cupo/entradas. **[V]** `redis.conf`: `maxmemory 256mb`, `volatile-lru` y `appendonly yes`; la corrida midió `evicted_keys=0`.

---

## Parámetros

| Variable | Default | Uso |
|---|---|---|
| `-Jusuarios` | 25 | Hilos concurrentes (probar 25 / 50 / 100) |
| `-Jramp` | 30 | Ramp-up en segundos |
| `-Jduracion` | 300 | Duración en segundos (**< 600** para no cruzar el TTL de reserva de 10 min) |
| `-JidEvento` | — | UUID del evento PUBLICADO (`esAltaDemanda=false` en el escenario A) |
| `-JidSector` | — | UUID de un **sector general** (no numerado) con cupo suficiente |
| `-Jcantidad` | 1 | Entradas por reserva |
| `-Jcsv` | `usuarios.csv` | Pool `email,token` con un JWT firmado (HS256) por hilo |
| `-JpollInterval` | 2000 | Intervalo del poll a `/fila/posicion` (ms) en el escenario B |
| `-JpollMax` | 30 | Reintentos máximos del poll; agotarlos cuenta como error (timeout), no como éxito |
| `-JadminThreads` | 0 | Hilos ADMIN en B: `0` = B1 (tanda automática), `1` = B2 (override manual) |
| `-JadminToken` | — | JWT de ADMIN para B2 (el gateway y ventas verifican el rol) |
| `-JadminPaso` | 25 | Incremento de `hastaPosicion` por tick del ADMIN en B2 |
| `-JadminIntervalo` | 10000 | Intervalo del hilo ADMIN (ms) en B2 |

El pool debe tener al menos `usuarios` filas y cumplir `iteraciones × cantidad ≤ 6` (límite `max-entradas-cliente-evento`). El token se firma con el secreto compartido gateway/usuarios; verificar claims contra un token real de login. **[V]** Ningún servicio valida al usuario contra `msvc-usuarios` en el flujo de compra (el JWT es stateless y el gateway valida la firma); solo `msvc-notificaciones` consulta `GET /usuarios/{id}` para resolver el email de forma *fail-soft*, así que los usuarios del pool no necesitan existir para la carga.

---

## Resultados (normal)

Corrida 2026-11-21, stack local (17 contenedores + JMeter en la misma máquina), JMeter 5.6.3,
`-Jiteraciones=1 -JloopForever=false`, evento Foo Fighters (`d000…006`) / sector General Admission
(`c000…015`). Por cada usuario: reserva → compra → pago → verificación.

| Métrica | 25 usuarios | 50 usuarios | 100 usuarios |
|---|---|---|---|
| p50 (ms) | 1477 | 1977 | 995 |
| p95 (ms) | 4372 | 8691 | 3094 |
| p99 (ms) | 6126 | 10252 | 3511 |
| RPS | 4,67 | 5,00 | 7,57 |
| Transacciones OK / KO | 25 / 0 | 49 / 1 | 100 / 0 |
| Errores por código HTTP | — | 400×2, 405×1 | — |

Latencia por paso (p95, ms):

| Paso | 25 | 50 | 100 |
|---|---|---|---|
| `POST /api/reservas` | 4276 | 6375 | 2732 |
| `POST /api/compras` | 2164 | 4803 | 1167 |
| `POST /api/compras/{id}/pagos` | 6126 | 10252 | 3467 |
| `GET /api/entradas` | 1643 | 2055 | 881 |

El fallo de A50 fue una cadena: el `GET /eventos/{id}` interno a eventos tardó **3,7 s** y superó el
`readTimeout` de Feign (3 s); ventas lo tradujo a **400** (debería ser 503/retry) y el usuario no
pudo reservar.

---

## Resultados (con fila)

Corrida 2026-11-21 con **B1** (tanda automática: `TICKETYA_FILA_AUTO_TAMANIO=100`,
`TICKETYA_FILA_AUTO_INTERVALO_MS=5000`), evento Metallica (`d000…001`, alta demanda) / sector Campo
(`c000…011`), `-JpollMax=60 -JpollInterval=1000`. B2 (override ADMIN) no se usó.

| Métrica | 25 usuarios | 50 usuarios | 100 usuarios |
|---|---|---|---|
| `ingreso` p50/p95/p99 (ms) | 20 / 121 / 208 | 23 / 108 / 246 | 30 / 302 / 387 |
| Tiempo en fila p50/p95/p99 (ms) | 74 246 / 75 177 / 75 318 | 77 825 / 80 451 / 80 677 | 93 183 / 97 059 / 97 683 |
| `validacion` p50/p95/p99 (ms) | 86 / 329 / 351 | 121 / 577 / 928 | 160 / 1132 / 3144 |
| `reserva con tokenFila` p50/p95/p99 (ms) | 946 / 2701 / 3024 | 3061 / 5720 / 6078 | 1542 / 4508 / 4883 |
| RPS (todas las requests) | 18,58 | 29,78 | 45,31 |
| Errores 409/503/timeouts | 0 | 0 | 0 |
| Compra completa p50 (ingreso → pago) | 77,7 s | 88,5 s | 97,9 s |

> El “tiempo en fila” está dominado por el **ramp** y por el scheduler: `siguienteTanda` abre
> `min(tamaño, cupo − activos)`, así que la tanda crece al ritmo en que entran los usuarios. No es
> una medida pura de la cola. El RPS incluye los polls de posición (60 por usuario como máximo).
>
> Un primer intento de B25 inmediatamente después de recrear `ms-ventas` dio **500 por agotamiento
> del pool Hikari** (10 conexiones, `active=10`, esperas de 30 s). El reintento en caliente dio 0
> errores; se descarta el primero y queda como riesgo de arranque en frío (ver Hallazgos).

---

## Verificación de integridad

Se tomaron snapshots antes y después de cada corrida (cupo, compras/pagos, entradas, duplicados y
`evicted_keys`). Consultas base:

```sql
SELECT id_sector, capacidad_total, cantidad_disponible,
       cantidad_reservada, cantidad_vendida
FROM cupo_sector
WHERE id_evento = :idEvento AND id_sector = :idSector;
```

Comprobaciones:

- `capacidad_total = cantidad_disponible + cantidad_reservada + cantidad_vendida` en ambos snapshots.
- Ningún contador negativo.
- `Δ(cantidad_reservada + cantidad_vendida)` coincide con las operaciones exitosas de JMeter.
- Sector general: cruzar contra `detalle_reserva` y compras `APROBADA`.
- Sector numerado (solo si se prueba uno): `SELECT id_sector, fila, numero, COUNT(*) FROM asiento_reservado ... HAVING COUNT(*) > 1` → 0 filas.
- Redis: `evicted_keys` → 0.
- Con fila, además: `ZCARD fila:<idEvento>`, `SCARD fila:<idEvento>:usados`, `GET fila:<idEvento>:tanda`.

**Resultado 2026-11-21:**

- Invariante de cupo **OK** en todos los snapshots (A y B), sin contadores negativos.
- A: 180 compras / 181 entradas → **1 compra con 2 entradas** (`3d558321-0f73-4649-97f3-dc21981f994d`,
  observada en A25). Es un hallazgo real de emisión duplicada, no un artefacto del plan.
- B: 164 compras / 164 entradas antes del último punto y 189/189 al cierre, sin duplicados.
- `evicted_keys = 0` en todas las corridas.
- Las reservas que quedaron `PENDIENTE` por fallos expiraron y devolvieron el cupo (contadores
  consistentes en el snapshot siguiente).

---

## Hallazgos

1. **Emisión duplicada — corregido.** Una compra de 1 entrada había terminado con 2 filas en
   `entrada` (`3d558321-…`, A25) porque `emitir` hacía *check-then-insert* sin lock y ventas
   reintentaba la emisión. **Fix:** tabla `emision_compra` (claim por compra) insertada en la misma
   transacción que las entradas; si el claim ya existe, `emitir` responde 503 y ventas reintenta.
   Verificado con `EmisionConcurrenciaIT` (8 hilos sobre la misma compra → 2 entradas) y con el
   reintento de A25/A50 (sin duplicados).
2. **Timeout de Feign a eventos mapeado a 400 — corregido.** `VentaServiceImpl.fetchEvento` traduce
   `FeignException` a **503** (`ServiceUnavailableException`) en lugar de 400; el reintento de A50
   cerró sin errores. Se mantiene el `readTimeout` de 3 s (fail-fast) y el warm-up previo a medir.
3. **Arranque en frío de ventas — mitigado.** El primer punto tras recrear `ms-ventas` agotaba el
   pool Hikari (10) y devolvía 500. **Fix:** `maximum-pool-size=30` (`TICKETYA_DB_POOL`) y
   `CannotCreateTransactionException` → **503**; además, warm-up descartado antes de cada familia.
4. **Tiempo en fila dependiente del ramp.** El scheduler abre turnos al ritmo de `activos`; el
   indicador “ingreso → validación” no es una medida pura de la cola. Para comparar escenarios,
   fijar el ramp o habilitar tandas por ADMIN (B2).

## Reintento post-fix (2026-11-21)

| Punto | Transacciones OK/KO | Errores HTTP | p95 global | Duplicados |
|---|---|---|---|---|
| A25 (normal) | 25 / 0 | — | 5559 ms | 0 |
| A50 (normal) | 50 / 0 | — | 6736 ms | 0 |
| B25 (fila) | 25 / 0 | — | 564 ms | 0 |

En los tres: `entradas = compras`, una fila de `emision_compra` por compra, invariante de cupo OK y
`evicted_keys = 0`. El escenario B50/B100 ya había cerrado con 0 errores en la matriz original.

## Conclusión

**Hecha.** La matriz 25/50/100 se ejecutó; los dos defectos detectados (emisión duplicada y
timeout→400) están corregidos y validados con los reintentos A25/A50 y con un IT de concurrencia.
B cerró con 0 errores en los tres puntos (hasta **45,3 RPS** y p95 global de 1,7 s a 100 usuarios).
Queda como límite del entorno que la carga y los contenedores comparten CPU/RAM.

Plan y criterios: [`specs/carga-e2-tst-03.md`](../../specs/carga-e2-tst-03.md).
