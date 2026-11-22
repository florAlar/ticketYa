# Especificación operativa paso a paso — Fila virtual + prueba de carga

> **HISTÓRICO (2026-11-19).** Runbook del plan previo, no del estado actual. La especificación
> vigente es [`specs/fila-virtual.md`](../../specs/fila-virtual.md) y los resultados de carga están
> en [`docs/testing/carga.md`](../../docs/testing/carga.md). En particular, el gate final es
> **fail-closed** (503) y las tandas son **automáticas** con override ADMIN.

> Este documento es el **runbook** del plan. El documento `fila-virtual-y-carga.md` es el resumen/decisiones; acá está el desglose ejecutable.
> Modo plan: **no se ejecutó nada**. Los comandos y snippets son la especificación para la implementación.
> Rutas relativas a `ticketYa/`. Convención **[V]** verificado · **[I]** inferido · **[NV]** no verificado.

---

## Cómo leer cada paso

Cada paso tiene: **Entrada** → **Acciones** → **Archivos** → **Verificación** → **Salida**. Un bloque = un commit. No se pasa al siguiente bloque hasta que la verificación del anterior esté limpia.

---

## FASE 0 — Precondición (una sola vez)

### PASO 0.1 — Árbol limpio
- **Entrada**: repo en `ticketYa/`.
- **Acciones**: ejecutar `git status --short`.
- **Verificación**: salida vacía.
- **Salida**: punto de partida para el primer commit.

### PASO 0.2 — No repetir auditoría
- **Entrada**: tabla del §1 de `fila-virtual-y-carga.md`.
- **Acciones**: usar esa tabla como único chequeo backlog ↔ código. A partir de acá, cada US actualiza sus propios docs.
- **Salida**: `MOD-06` es el único desajuste dentro del alcance; se corrige en `0a`.

---

## BLOQUE `0a` — Base documental (commit 1)

### PASO A.1 — Crear `Backend/specs/fila-virtual.md`
- **Entrada**: 4 decisiones confirmadas + 5 endpoints de `Backend/specs/endpoints.md:206-212`.
- **Acciones**: crear el archivo con este índice exacto:
  1. `Alcance y decisión de simplificación` → citar `specs/consigna.md:148`.
  2. `Decisiones confirmadas` → Redis-only; gate por `esAltaDemanda`; fail-closed/fail-open; sin UI.
  3. `Modelo de datos en Redis` → claves `fila:{idEvento}`, `fila:{idEvento}:seq`, `fila:{idEvento}:tanda`, `fila:{idEvento}:token:{userId}`; TTLs; sin MySQL/Rabbit.
  4. `Contrato de los 5 endpoints` → los de `endpoints.md:206-212`, con request/response definidos en los pasos C..F.
  5. `US FIL-01…FIL-05` → criterios de aceptación copiados de este runbook.
  6. `Riesgos y degradación` → justificación de fail-open dentro de `crearReserva` (MySQL conserva lock y unique key, `init.sql:71,85`).
- **Archivos**: `Backend/specs/fila-virtual.md` (nuevo).
- **Verificación**: `grep -n 'FIL-0[1-5]' Backend/specs/fila-virtual.md` → 5 coincidencias.
- **Salida**: US versionadas en el repo, independientes de Trello (`README.md:96`).

### PASO A.2 — Corregir `MOD-06`
- **Entrada**: `Backend/specs/backlog.md:83` dice "Falta cuando entren fila, notificaciones y recomendaciones".
- **Acciones**: reemplazar ese texto por: `Falta sólo la fila (FIL-01…FIL-05); notificaciones y recomendaciones ya documentadas en los diagramas.` El resto de la fila (21 tablas verificadas, fecha 2026-11-11) se conserva.
- **Archivos**: `Backend/specs/backlog.md:83`.
- **Verificación**: `grep -n 'MOD-06' Backend/specs/backlog.md` → ya no menciona notificaciones ni recomendaciones como pendientes.
- **Salida**: consistencia con `specs/diagramas/modelo.md:176,208` y `modelo-por-microservicio.md:240-282`.

