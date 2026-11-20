# Prueba de carga — flujo de compra (E2-TST-03)

**Estado: PLAN VERSIONADO, SIN CORRIDA EJECUTADA (2026-11-19).**  
Los planes JMeter y el andamiaje de verificación están listos, pero **no se ejecutaron**: en este entorno no hay Docker ni JMeter disponibles. No hay métricas reales; los campos de resultados quedan explícitamente vacíos y no deben interpretarse como medidas. Ver la sección `Pendiente de ejecución`.

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

El pool debe tener al menos `usuarios` filas y cumplir `iteraciones × cantidad ≤ 6` (límite `max-entradas-cliente-evento`). El token se firma con el secreto compartido gateway/usuarios; verificar claims contra un token real de login. **[NV]** Si algún servicio valida al usuario contra `msvc-usuarios`, los usuarios del pool deben existir.

---

## Resultados (normal)

**Pendiente de ejecución.** Comando previsto:

```bash
jmeter -n -t Backend/docs/testing/carga/compra-normal.jmx \
  -JidEvento=<uuid> -JidSector=<uuid> \
  -Jusuarios=50 -Jramp=30 -Jduracion=300 \
  -l Backend/docs/testing/carga/resultados-normal.jtl \
  -e -o Backend/docs/testing/carga/report-normal
```

| Métrica | 25 usuarios | 50 usuarios | 100 usuarios |
|---|---|---|---|
| p50 (ms) | — | — | — |
| p95 (ms) | — | — | — |
| p99 (ms) | — | — | — |
| RPS | — | — | — |
| Transacciones OK / KO | — | — | — |
| Errores por código HTTP | — | — | — |

---

## Resultados (con fila)

**Pendiente de ejecución.** Requiere un evento `esAltaDemanda=true` y sector general. Comando previsto:

```bash
jmeter -n -t Backend/docs/testing/carga/compra-con-fila.jmx \
  -JidEvento=<uuid> -JidSector=<uuid> \
  -Jusuarios=50 -Jramp=30 -Jduracion=300 \
  -l Backend/docs/testing/carga/resultados-fila.jtl \
  -e -o Backend/docs/testing/carga/report-fila
```

| Métrica | B1 (tanda automática) | B2 (override ADMIN) |
|---|---|---|
| Latencia `ingreso` (p50/p95/p99) | — | — |
| Tiempo en fila (ingreso → token) | — | — |
| Latencia `validacion` | — | — |
| Latencia `crearReserva` | — | — |
| RPS | — | — |
| Errores 409 fuera de tanda / 503 / timeouts | — | — |
| Overhead vs. escenario normal | — | — |

---

## Verificación de integridad

**Pendiente de ejecución.** Repetir antes y después de cada corrida:

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

---

## Conclusión

**Pendiente.** No hay datos de ejecución.Una vez corridos A y B, completar comparación de overhead, integridad y el estado de `backlog.md:242`.
