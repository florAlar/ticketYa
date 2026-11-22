# Plan de implementación — Fila virtual + prueba de carga (Entrega 2)

> **HISTÓRICO (2026-11-19).** Este documento es el plan previo a la implementación. La
> especificación vigente es [`specs/fila-virtual.md`](../../specs/fila-virtual.md) y los resultados
> de carga están en [`docs/testing/carga.md`](../../docs/testing/carga.md). Varias decisiones fueron
> reemplazadas durante la implementación; ver **Diferencias con la implementación final** (§2.1).

> **Especificación ejecutable paso a paso:** `especificacion-paso-a-paso.md` (mismo directorio). Este archivo es el resumen de decisiones, auditoría inicial y alcance por US.

> **Numeración:** este plan usaba `FIL-02`=estado, `FIL-03`=tandas, `FIL-04`=token/gate y
> `FIL-05`=cierre. La implementación final fue `FIL-01`=ingreso/posición/estado, `FIL-02`=tandas,
> `FIL-03`=token/gate y `FIL-04`=cierre de contrato (commits del 2026-11-19).

> Estado: **histórico (superseded)**. La implementación (FIL-01…FIL-04), la UI de fila y
> `E2-TST-03` están cerradas y verificadas.
> Todas las rutas son relativas a `ticketYa/`.
> Convención: **[V]** verificado en esta pasada · **[I]** inferido del código · **[NV]** no verificado / no ejecutado.

---

## 1. Chequeo único backlog ↔ código real (sólo esta vez) — histórico

> Registro del 2026-11-19. Los estados "pendiente" de la tabla ya se cerraron; ver §2.1 y
> `specs/fila-virtual.md`.

No se repetirá esta auditoría. Desde acá, cada US actualiza backlog/docs **en el mismo commit**.

| Entrada | Backlog / doc | Código real | Resultado |
|---|---|---|---|
| `E2-FILA-01` | `Backend/specs/backlog.md:221` pendiente, "simplificable con justificación" | Sin controller/service/DTO de fila en ventas; `Backend/msvc-ventas/src/main/java/ventas/queue/VentaQueueProducer.java:1-4` y `VentaQueueConsumer.java:1-4` vacíos; el gateway **sí** rutea (`Backend/APIGateway/src/main/resources/application.yml:50-55`) | **[V] coincide** |
| `API-04` | `Backend/specs/backlog.md:117` parcial, "falta fila virtual y notificaciones" | Sin `/fila` en `Backend/docs/openapi/*.json`; no existe spec de notificaciones | **[V] coincide** |
| `DOC-MS-VEN` | `Backend/specs/backlog.md:65` parcial, "falta plantilla + fila E2" | `Backend/msvc-ventas/docs/endpoints.md` existe, sin sección `/fila` | **[V] coincide** |
| `MOD-06` | `Backend/specs/backlog.md:83` "falta cuando entren fila, **notificaciones y recomendaciones**" | Notificaciones y recomendaciones **ya están** en `Backend/specs/diagramas/modelo.md:176,208` y `modelo-por-microservicio.md:240-282` | **[V] NO coincide**: sólo falta fila |
| Contrato de 5 endpoints | `Backend/specs/endpoints.md:206-212` | No implementados | **[V] coincide como diseño** |
| Docs de estado | `Backend/docs/api/gateway.md:18,39`, `Backend/docs/openapi/README.md:120` dicen "no implementada" | Sin impl | **[V] coincide** |
| Comunicación / BD | `Backend/specs/comunicacion.md:140`, `Backend/specs/bases-de-datos.md:13` (fila en Redis, pendiente) | Redis disponible (`Backend/msvc-ventas/src/main/java/ventas/redis/RedisConfig.java:12-15`); sin tabla de fila en `Backend/infrastructure/mysql/ventas/init.sql:8-85` | **[V] coincide** |
| Reglas de negocio | `Backend/specs/reglas-de-negocio.md:42` "pendiente E2"; decisión 16 abierta en `:265` | Sin impl; decisión efectivamente abierta | **[V] coincide** |
| `E2-TST-03` | `Backend/specs/backlog.md:242` **pendiente**; `Backend/specs/consigna.md:73,159-163,171` lo exige | No hay informe ni script de carga versionado en `Backend/docs/` | **[V] coincide**, entregable en cero |

### Inconsistencias fuera del alcance de la fila — `0b` opcional, **no incluido por defecto**