### PASO A.3 — Checklist corto
- **Acciones**: `git diff --check` y `git status --short`.
- **Salida**: sólo los 2 archivos del bloque.

### PASO A.4 — Commit
- **Mensaje**: `docs(fila): base de specs y corrección MOD-06 (0a)`

---

## BLOQUE `TST-CARGA-01` / PARTE A — Compra normal (commit 2)

> Se hace **antes** de la fila. No depende de `FIL-*`. Es el entregable exigido por `consigna.md:171`.

### PASO T-A.1 — Levantar entorno
- **Acciones**: `cd Backend && docker compose up -d --build` **[NV]** confirmar nombre de servicios.
- **Verificación**: `curl -sf http://localhost:8080/actuator/health` (o el smoke del gateway) → 200.
- **Salida**: stack listo.

### PASO T-A.2 — Elegir datos y tomar baseline
- **Acciones**:
  1. Elegir evento `esAltaDemanda=false` con sector con cupo disponible (evitar los seeds agotados de `init.sql:114-115`).
  2. Guardar snapshot inicial:
     ```sql
     SELECT id_sector, capacidad_total, cantidad_disponible,
            cantidad_reservada, cantidad_vendida
     FROM cupo_sector
     WHERE id_evento = :idEvento AND id_sector = :idSector;
     ```
- **Comando** **[I]**: `docker compose exec -T mysql mysql -uticketya -pticketya ventas -e "<SQL>"`.
- **Salida**: baseline guardado en `Backend/docs/testing/carga.md` (tabla "Antes").

### PASO T-A.3 — Escribir el plan JMeter
- **Archivos**: `Backend/docs/testing/carga/compra-normal.jmx` (nuevo).
- **Acciones**: armar con:
  - `Thread Group` con variables `-Jusuarios`, `-Jramp`, `-Jduracion`, `-JidEvento`, `-JidSector`.
  - `HTTP Request Defaults` → `http://localhost:8080` (gateway).
  - `HTTP Header Manager` → `Content-Type: application/json`, `X-Correlation-Id: carga-normal-${__threadNum}`.
  - Secuencia: `POST /api/usuarios/login` → guardar JWT → `POST /api/ventas/reservas` (`ReservaCreateRequestDTO` de 2 componentes, `VentaController.java:183-191`) → pago → verificación de emisión **[I]**, mismo recorrido que la colección Postman E1.
  - `JSON Extractor` para `idReserva` / token JWT; `Response Assertion` para 200/201.
  - `Constant Timer` corto para no golpear el login en ráfaga.
- **Verificación**: `mvn` no aplica; abrir el `.jmx` en JMeter GUI una vez y correr con 1 usuario.
- **Salida**: plan parametrizable.

### PASO T-A.4 — Ejecutar la corrida
- **Acciones**:
  ```bash
  jmeter -n -t Backend/docs/testing/carga/compra-normal.jmx \
    -JidEvento=<uuid> -JidSector=<uuid> \
    -Jusuarios=50 -Jramp=30 -Jduracion=300 \
    -l Backend/docs/testing/carga/resultados-normal.jtl \
    -e -o Backend/docs/testing/carga/report-normal
  ```
- **Verificación**: el `.jtl` existe y el `report-normal/index.html` se generó.
- **Nota**: no versionar `.jtl` ni `report-*`; si hace falta, agregarlos a `.gitignore` en este mismo commit. Sí versionar `.jmx` e `informe.md`.
- **Salida**: resultados crudos + reporte HTML.

### PASO T-A.5 — Extraer métricas
- **Acciones**: del `report-normal` sacar p50/p95/p99, RPS, errores por código HTTP, y cantidad de transacciones OK/KO. Volcarlos al informe.
- **Archivos**: `Backend/docs/testing/carga.md`.

### PASO T-A.6 — Verificar no doble venta
- **Acciones**:
  1. Repetir el SQL del PASO T-A.2 y comparar contra el baseline.
  2. Ejecutar:
     ```sql
     SELECT id_sector, fila, numero, COUNT(*) AS veces
     FROM asiento_reservado
     WHERE id_evento = :idEvento
     GROUP BY id_sector, fila, numero
     HAVING COUNT(*) > 1;
     ```
