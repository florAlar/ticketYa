# E2-TST-03 — Prueba de carga del flujo de compra

> **Estado:** ejecutada y cerrada (2026-11-21) · **Fecha:** 2026-11-21 · **Alcance:** flujo de
> compra (reserva → compra → pago → emisión) por el gateway, con y sin fila virtual · **No incluye:**
> carga de UI, multi-instancia ni red real.

**Resultado:** la matriz A/B 25/50/100 se ejecutó y cerró el 2026-11-21.

| Punto | Resultado |
| --- | --- |
| A (compra normal) | 25/0, 49/1 y 100/0 transacciones OK/KO; p95 4,4 / 8,7 / 3,1 s. El KO de A50 fue un timeout de Feign→400, corregido a 503. |
| B (fila) | 0 errores en 25/50/100; hasta **45,3 RPS** a 100 usuarios (p95 global 1,7 s). |
| Integridad | Invariante de cupo OK y `evicted_keys = 0`; 1 emisión duplicada en A25 detectada y corregida. |
| Reintentos post-fix | A25, A50 y B25 con 0 errores y 0 duplicados. |

**Desvíos registrados:** el primer intento de B25 se descartó por 500 (cold start del pool Hikari;
ver [`Hallazgos`](../docs/testing/carga.md#hallazgos)) y A50 registró 405×1 sin explicar además del
400×2.

Este documento conserva el **plan de ejecución** y el **registro** de la corrida formal. Las tablas
completas, hallazgos y reintentos están en
[`docs/testing/carga.md`](../docs/testing/carga.md#resultados-normal); los planes JMeter y los
scripts, en [`docs/testing/carga/`](../docs/testing/carga/).

## 1. Objetivo y criterios de aceptación

- Medir throughput, latencia (media y p50/p95/p99) y porcentaje de error del flujo de compra con
  **25, 50 y 100 usuarios concurrentes**, en dos escenarios:
  - **A — compra normal:** evento sin alta demanda.
  - **B — compra con fila:** evento de alta demanda, ingreso → posición → validación → reserva.
- **Cero errores 5xx y cero 401.** Los 409 solo son aceptables si el cupo o el límite por cliente se
  agotan (no debería ocurrir con la matriz elegida).
- **Integridad:** invariante de cupo
  `capacidad = disponible + reservada + vendida`, sin contadores negativos, sin asientos duplicados,
  `entradas emitidas = pagos APROBADOS`, `evicted_keys = 0`.
- Completar las tablas de `docs/testing/carga.md` y pasar `E2-TST-03` a `hecho` en el backlog con
  fecha, comandos y evidencia.

> **Desvío de la corrida original:** A50 registró 400×2 (timeout de Feign, corregido a 503) y 405×1
> (sin explicar en el registro), con 49 OK / 1 KO. Los reintentos A25/A50/B25 no repitieron errores.
> El primer B25 también se descartó por 500 de cold start (pool Hikari); ver
> [`Hallazgos`](../docs/testing/carga.md#hallazgos).

## 2. Estado actual (qué ya está listo)

- **Planes JMeter corregidos** (2026-11-20): usan `/api/reservas` (no `/api/ventas/reservas`), **no**
  mandan `X-User-Id` (lo inyecta el gateway), leen un pool `email,token` del CSV, generan
  `Idempotency-Key` por iteración y corren con `-Jiteraciones=1 -JloopForever=false`.
- **Sectores generales** en ambos planes (no numerados), para que los hilos no compitan por el mismo
  asiento: A usa `General Admission`, B usa `Campo`.
- **Scripts:** `generar-usuarios.sh` (registra y loguea N usuarios, JWT de 1 h) y `jmeter.sh`
  (regenera el CSV y ejecuta el plan).
- **Fila virtual:** token atómico con `SET fila:{evento}:usados`, consumo al inicio del gate y
  restauración por rollback, fail-closed (`503 FILA_NO_DISPONIBLE`), scheduler automático de tandas
  (`TICKETYA_FILA_AUTO_*`) y caché del flag de alta demanda.
- **Humo:** corrida de 5 usuarios con 0 % de error (2026-11-20), que valida el andamiaje.

> El review de la fila ([`fila-virtual.md`](fila-virtual.md)) describe una versión anterior del
> diseño: el Lua ya devuelve
> `tonumber(pos)`, el token se consume de forma atómica y se restaura si la transacción no commitea,
> el gate es fail-closed y las tandas son automáticas. Lo que sigue vigente es monitorear
> **eviction** de Redis (`maxmemory 256mb`, `volatile-lru`) y la coherencia de TTL.

## 3. Precondiciones

- Docker accesible (`sg docker -c ...` si la sesión no heredó el grupo) y el stack sano.
- JMeter 5.6.x, JDK 17/21, `curl` y `jq`.
- Un `.env` con los defaults de fila (o variables exportadas en el shell).
- Tokens nuevos por corrida: los JWT vencen a la hora.

### 3.1 Datos de prueba (seeds)

| Escenario | Evento | Sector | Capacidad total | Alta demanda |
| --- | --- | --- | --- | --- |
| A (normal) | `d0000000-0000-4000-8000-000000000006` (Foo Fighters) | `c0000000-0000-4000-8000-000000000015` (General Admission) | 65 000 | no |
| B (fila) | `d0000000-0000-4000-8000-000000000001` (Metallica) | `c0000000-0000-4000-8000-000000000011` (Campo) | 60 000 | sí |

B arranca con **1 entrada ya vendida** en el seed: capacidad total 60 000 y disponible inicial
59 999 (`infrastructure/mysql/ventas/init.sql`).

Cada usuario compra **1 entrada** (`-Jcantidad=1`) en **1 iteración**: 100 usuarios consumen 100
unidades, muy por debajo del cupo y del límite de 6 por cliente/evento.

### 3.2 Reset y arranque

```bash
cd Backend
docker compose down -v          # borra las bases y vuelve a sembrar
docker compose up -d --build
# esperar healthy; el E2E usa el mismo criterio
./scripts/status.sh --http
```

Recomendado: un reset **antes de A** y otro **antes de B**, para que los deltas de cupo de cada
familia arranquen de un estado conocido. Si se ejecuta JMeter a mano (sin `jmeter.sh`), regenerar el
CSV después de cada reset; el script lo hace solo en cada corrida.

### 3.3 Entorno de la corrida

| Item | Valor |
| --- | --- |
| Gateway / servicios | `http://localhost:8080` + los 17 contenedores del compose raíz, bases `healthy` |
| JMeter | 5.6.3, modo no GUI (`-n`) |
| Carga | JMeter y contenedores en la misma máquina: las latencias incluyen contención de CPU/RAM |
| Hardware | No registrado en la corrida (mejora de trazabilidad pendiente) |
| Código bajo prueba | `main` al 2026-11-21, con los fixes de ese día; sin SHA puntual registrado |

## 4. Matriz de corridas

| ID | Plan | Usuarios | Ramp (s) | Iteraciones | Duración estimada |
| --- | --- | --- | --- | --- | --- |
| W | `compra-normal.jmx` | 5 | 3 | 1 | ~30 s (warm-up, se descarta) |
| A25 | `compra-normal.jmx` | 25 | 13 | 1 | ~1 min |
| A50 | `compra-normal.jmx` | 50 | 26 | 1 | ~1–2 min |
| A100 | `compra-normal.jmx` | 100 | 51 | 1 | ~2–3 min |
| B25 | `compra-con-fila.jmx` | 25 | 13 | 1 | ~2 min |
| B50 | `compra-con-fila.jmx` | 50 | 26 | 1 | ~2–3 min |
| B100 | `compra-con-fila.jmx` | 100 | 51 | 1 | ~3–5 min |

- Si la varianza entre repeticiones es alta (> 15 % en p95), repetir el punto una vez y promediar.
- No superar los 10 minutos por corrida: la reserva tiene TTL y el JWT vence a la hora.

## 5. Procedimiento

### 5.1 Preparar el escenario A

```bash
cd Backend
docker compose down -v && docker compose up -d --build
# esperar servicios
cd docs/testing/carga
./jmeter.sh compra-normal.jmx 25        # warm-up + A25
```

El script deja `corrida.jtl` y `report/`; **renombrar la salida por escenario** (o pasar `-o` a
mano) para no pisarla:

```bash
mv corrida.jtl corrida-A25.jtl
mv report report-A25
```

Repetir con `50` y `100`. El script regenera el CSV en cada corrida.

### 5.2 Preparar el escenario B (fila)

La fila abre tandas automáticamente cada `intervalo-ms` con `tamanio` usuarios. Para 100 usuarios,
con el default (25 cada 10 s) se necesitan 4 tandas (~40 s) y el poll por defecto equivale a 60 s
(30 × 2 s): funciona, pero queda ajustado. Recomendado **abrir todos los turnos en la primera tanda**:

```bash
# recrear ventas con tandas de 100 cada 5 s (solo para la corrida)
TICKETYA_FILA_AUTO_TAMANIO=100 TICKETYA_FILA_AUTO_INTERVALO_MS=5000 \
  docker compose up -d --force-recreate ms-ventas
```

Si se prefiere la variante manual **B2** (sin scheduler):

```bash
TICKETYA_FILA_AUTO_ENABLED=false docker compose up -d --force-recreate ms-ventas
# y en JMeter:
-JadminThreads=1 -JadminToken=<JWT_ADMIN> -JadminPaso=100 -JadminIntervalo=5000
```

```bash
cd Backend/docs/testing/carga
./jmeter.sh compra-con-fila.jmx 25
mv corrida.jtl corrida-B25.jtl && mv report report-B25
# repetir con 50 y 100
```

> **Rangos del pool:** `generar-usuarios.sh` crea `carga<base>..carga<base+N-1>`. Elegir un `base`
> que no se solape con corridas anteriores: si un usuario ya consumió su turno, el ingreso responde
> `409 FILA_TURNO_USADO`.

Parámetros sugeridos para B: `-JpollMax=60 -JpollInterval=1000` (poll cada 1 s, hasta 60 s) para
absorber el tiempo de la tanda sin marcar falsos errores.

### 5.3 Warm-up

La primera corrida de cada familia calienta JIT, pools y cachés. Ejecutar W y descartarlo; no
incluirlo en las tablas.

## 6. Métricas y verificación de integridad

### 6.1 JMeter

Por escenario, extraer del reporte HTML o del JTL:

- Throughput (req/s), media, p50, p90, p95, p99.
- `error %` global y por sampler (`reserva`, `compra`, `pago`, `verificación`).
- Distribución de códigos: 2xx, 4xx (discriminar 409), 5xx, timeouts.

### 6.2 Base de datos (antes y después de cada corrida)

```sql
-- invariante por sector
SELECT id_evento, id_sector, capacidad_total, cantidad_disponible,
       cantidad_reservada, cantidad_vendida,
       capacidad_total - (cantidad_disponible + cantidad_reservada + cantidad_vendida) AS dif
FROM cupo_sector
WHERE id_evento = :idEvento AND id_sector = :idSector;

-- compras por evento (compra no tiene id_evento: se llega por reserva)
SELECT c.estado, COUNT(*) FROM compra c
JOIN reserva r ON r.id_reserva = c.id_reserva
WHERE r.id_evento = :idEvento GROUP BY c.estado;

-- pagos aprobados por evento
SELECT COUNT(*) FROM pago p
JOIN compra c ON c.id_compra = p.id_compra
JOIN reserva r ON r.id_reserva = c.id_reserva
WHERE r.id_evento = :idEvento AND p.estado = 'APROBADO';

-- entradas emitidas (DB `entradas`, contenedor db-mysql-entradas)
SELECT COUNT(*) FROM entrada WHERE id_evento = :idEvento;

-- asientos duplicados (solo si se prueba un sector numerado)
SELECT id_evento, id_sector, fila, numero, COUNT(*) FROM asiento_reservado
WHERE id_evento = :idEvento
GROUP BY id_evento, id_sector, fila, numero HAVING COUNT(*) > 1;
```

Comprobaciones:

- `dif = 0`, ningún contador negativo.
- `Δ vendidas = usuarios × cantidad` (1 por usuario).
- `compras APROBADAS = usuarios`, `pagos APROBADOS = usuarios`.
- Entradas emitidas por evento = usuarios (DB `entradas`, contenedor `db-mysql-entradas`; la emisión
  es asincrónica).
- Sin asientos duplicados.

### 6.3 Redis

```bash
docker exec db-redis-ventas redis-cli INFO stats | grep evicted_keys   # debe ser 0
docker exec db-redis-ventas redis-cli ZCARD fila:<idEvento>
docker exec db-redis-ventas redis-cli SCARD fila:<idEvento>:usados
docker exec db-redis-ventas redis-cli GET fila:<idEvento>:tanda   # última posición habilitada
```

- `evicted_keys = 0` (si no, la fila o el stock pueden desalojarse).
- En B: `usados ≈ usuarios` y `tanda ≥` la mayor posición usada.

### 6.4 Logs

- Gateway y servicios sin 5xx; `X-Correlation-Id` por request.
- `msvc-ventas`: rechazos de fila (`FILA_GATE_RECHAZO`, `FILA_TOKEN_*`) y conflictos de cupo.

## 7. Evidencia y documentación

- `docs/testing/carga/corrida-<ID>.jtl` y `report-<ID>/` quedan locales: el `.gitignore` ignora
  `Backend/docs/testing/carga/*.jtl` y `report-*/`; **no** commitear los reportes.
- Tablas de `docs/testing/carga.md` completas (métricas, errores, integridad, conclusión). Esa es la
  evidencia versionada.
- `specs/backlog.md`: `E2-TST-03` → `hecho` con fecha, matriz y resultado.
- Resumen en el PR: comandos y limitación “carga y contenedores en la misma máquina”.

## 8. Riesgos y mitigaciones

| Riesgo | Mitigación |
| --- | --- |
| JMeter y 17 contenedores comparten CPU/RAM | Documentar como límite del entorno; usar p95/p99 y no solo medias; cerrar apps de escritorio |
| El servidor de este entorno se reinicia | Correr por escenarios cortos y guardar JTL/reportes después de cada uno; no encadenar toda la matriz en un solo proceso |
| JWT vence a la hora | Regenerar el CSV antes de cada corrida; no superar 10 min por corrida |
| La tanda no llega dentro del poll | Subir `TICKETYA_FILA_AUTO_TAMANIO`/bajar el intervalo o usar B2 con `adminPaso` |
| 409 por límite de 6 entradas/cliente | 1 iteración por usuario; no reutilizar el mismo usuario en varias corridas |
| Eviction de Redis | Monitorear `evicted_keys`; si ocurre, subir `maxmemory` y repetir |
| Primer request lento / cold start | Warm-up descartado + ramp-up; el pool Hikari arranca en 30 (`TICKETYA_DB_POOL`) y `CannotCreateTransactionException` responde 503, no 500 |

## 9. Estimación

- Reset + arranque: 5–10 min por familia.
- Corridas A: 3 × ~2 min. Corridas B: 3 × ~4 min.
- Análisis y carga de tablas: 20–30 min.
- **Total: ~1 h** en un entorno sano; prever una segunda sesión si hay reinicios. *(Estimación previa;
  la corrida real se ejecutó el 2026-11-21.)*

## 10. Fuera de alcance

- 200+ usuarios, pruebas multi-instancia o de red real.
- Carga de la UI o del frontend.
- Comparación de proveedores de pago (el pago es mock).
- Corrección de la fila virtual: los hallazgos del review ya están implementados; este plan solo
  monitorea eviction/TTL.

## 11. Checklist de cierre

- [x] Stack limpio y healthy antes de cada familia.
- [x] Warm-up ejecutado y descartado.
- [x] A25, A50 y A100 ejecutados con JTL y reporte guardados.
- [x] B25, B50 y B100 ejecutados con JTL y reporte guardados.
- [x] Tablas de `docs/testing/carga.md` completas.
- [x] Integridad SQL/Redis verificada en cada punto (invariante OK; sin duplicados tras el fix).
- [x] Cold start de ventas documentado y mitigado (pool Hikari 30 + 503).
- [x] `E2-TST-03` actualizado a `hecho` en el backlog.
- [x] Resultados y hallazgos reflejados en `docs/testing/carga.md` y en esta spec.