- Resumen §9 y nota final de `Backend/specs/backlog.md:377-390,419` siguen marcando E2-PRE/NOT/HIG como pendientes, aunque las filas `:223-243` y el código las dan por hechas.
- `Backend/docs/openapi/README.md:120` también lista `/reportes` como no implementado (sí lo está, `Backend/specs/backlog.md:228`). La parte `/fila` de esa misma línea sí se corrige en `FIL-05`.
- `README.md:89` y `Frontend/README.md:32,47` dicen que QR/reportes/preferencias están apagados, pero `Frontend/src/config/featureGates.js:58,64,67,73` los tiene en `!USE_MOCK`.
- `Backend/specs/microservicios.md` y `Backend/specs/arquitectura.md` seguían con lenguaje de "scaffold" (corregido 2026-11-22).

---

## 2. Decisiones confirmadas (no volver a discutir)

1. **Redis-only.** Fila, posiciones, tanda y token viven en Redis (`StringRedisTemplate`, `RedisConfig.java:12-15`). **No hay tablas MySQL nuevas ni Rabbit** para la fila. `VentaQueueProducer/Consumer` siguen vacíos; no se usan. Justificación formal en `specs/consigna.md:148` y en el doc creado en `0a`.
2. **Gate sólo si `esAltaDemanda = true`.** Protege el flujo E1 y los requests de Postman existentes. Hoy el flag no llega a ventas: agregarlo a `EventosClient.java:23-30` (sí existe en `Evento.java:53-54` y `EventoMapper.java:35`).
3. **Fail-closed en endpoints de fila / fail-open dentro de `crearReserva`.** Si Redis cae: `503` en `ingreso`, `posicion`, `validacion`, `estado` y `habilitacion` (se pierde el orden); en `crearReserva` se registra en bitácora y se continúa (no se pierde integridad: MySQL sigue con lock de `cupo_sector` y `uk_asiento_evento_sector_fila_numero`, `init.sql:71,85`). Patrón de referencia: `StockCache.java:28-73` es fail-open; acá se diverge a propósito y se documenta. **Reemplazado:** la implementación final es **fail-closed** también en el gate (D3 de `specs/fila-virtual.md`); ver §2.1.
4. **Sin UI de fila.** Queda fuera. El front ya cubre los flujos principales del cliente; la consigna prioriza backend. **Reemplazado:** la UI de fila se implementó en 2026-11-20 (`FilaVirtualPage`); ver §2.1.

### Numeración (histórica)

**`0a` → `FIL-01` → `FIL-02` → `FIL-03` → `FIL-04` → `TST-CARGA-01` (Partes A y B).**

- Esta sección conserva la numeración del plan, ya reemplazada: en la implementación final
  `FIL-01` incluye ingreso/posición/estado, `FIL-02` las tandas, `FIL-03` el token/gate y
  `FIL-04` el cierre de contrato (docs only). Ver `specs/fila-virtual.md` §12 y §18.
- `TST-CARGA-01` es **una sola US con dos pasos**: Parte A (compra normal) y Parte B (con fila,
  después de que existan el gate y la habilitación).
- `0b` es opcional y no está en la secuencia salvo autorización explícita.

## 2.1 Diferencias con la implementación final

Este plan se escribió antes de codificar. Estas son las decisiones que cambiaron y el estado real
(la fuente de verdad es `specs/fila-virtual.md`):

| Tema | En este plan | Implementación final |
| --- | --- | --- |
| Redis caído en `crearReserva` | **Fail-open** (bitácora y continuar) | **Fail-closed:** `503 FILA_NO_DISPONIBLE` también en el gate (D3 de `fila-virtual.md`). La fila es control de acceso, no un caché. |
| Habilitación de tandas | Manual con hilo ADMIN obligatorio en la carga | **Tanda automática** (`FilaTandaScheduler`, `TICKETYA_FILA_AUTO_*`); el hilo ADMIN es solo el override B2. |
| Consumo del token | Después de confirmar la reserva | **Atómico al inicio del gate** (`SET fila:{evento}:usados`) + restauración en `afterCompletion` si hay rollback. |
| UI de fila | Fuera de alcance | Implementada en el SPA: `FilaVirtualPage` y ruta `/eventos/:eventoId/fila` (2026-11-20). |
| Prueba de carga | Sin `.jmx` ni informe | `docs/testing/carga/` + `docs/testing/carga.md`: matriz 25/50/100 normal y con fila, más reintentos post-fix. |
| Estado | `E2-TST-03` pendiente | `E2-TST-03` **hecho**; `E2-DEP-01` **hecho**; `E2-FILA-01` **hecho**. |