- **Verificación (CA)**:
  - `capacidad_total = cantidad_disponible + cantidad_reservada + cantidad_vendida` en ambos snapshots.
  - Ningún contador negativo.
  - `Δ(reservada + vendida)` = operaciones exitosas de JMeter (duración < 10 min evita expiraciones, `backlog.md:236`).
  - La query de duplicados devuelve **0 filas** (respaldo: `uk_asiento_evento_sector_fila_numero`, `init.sql:85`).
- **Salida**: sección "Verificación de integridad" del informe.

### PASO T-A.7 — Redactar informe
- **Archivos**: `Backend/docs/testing/carga.md`.
- **Acciones**: dejar este índice: `Entorno`, `Parámetros`, `Resultados (normal)`, `Verificación de integridad`, `Conclusión`. La sección de fila se agrega en T-B.6.

### PASO T-A.8 — Actualizar backlog
- **Acciones**: `Backend/specs/backlog.md:242` → `parcial` con nota: `Escenario normal ejecutado el <fecha>; informe en docs/testing/carga.md. Falta escenario con fila (TST-CARGA-01 Parte B).`

### PASO T-A.9 — Checklist y commit
- **Acciones**: `git diff --check`, `git status --short`.
- **Mensaje**: `test(carga): escenario compra normal + informe mínimo (E2-TST-03)`

---

## BLOQUE `FIL-01` — Ingreso y posición (commit 3)

### PASO C.1 — Configuración
- **Archivos**: `Backend/msvc-ventas/src/main/resources/application.yml`, después del bloque `ventas:` (`:37-46`).
- **Acciones**: agregar:
  ```yaml
    fila:
      key-prefix: ${TICKETYA_FILA_PREFIX:fila}
      position-ttl-hours: ${TICKETYA_FILA_POSITION_TTL_HOURS:24}
      token-ttl-minutes: ${TICKETYA_FILA_TOKEN_TTL_MINUTES:10}
      default-tanda: ${TICKETYA_FILA_DEFAULT_TANDA:100}
  ```

### PASO C.2 — Contrato de claves Redis
- **Archivos**: `Backend/msvc-ventas/src/main/java/ventas/redis/FilaRedisStore.java` (nuevo).
- **Acciones**: definir:
  | Clave | Tipo | Contenido |
  |---|---|---|
  | `fila:{idEvento}` | ZSET | member = `X-User-Id`, score = posición |
  | `fila:{idEvento}:seq` | String | contador `INCR` |
  | `fila:{idEvento}:tanda` | String | última posición habilitada |
  | `fila:{idEvento}:token:{userId}` | String | token con TTL |
- Usar `StringRedisTemplate` (`RedisConfig.java:12-15`). TTL en segundos = `position-ttl-hours * 3600`.

### PASO C.3 — Ingreso atómico
- **Acciones**: script Lua ejecutado con `redisTemplate.execute(script, keys, args)`:
  ```lua
  local pos = redis.call('ZSCORE', KEYS[1], ARGV[1])
  if pos then return pos end
  local n = redis.call('INCR', KEYS[2])
  redis.call('ZADD', KEYS[1], n, ARGV[1])
  redis.call('EXPIRE', KEYS[1], ARGV[2])
  redis.call('EXPIRE', KEYS[2], ARGV[2])
  return n
  ```
  `KEYS = [fila:{idEvento}, fila:{idEvento}:seq]`, `ARGV = [userId, ttlSegundos]`.
- **CA**: repetir ingreso devuelve la misma posición; dos hilos concurrentes del mismo usuario no duplican.

### PASO C.4 — Posición y fail-closed
- **Acciones**:
  - `posicion`: `ZSCORE` (si `null` → 404) + `ZCARD` para el total.
  - En **todos** los métodos del store, envolver `DataAccessException` en `ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FILA_REDIS_UNAVAILABLE")`.
  - Registrar el evento en bitácora con `X-Correlation-Id` (patrón `E2-NFR-03`, `backlog.md:238`).
