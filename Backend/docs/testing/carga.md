# Prueba de carga — flujo de compra (E2-TST-03)

**Estado: CORRIDA FORMAL EJECUTADA (2026-11-21) — resultado PARCIAL por hallazgos.**
Se corrió la matriz A/B con 25/50/100 usuarios. El escenario B (fila) cerró con **0 errores** en los
tres puntos; A tuvo un fallo en 50 usuarios. La prueba encontró dos defectos que impiden dar
`E2-TST-03` por cerrado: **una emisión duplicada** y **timeouts de Feign a eventos mapeados a 400**.
Ver `Hallazgos` al final.

Plan de ejecución formal (matriz A/B, integridad SQL/Redis y evidencia):
[`specs/carga-e2-tst-03.md`](../../specs/carga-e2-tst-03.md).

Diseño: `Backend/specs/fila-virtual.md` §15. Antes de cada corrida: `cd Backend && docker compose down -v && docker compose up -d --build` (**`down -v` borra los datos de todas las bases**).

---

## Entorno

| Item | Valor |
|---|---|
| Gateway | `http://localhost:8080` (todas las requests van por él; **no** enviar `X-User-Id`, lo inyecta el gateway desde el JWT) |
| Servicios | 17 contenedores del compose raíz, todos arriba y bases `healthy` |
| Smoke | `curl -sf http://localhost:8080/actuator/health` **[NV]** |
| MySQL ventas | `db-mysql-ventas` |
| Redis ventas | `db-redis-ventas` |
| JMeter | 5.6.x, ejecución en modo no GUI |
| Limitación | La carga y los contenedores corren en la misma máquina: las latencias incluyen contención de CPU/RAM |

**[NV]** Credenciales de MySQL en el compose, política real de `maxmemory`/evicción de Redis y persistencia (AOF/RDB). Ver puntos abiertos 8–10 de la spec.

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

El pool debe tener al menos `usuarios` filas y cumplir `iteraciones × cantidad ≤ 6` (límite `max-entradas-cliente-evento`). El token se firma con el secreto compartido gateway/usuarios; verificar claims contra un token real de login. **[NV]** Si algún servicio valida al usuario contra `msvc-usuarios`, los usuarios del pool deben existir.

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

1. **Emisión duplicada (bloqueante para integridad).** Una compra de 1 entrada terminó con 2 filas
   en `entrada` (`3d558321-0f73-4649-97f3-dc21981f994d`, A25). `EntradaServiceImpl.emitir` hace
   *check-then-insert* sin lock; los reintentos de `EmisionEntradasService` (Feign con timeout) pueden
   ejecutar la emisión dos veces. Requiere idempotencia transaccional por compra (claim/lock), no solo
   un `findByIdCompra` previo. Mientras no se corrija, un cliente podría ingresar dos veces.
2. **Timeout de Feign a eventos mapeado a 400.** `VentaServiceImpl.fetchEvento` traduce cualquier
   `FeignException` a `IllegalArgumentException` (400). Bajo carga el detalle de evento tardó 3,7 s
   y superó el `readTimeout` de 3 s, cortando la compra de A50. Corresponde un **503** (igual que en
   reportes) y evaluar retry/backoff o subir el timeout.
3. **Arranque en frío de ventas.** El primer B25 tras recrear `ms-ventas` agotó el pool Hikari
   (`maximum-pool-size=10`) y devolvió 500 con esperas de 30 s. En caliente, B25/B50/B100 dieron 0
   errores. Mitigación: warm-up antes de medir y/o subir el pool; documentar la capacidad real.
4. **Tiempo en fila dependiente del ramp.** El scheduler abre turnos al ritmo de `activos`; el
   indicador “ingreso → validación” no es una medida pura de la cola. Para comparar escenarios,
   fijar el ramp o habilitar tandas por ADMIN (B2).

## Conclusión

**Parcial.** La matriz 25/50/100 se ejecutó y el escenario con fila cerró con **0 errores** en los
tres puntos (hasta **45,3 RPS** y p95 global de 1,7 s a 100 usuarios). El escenario normal tuvo un
fallo de dependencia en 50 usuarios (timeout → 400) y la verificación de integridad detectó **una
entrada duplicada**. `E2-TST-03` no se cierra hasta corregir los hallazgos 1 y 2 y repetir A50/A25
sobre un stack limpio.

Plan y criterios: [`specs/carga-e2-tst-03.md`](../../specs/carga-e2-tst-03.md).