La auditoría de §1 y los estados del backlog que cita son **históricos** (2026-11-19). Además, la
corrida real encontró dos defectos ajenos a la fila que ya se corrigieron: emisión de entradas
duplicada por reintentos (`emision_compra`) y timeout de Feign a eventos mapeado a 400 (hoy 503).

---

## 3. `0a` — Base documental de la fila (docs only)

**Objetivo:** cerrar el hueco de "US versionadas" (`specs/consigna.md:94-96,182`, hoy sólo en Trello según `README.md:96`) y corregir el único desajuste de la auditoría.

**Criterios de aceptación**
- Existe `Backend/specs/fila-virtual.md` con: las 5 US `FIL-01…FIL-05`, criterios de aceptación, las 4 decisiones confirmadas, el contrato de los 5 endpoints y la justificación de simplificación Redis-only / fail-open-fail-closed.
- `Backend/specs/backlog.md:83` (`MOD-06`) pasa a decir que sólo falta fila; notificaciones y recomendaciones dejan de figurar como pendientes.
- Cero cambios de código.

**Archivos**
- `Backend/specs/fila-virtual.md` (nuevo).
- `Backend/specs/backlog.md:83`.

**Commit sugerido:** `docs(fila): base de specs y corrección MOD-06 (0a)`

---

## 4. `TST-CARGA-01` — Parte A: escenario de compra normal

**Objetivo:** dejar el entregable obligatorio de `consigna.md:171` funcionando **sin depender de la fila**.

**Criterios de aceptación**
- Plan JMeter versionado que ejecuta el flujo E1 de compra a través del gateway con JWT de CLIENTE: login → reserva en ventas → pago → verificación de emisión **[I]** (mismo recorrido de la colección Postman de E1).
- Parámetros configurables: cantidad de usuarios concurrentes (p. ej. 25 / 50 / 100), ramp-up y duración. Duración **< 10 min** para que no entren expiraciones de reserva (`StockCache` usa TTL 10 min, `backlog.md:236`).
- Evento/sector de prueba **no** `esAltaDemanda`, con cupo suficiente para no agotarlo durante la corrida.
- Informe `Backend/docs/testing/carga.md` con: entorno (`docker compose`, versiones), usuarios concurrentes, ramp-up, duración, p50/p95/p99, throughput (RPS), errores agrupados por código HTTP y resultados de la verificación SQL (§8).
- `Backend/specs/backlog.md:242` pasa a `parcial` con nota: "escenario normal hecho; falta escenario con fila".
- No toca nada de `FIL-*`.

**Archivos**
- `Backend/docs/testing/carga/compra-normal.jmx` (nuevo).
- `Backend/docs/testing/carga.md` (nuevo).
- `Backend/specs/backlog.md:242`.

**Commit sugerido:** `test(carga): escenario compra normal + informe mínimo (E2-TST-03)`

---

## 5. `FIL-01` — Ingreso y posición

**Criterios de aceptación**
- `POST /api/eventos/{id}/fila/ingreso` con JWT asigna posición **en el servidor** (ZSET + contador monotónico), es idempotente por cliente: repetirlo no duplica ni cambia la posición.
- `GET /api/eventos/{id}/fila/posicion` devuelve posición propia y total; `404` si el cliente no ingresó.
- Evento no `PUBLICADO` → `409`.
- Identidad por header `X-User-Id` (patrón `VentaController.java:183-189`). Correlation id en bitácora (`backlog.md:238`).
- **Fail-closed:** cualquier error de Redis → `503`, sin caer a MySQL.

**Archivos**
- `Backend/msvc-ventas/src/main/java/ventas/redis/FilaRedisStore.java` (ZSET `fila:{idEvento}`, contador `fila:{idEvento}:seq`; patrón `StockCache.java:19-33`).
- `Backend/msvc-ventas/src/main/java/ventas/service/FilaService.java` + `FilaServiceImpl.java`.
- `Backend/msvc-ventas/src/main/java/ventas/controller/FilaController.java`.
- DTOs `FilaIngresoResponseDTO`, `FilaPosicionResponseDTO`.
- `Backend/msvc-ventas/src/main/resources/application.yml:37-46`: bloque `ticketya.fila.*` (prefijos, TTLs, tanda por defecto).
- Docs del mismo commit: `Backend/specs/backlog.md:221` → `parcial`; `Backend/msvc-ventas/docs/endpoints.md` sección fila; `Backend/specs/endpoints.md:208-209` marcados como implementados; `Backend/docs/api/gateway.md:39` → "parcial: ingreso y posición".