- **CA**: Redis caído → `503`, nunca fallback a MySQL.

### PASO C.5 — Servicio y controller
- **Archivos**: `FilaService.java`, `FilaServiceImpl.java`, `FilaController.java`, `FilaIngresoResponseDTO.java`, `FilaPosicionResponseDTO.java`.
- **Acciones**:
  - `FilaServiceImpl.ingresar(idEvento, userId)`: `EventosClient.getEvento(idEvento)` (`EventosClient.java:18-19`); si `estado != "PUBLICADO"` (`EventoServiceImpl.java:76`) → 409; luego store.
  - `FilaController` con `@RequestMapping("/eventos/{idEvento}/fila")`, `@PostMapping("/ingreso")` y `@GetMapping("/posicion")`, leyendo `@RequestHeader("X-User-Id")` (patrón `VentaController.java:183-189`).

### PASO C.6 — Tests
- **Archivos**: `FilaRedisStoreTest.java`, `FilaControllerTest.java`.
- **Acciones**: Mockito sobre `ZSetOperations`/`ValueOperations` (patrón `StockCacheTest.java:22-48`); MockMvc standalone (patrón `EntradaControllerValidacionTest.java:39`). Casos: ingreso nuevo, reingreso idempotente, posición 404, evento no PUBLICADO 409, Redis down 503.

### PASO C.7 — Docs y commit
- **Acciones**:
  - `Backend/specs/backlog.md:221` → `parcial`.
  - `Backend/msvc-ventas/docs/endpoints.md`: sección "Fila virtual" con ingreso y posición.
  - `Backend/specs/endpoints.md:208-209`: marcar implementados.
  - `Backend/docs/api/gateway.md:39`: `parcial: ingreso y posición implementados`.
- **Mensaje**: `feat(fila): ingreso y posición con Redis (FIL-01)`

---

## BLOQUE `FIL-02` — Estado público (commit 4)

### PASO D.1 — Store
- **Acciones**: método `estado(idEvento)` → `ZCARD` (`totalEnFila`), `GET fila:{idEvento}:tanda` (`posicionHabilitadaHasta`, 0 si no existe), `abierta` = flag de configuración/evento PUBLICADO. Fail-closed igual que C.4.

### PASO D.2 — Controller y DTO
- **Archivos**: `FilaEstadoResponseDTO.java`, mapping `@GetMapping("/estado")` en `FilaController`.
- **CA**: endpoint público (sin `X-User-Id` obligatorio); response sin identifiers de clientes.

### PASO D.3 — Tests y docs
- **Acciones**: test de store + controller; `Backend/specs/endpoints.md:211` marcado; nota en `backlog.md:221`.

### PASO D.4 — Commit
- **Mensaje**: `feat(fila): estado público de la fila (FIL-02)`

---

## BLOQUE `FIL-03` — Tandas y habilitación ADMIN (commit 5)

### PASO E.1 — CAS de tanda
- **Archivos**: `FilaRedisStore.java`.
- **Acciones**: script Lua:
  ```lua
  local actual = tonumber(redis.call('GET', KEYS[1]) or '0')
  local pedido = tonumber(ARGV[1])
  if pedido <= actual then return 0 end
  redis.call('SET', KEYS[1], pedido)
  redis.call('EXPIRE', KEYS[1], ARGV[2])
  return 1
  ```
  `KEYS=[fila:{idEvento}:tanda]`, `ARGV=[hastaPosicion, ttlSegundos]`. Retorno `0` → 409 (no se puede reducir). Fail-closed.

### PASO E.2 — DTO y mapping
- **Archivos**: `FilaHabilitacionRequestDTO.java` con `@Min(1) Integer hastaPosicion`.
- **Acciones**: `@PostMapping("/habilitacion")` leyendo `@RequestHeader("X-User-Role")`.

### PASO E.3 — RBAC en ventas
- **Acciones**: si `role != "ADMIN"` → 403. Aunque `JwtAuthenticationFilter.java:234-237` deja pasar ORGANIZADOR por `/api/eventos/**`, ventas revalida (patrón `ReporteController.java:53`; `ventas/security/SecurityConfig.java` vacío).