**Tests**
- `FilaRedisStoreTest` con Mockito sobre `ZSetOperations`/`ValueOperations` (patrón `StockCacheTest.java:22-48`).
- `FilaControllerTest` con MockMvc standalone (patrón `EntradaControllerValidacionTest.java:39`).

**Commit sugerido:** `feat(fila): ingreso y posición con Redis (FIL-01)`

---

## 6. `FIL-02` — Estado público de la fila

**Criterios de aceptación**
- `GET /api/eventos/{id}/fila/estado`, público, devuelve `{totalEnFila, posicionHabilitadaHasta, abierta}` sin exponeridentidades de clientes.
- **Fail-closed** → `503` si Redis no responde.

**Archivos**
- Método nuevo en `FilaService` / `FilaRedisStore`.
- `FilaEstadoResponseDTO`; mapping en `FilaController`.
- Docs: `Backend/specs/endpoints.md:211`; nota en `Backend/specs/backlog.md:221`.

**Tests:** store + controller.

**Commit sugerido:** `feat(fila): estado público de la fila (FIL-02)`

---

## 7. `FIL-03` — Tandas y habilitación (ADMIN)

**Criterios de aceptación**
- `POST /api/eventos/{id}/fila/habilitacion` con `{hastaPosicion}` → **sólo ADMIN**.
- `403` para ORGANIZADOR / CLIENTE / PERSONAL_ACCESO, aunque hoy el gateway deja pasar a ORGANIZADOR por `/api/eventos/**` (`JwtAuthenticationFilter.java:234-237`). Ventas revalida `X-User-Role` (patrón `ReporteController.java:53`; `ventas/security/SecurityConfig.java` está vacío).
- No se puede **reducer** una tanda ya habilitada → `409`.
- Se cierra la decisión 16 de `Backend/specs/reglas-de-negocio.md:265`.
- **Fail-closed** → `503`.

**Archivos**
- Lógica de tanda en `FilaRedisStore` (`fila:{idEvento}:tanda`).
- `FilaHabilitacionRequestDTO`; mapping en `FilaController`.
- `Backend/APIGateway/src/test/.../JwtGatewayIntegrationTest.java` (patrón `:168`): caso ADMIN 200 y ORGANIZADOR 403.
- Test de servicio en ventas.
- Docs: `Backend/specs/reglas-de-negocio.md:42,265`; `Backend/docs/api/gateway.md:74-92`; nota en backlog.

**Commit sugerido:** `feat(fila): habilitación de tandas solo ADMIN (FIL-03)`

---

## 8. `FIL-04` — Token de compra + gate en `crearReserva`

**Criterios de aceptación**
- `POST /api/eventos/{id}/fila/validacion` emite token con TTL **sólo si** `posición <= tanda`. Revalidar devuelve el mismo token vigente; fuera de tanda → `409`; Redis caído → `503`.
- `crearReserva` (`Backend/msvc-ventas/src/main/java/ventas/service/VentaServiceImpl.java:292-368`) exige `tokenFila` válido **antes de `lockCupo`** (`:335`) **sólo cuando** `esAltaDemanda=true`.
  - Sin token / token inválido → `409` y **no se descuenta cupo**.
  - Evento normal → comportamiento idéntico al actual.
  - Token de un solo uso, consumido al reservar **[I]**: consumirlo justo después de confirmar la reserva para no quemarlo si la transacción falla.
- **Fail-open dentro de `crearReserva`**: si Redis falla al validar, `Bitacora.registrar(...)` y continuar. La integridad la sigue garantizando MySQL (`cupo_sector` con `SELECT … FOR UPDATE`, `init.sql:71`, y `uk_asiento_evento_sector_fila_numero`, `init.sql:85`). **Reemplazado:** fail-closed (503) también en el gate; ver §2.1.
- `EventosClient.java:23-30` incorpora `esAltaDemanda` (hoy ausente, aunque `Evento.java:53-54` / `EventoMapper.java:35` lo exponen).
- `ReservaCreateRequestDTO` incorpora `tokenFila` opcional.

**Archivos**
- Store de token en `FilaRedisStore`; `FilaValidacionResponseDTO`.
- `EventosClient.java`, `ReservaCreateRequestDTO`, `VentaServiceImpl.java`.
- `FilaServiceTest` (vigente/expirado/fuera de tanda) + nuevos casos en `VentaServiceImplTest`. Los mocks actuales devuelven `false` por defecto para el flag nuevo → sin regresión **[I]**.
- Docs: `Backend/specs/endpoints.md:210`; `Backend/specs/comunicacion.md:140`; sección nueva en `Backend/specs/diagramas/diagramas-secuencia.md`; justificación de fail-open/closed en `Backend/specs/fila-virtual.md`. `backlog.md:221` sigue `parcial` hasta `FIL-05`.

**Commit sugerido:** `feat(fila): token temporal y gate por alta demanda (FIL-04)`

---

## 9. `TST-CARGA-01` — Parte B: escenario con fila

**Objetivo:** medir el segundo camino del sistema una vez que el gate existe (después de `FIL-04`).

**Criterios de aceptación**
- Thread Group de CLIENTES sobre evento `esAltaDemanda=true`:
  `ingreso → posicion (poll) → validacion/token → crearReserva`.
- **Thread Group aparte con JWT de ADMIN** que llama a `POST /api/eventos/{id}/fila/habilitacion` con `hastaPosicion` **creciente por tandas** (p. ej. +N cada X segundos) mientras los clientes esperan. Sin este hilo la validación se traba y la corrida no avanza. **Reemplazado:** la tanda automática es el modo B1 por defecto; el hilo ADMIN es solo el override B2 (ver `docs/testing/carga/README.md`).
- Clientes con posición fuera de tanda hacen poll hasta obtener token o hasta un timeout configurado; el timeout se contabiliza como error/espera, no como éxito.
- Métricas separadas por escenario: latencia de `ingreso`, tiempo de espera en fila, `validacion`, `crearReserva`, RPS y errores (`409` fuera de tanda, `503` Redis, timeouts).
- Misma verificación SQL de no doble venta que la Parte A (§10), ejecutada al terminar la corrida.
- `Backend/docs/testing/carga.md` queda con dos secciones: "Compra normal" y "Compra con fila".
- `Backend/specs/backlog.md:242` pasa a `hecho`.

**Archivos**
- `Backend/docs/testing/carga/compra-con-fila.jmx` (nuevo).
- `Backend/docs/testing/carga.md` (ampliado).
- `Backend/specs/backlog.md:242`.

**Commit sugerido:** `test(carga): escenario con fila y ADMIN habilitando tandas (E2-TST-03)`

---

## 10. `FIL-05` — Cierre de contrato y consistencia (docs only)

**Criterios de aceptación**
- Todo lo implementado queda reflejado; no queda texto de "pendiente / no implementado" referido a la fila.
- OpenAPI regenerado con los 5 endpoints: `Backend/docs/openapi/msvc-ventas.json` y `Backend/docs/openapi/openapi-bundle.json`; `Backend/docs/openapi/README.md:16,120` corregidos.
- Nueva carpeta en la colección Postman (`Backend/docs/postman/README.md`) con los 5 requests, incluido el de `habilitacion` con JWT de ADMIN.
- `Backend/docs/api/gateway.md:18,39`: de "no implementada" a implementada (con RBAC de `FIL-03`).
- `Backend/specs/diagramas/modelo*.md` y `Backend/docs/modelo-datos.md`: nota explícita de que la fila vive en Redis y **no** entra al ER de MySQL, con la justificación de la decisión 1.
- `Backend/specs/backlog.md`: `:221` → `hecho`; `:117` API-04 → "falta sólo notificaciones"; `:65` DOC-MS-VEN; `:83` MOD-06; `:388` resumen; `:242` ya quedó `hecho` desde la Parte B.
- `Backend/specs/bases-de-datos.md:13` y `Backend/specs/reglas-de-negocio.md:42` actualizados.

**Checklist final rápido:** el de la §11 más `grep -rni 'sin implementar\|no implementada' Backend/docs Backend/specs | grep -i fila` → 0 resultados.

**Commit sugerido:** `docs(fila): cierre de contrato y backlog (FIL-05)`

---

## 11. Verificación concreta de "no doble venta" (CA de `TST-CARGA-01`, ambas partes)

Ejecutar **al terminar cada corrida** contra MySQL del contenedor `ventas`. Tablas y columnas verificadas en `Backend/infrastructure/mysql/ventas/init.sql:8-88`.