### PASO E.4 — Tests
- **Archivos**: `JwtGatewayIntegrationTest.java` (patrón `:168`) + test de servicio.
- **CA**: ADMIN → 200; ORGANIZADOR / CLIENTE / PERSONAL_ACCESO → 403; reducir tanda → 409; Redis down → 503.

### PASO E.5 — Docs y commit
- **Acciones**: `Backend/specs/reglas-de-negocio.md:42,265` (cerrar decisión 16), `Backend/docs/api/gateway.md:74-92` (RBAC), nota en backlog.
- **Mensaje**: `feat(fila): habilitación de tandas solo ADMIN (FIL-03)`

---

## BLOQUE `FIL-04` — Token + gate (commit 6) ← riesgo concentrado

### PASO F.1 — Flag `esAltaDemanda` en Feign
- **Archivos**: `Backend/msvc-ventas/src/main/java/ventas/clients/EventosClient.java`.
- **Acciones**: agregar `private Boolean esAltaDemanda;` a `EventoDetalleFeignDTO` (`:23-30`). Lombok `Getter/Setter` ya está. Origen: `Evento.java:53-54`, `EventoMapper.java:35`.

### PASO F.2 — DTO de reserva compatible
- **Archivos**: `Backend/msvc-ventas/src/main/java/ventas/dto/ReservaCreateRequestDTO.java`.
- **Acciones**: agregar tercer componente `String tokenFila` y **conservar el constructor de 2 argumentos** para no romper llamadas existentes:
  ```java
  public ReservaCreateRequestDTO(String idEvento, List<Item> items) {
      this(idEvento, items, null);
  }
  ```

### PASO F.3 — Token get-or-create
- **Archivos**: `FilaRedisStore.java`.
- **Acciones**: Lua:
  ```lua
  local t = redis.call('GET', KEYS[1])
  if t then return t end
  redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])
  return ARGV[1]
  ```
  `KEYS=[fila:{idEvento}:token:{userId}]`, `ARGV=[UUID.randomUUID().toString(), token-ttl-minutes*60]`.
- **CA**: posición `> tanda` → 409; revalidar devuelve el mismo token; expirado genera uno nuevo si aún está habilitado; Redis down → 503.

### PASO F.4 — Consumo de token
- **Acciones**: Lua compare-and-del, para no borrar un token más nuevo:
  ```lua
  if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end
  return 0
  ```
  Llamar **después** de confirmar la reserva **[I]** para no quemarlo si la transacción falla.

### PASO F.5 — Gate en `crearReserva`
- **Archivos**: `Backend/msvc-ventas/src/main/java/ventas/service/VentaServiceImpl.java:292-368`.
- **Acciones**:
  1. Obtener `EventoDetalleFeignDTO` vía `EventosClient`.
  2. **Antes de `lockCupo` (`:335`)**, si `Boolean.TRUE.equals(dto.getEsAltaDemanda())`: exigir `body.tokenFila()` no vacío y válido para el `userId`; si no → 409 sin tocar cupo.
  3. Si Redis falla en esa validación: `Bitacora.registrar("FILA_REDIS_FAILOPEN", idEvento, idReserva, "token=<...>")` y continuar (**fail-open** decided). **Reemplazado:** la implementación final es **fail-closed** (503 `FILA_NO_DISPONIBLE`); ver D3 de `specs/fila-virtual.md`.
  4. Evento normal (`false`/`null`) → flujo sin cambios.
  5. Tras éxito, consumir token (F.4).

### PASO F.6 — Tests
- **Archivos**: `FilaServiceTest.java` (nuevo), `VentaServiceImplTest.java`, `ReservaAsientoConcurrenciaTest.java` (sólo si aplica).
- **CA**: token vigente ✓; expirado/fuera de tanda → 409; sin token en alta demanda → 409 y `cantidad_disponible` sin cambio; Redis down en alta demanda → **503 (fail-closed, reemplaza el “reserva igual OK” del plan)**; evento normal sin token → OK. **[I]** Los mocks actuales devuelven `false` por defecto → sin regresión.

### PASO F.7 — Docs del cambio
- **Acciones**: `Backend/specs/endpoints.md:210`; `Backend/specs/comunicacion.md:140`; sección nueva en `Backend/specs/diagramas/diagramas-secuencia.md`; `Backend/specs/fila-virtual.md` §6 con el detalle fail-closed vs fail-open.

### PASO F.8 — Verificación y commit
- **Acciones**: corrida completa de tests de ventas + gateway.
- **Mensaje**: `feat(fila): token temporal y gate por alta demanda (FIL-04)`

---

## BLOQUE `TST-CARGA-01` / PARTE B — Escenario con fila (commit 7)

> Recién acá existe el gate. El hilo ADMIN es obligatorio o la corrida se traba en `validacion`.

### PASO T-B.1 — Preparar evento de alta demanda
- **Acciones**: usar evento con `esAltaDemanda=true` en estado PUBLICADO; tomar baseline SQL igual que T-A.2. Confirmar que `EventosClient` ya trae el flag (F.1).

### PASO T-B.2 — Thread Group CLIENTES
- **Archivos**: `Backend/docs/testing/carga/compra-con-fila.jmx` (nuevo).
- **Acciones**: secuencia por hilo:
  1. `POST /api/eventos/{id}/fila/ingreso` (JWT cliente) → guardar posición.
  2. `GET /api/eventos/{id}/fila/posicion` en poll (intervalo configurable, timeout configurable).
  3. `POST /api/eventos/{id}/fila/validacion` en poll hasta 200 con token, o hasta timeout → contar como espera fallida.
  4. `POST /api/ventas/reservas` con `tokenFila` del paso anterior.
  5. Continuar pago/verificación como en T-A.3.
- **Headers**: `X-User-Id` y JWT del cliente; `X-Correlation-Id: carga-fila-${__threadNum}`.

### PASO T-B.3 — Thread Group ADMIN (el paso que faltaba)
- **Acciones**: hilo aparte con **JWT de ADMIN**:
  1. Obtener token ADMIN vía login.
  2. Bucle: `POST /api/eventos/{id}/fila/habilitacion` con `{"hastaPosicion": N}`, incrementando `N` por tandas (p. ej. +25 cada 10 s) usando `Counter` / `__counter` de JMeter.
  3. **Sincronización**: los clientes hacen poll en `validacion`; ADMIN corre en paralelo desde el inicio. Si los clientes agotan timeout antes que ADMIN, se registra como error, no como éxito.
- **CA**: ADMIN → 200; ORGANIZADOR → 403 (ya cubierto en FIL-03, acá sólo se usa ADMIN).

### PASO T-B.4 — Ejecutar
- **Acciones**:
  ```bash
  jmeter -n -t Backend/docs/testing/carga/compra-con-fila.jmx \
    -JidEvento=<uuid> -JidSector=<uuid> \
    -Jusuarios=50 -Jramp=30 -Jduracion=300 \
    -JtamanioTanda=25 -JintervaloTanda=10000 \
    -l Backend/docs/testing/carga/resultados-fila.jtl \
    -e -o Backend/docs/testing/carga/report-fila
  ```

### PASO T-B.5 — Métricas específicas de fila
- **Acciones**: medir por separado latencia de `ingreso`, tiempo de espera hasta token, `validacion`, `crearReserva`, RPS, y errores: `409` fuera de tanda, `503` Redis, timeouts de poll.

### PASO T-B.6 — Integridad + informe
- **Acciones**: repetir T-A.6 sobre el evento de alta demanda y agregar a `Backend/docs/testing/carga.md` la sección `Resultados (con fila)`. Comparar contra la línea base normal de T-A.5 para cuantificar el overhead del gate.

### PASO T-B.7 — Backlog y commit
- **Acciones**: `Backend/specs/backlog.md:242` → `hecho`.
- **Mensaje**: `test(carga): escenario con fila y ADMIN habilitando tandas (E2-TST-03)`