**1) Snapshot antes y después del cupo del sector** (se comparan deltas para no confusiones con el seed, que ya trae `cantidad_vendida=1` en algunos sectores, `init.sql:93-119`):

```sql
SELECT id_sector,
       capacidad_total,
       cantidad_disponible,
       cantidad_reservada,
       cantidad_vendida
FROM cupo_sector
WHERE id_evento = :idEvento
  AND id_sector  = :idSector;
```

**CA:**
- En ambos snapshots: `capacidad_total = cantidad_disponible + cantidad_reservada + cantidad_vendida`.
- `cantidad_disponible >= 0`, `cantidad_reservada >= 0`, `cantidad_vendida >= 0`.
- `Δ(cantidad_reservada + cantidad_vendida)` coincide con las reservas/compras exitosas informadas por JMeter (la corrida dura < 10 min para evitar expiraciones).

**2) Conteo de asientos duplicados (sectores numerados):**

```sql
SELECT id_sector, fila, numero, COUNT(*) AS veces
FROM asiento_reservado
WHERE id_evento = :idEvento
GROUP BY id_sector, fila, numero
HAVING COUNT(*) > 1;
```

**CA:** 0 filas. Respaldo estructural: `uk_asiento_evento_sector_fila_numero` (`init.sql:85`).

**3) Cruce opcional contador ↔ asientos tomados:**

```sql
SELECT c.id_sector,
       c.cantidad_vendida + c.cantidad_reservada AS ocupadas_contador,
       (SELECT COUNT(*) FROM asiento_reservado a
         WHERE a.id_evento = c.id_evento AND a.id_sector = c.id_sector) AS ocupadas_asientos
FROM cupo_sector c
WHERE c.id_evento = :idEvento AND c.id_sector = :idSector;
```

**CA:** `ocupadas_contador = ocupadas_asientos` en sectores numerados. Para sectores sin asientos, el cruce se hace contra `detalle_reserva` + `compra.estado='APROBADA'` (`VentaServiceImpl.java:74`, `ReporteServiceImpl.java:38`).

> Nota: estos chequeos son complementarios al IT existente (`docs/testing/concurrencia.md`, `AsientoReservadoConcurrenciaIT`); no lo reemplazan.

---

## 12. Checklist corto después de **cada** commit (no re-auditar todo)

```bash
grep -rn '/fila' Backend/msvc-ventas/docs Backend/docs/openapi
grep -n 'E2-FILA-01\|E2-TST-03' Backend/specs/backlog.md
grep -rni 'sin implementar\|no implementada' Backend/docs Backend/specs | grep -i fila
mvn -q -f Backend/msvc-ventas/pom.xml test          # [NV] ejecutar recién en la implementación
mvn -q -f Backend/APIGateway/pom.xml test           # [NV] ejecutar recién en la implementación
git status --short
```

Cada commit deja el repo compilable y con tests verdes. El riesgo concentrado es `FIL-04` (toca el flujo E1); por eso `TST-CARGA-01/Parte A` se hace antes, como línea base de comparación.

---

## 13. No verificado / supuestos

> **Actualización post-implementación (2026-11-21):** Maven, Docker Compose, JMeter y Newman se
> ejecutaron; los `.jmx` y el informe están en `docs/testing/carga/` y `docs/testing/carga.md`.
> Lo que sigue es el registro de supuestos del plan, no el estado actual.

- **[NV]** No ejecuté Maven, Docker Compose ni JMeter durante el plan.
- **[NV]** No existe hoy informe ni `.jmx` de carga versionado (búsqueda en `Backend/docs/`).
- **[NV]** No comprobé si el tablero Trello tiene una US de fila con CA propios que debamos importar en vez de escribir `specs/fila-virtual.md` (`README.md:96`).
- **[NV]** No verifiqué que JMeter esté instalado en el entorno; la consigna dice "por ejemplo, con JMeter", así que se puede sustituir por otra herramienta si hace falta, manteniendo el mismo informe.
- **[I]** Formato exacto de body/response de los 5 endpoints: `endpoints.md:206-212` sólo da la firma de una línea. Se define durante `FIL-01…FIL-04` y se versiona en `FIL-05`.
- **[I]** Los mocks de `VentaServiceImplTest` devolverán `false` para `esAltaDemanda` → sin regresión en el flujo normal.
- **[NV]** La numeración vieja que se descartó no debe quedar en ningún doc; `0a` la fija por escrito.