---

## BLOQUE `FIL-05` — Cierre de contrato (commit 8)

### PASO H.1 — OpenAPI
- **Acciones**: regenerar `Backend/docs/openapi/msvc-ventas.json` y `Backend/docs/openapi/openapi-bundle.json` con los 5 endpoints; corregir `Backend/docs/openapi/README.md:16,120`.

### PASO H.2 — Postman
- **Acciones**: nueva carpeta en `Backend/docs/postman/README.md` con los 5 requests; el de `habilitacion` usa JWT de ADMIN y body `{"hastaPosicion": N}`.

### PASO H.3 — Gateway y estado
- **Acciones**: `Backend/docs/api/gateway.md:18,39` → implementada; RBAC ya actualizado en E.5.

### PASO H.4 — Modelo de datos
- **Acciones**: `Backend/specs/diagramas/modelo*.md` y `Backend/docs/modelo-datos.md`: nota "fila/tanda/token en Redis, no en ER MySQL" + justificación (decisión 1). `Backend/specs/bases-de-datos.md:13` actualizado.

### PASO H.5 — Backlog final
- **Acciones**: `Backend/specs/backlog.md`: `:221` → `hecho`; `:117` → "falta sólo notificaciones"; `:65` DOC-MS-VEN; `:83` MOD-06; `:388` resumen; `:242` ya `hecho`; `Backend/specs/reglas-de-negocio.md:42`.

### PASO H.6 — Checklist final y commit
- **Acciones**:
  ```bash
  grep -rn '/fila' Backend/msvc-ventas/docs Backend/docs/openapi
  grep -n 'E2-FILA-01\|E2-TST-03' Backend/specs/backlog.md
  grep -rni 'sin implementar\|no implementada' Backend/docs Backend/specs | grep -i fila   # debe dar 0
  git diff --check && git status --short
  ```
- **Mensaje**: `docs(fila): cierre de contrato y backlog (FIL-05)`

---

## Matriz de trazabilidad

| Commit | US | Código | Tests | Docs que se tocan |
|---|---|---|---|---|
| 1 | `0a` | — | — | `specs/fila-virtual.md`, `backlog.md:83` |
| 2 | `TST-CARGA-01/A` | — | JMeter + SQL | `docs/testing/carga/*`, `backlog.md:242` (parcial) |
| 3 | `FIL-01` | store/service/controller + config | `FilaRedisStoreTest`, `FilaControllerTest` | `endpoints.md:208-209`, `gateway.md:39`, `backlog.md:221` |
| 4 | `FIL-02` | estado | test store/controller | `endpoints.md:211` |
| 5 | `FIL-03` | tanda + RBAC | `JwtGatewayIntegrationTest`, service | `reglas-de-negocio.md:42,265`, `gateway.md:74-92` |
| 6 | `FIL-04` | Feign + DTO + gate + token | `FilaServiceTest`, `VentaServiceImplTest` | `endpoints.md:210`, `comunicacion.md:140`, diagrama secuencia |
| 7 | `TST-CARGA-01/B` | — | JMeter fila + ADMIN + SQL | `docs/testing/carga/*`, `backlog.md:242` (hecho) |
| 8 | `FIL-05` | — | checklist | OpenAPI, Postman, gateway, modelos, backlog |

---

## Supuestos y no verificados

- **[NV]** No ejecuté Maven, Docker ni JMeter; ningún comando de este runbook fue probado.
- **[NV]** Nombre real del servicio MySQL en Compose para el `docker compose exec`; ajustar si no es `mysql`.
- **[I]** Secuencia exacta de login/pago del flujo E1 en JMeter: copiarla de la colección Postman existente.
- **[I]** Valor `"PUBLICADO"` confirmado en `EventoServiceImpl.java:76`; estados de reserva `PENDIENTE|CONFIRMADA|CANCELADA|EXPIRADA` en `VentaServiceImpl.java:67-70`; compra `APROBADA` en `:74`.
- **[NV]** Trello puede tener una US de fila con CA propios (`README.md:96`); `0a` versiona la nuestra de todos modos.
