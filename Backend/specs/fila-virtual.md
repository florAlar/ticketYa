# Especificación final — Fila virtual (E2-FILA-01) y prueba de carga (E2-TST-03)

| | |
|---|---|
| **Estado** | Decisiones D1–D11 y puntos abiertos 1–7 y 11 aprobados el 2026-11-19. **Implementado:** FIL-01…FIL-03 en backend y FIL-04 de cierre documental (2026-11-19), más la pantalla de fila en el SPA (2026-11-20). La §2 conserva el estado de partida como registro histórico. `E2-TST-03` **hecho**: matriz 25/50/100 (normal y con fila) y reintentos post-fix; resultados en `docs/testing/carga.md`. |
| **Fecha** | 2026-11-19 (actualizado 2026-11-22) |
| **Destino en el repo** | `Backend/specs/fila-virtual.md` (commit `0a`) |
| **Servicio principal** | `msvc-ventas` (+ un ajuste en `APIGateway`) |
| **Backlog** | E2-FILA-01 y E2-TST-03 (`specs/backlog.md`) |
| **Convención** | **[V]** verificado en el análisis del repo · **[I]** inferido · **[NV]** no verificado |

> Las referencias `archivo:línea` salen de los análisis previos del repo. **Revalidarlas antes de mergear**, porque cambian con cada commit. Todas las rutas son relativas a `ticketYa/`.

---

## 1. Objetivo y alcance

Para los eventos de alta demanda, los clientes ingresan a una cola que:

1. asigna una **posición por orden de llegada**,
2. permite **consultarla**,
3. **habilita la compra por tandas** y
4. **condiciona la creación de la reserva** a haber sido habilitado.

El objetivo es controlar la avalancha sobre `POST /reservas` sin tocar las garantías anti doble venta, que siguen en MySQL (`SELECT … FOR UPDATE` sobre `cupo_sector` y `uk_asiento_evento_sector_fila_numero`, `infrastructure/mysql/ventas/init.sql`).

**Incluye**

- Los 5 endpoints diseñados en `specs/endpoints.md`.
- Almacenamiento íntegramente en Redis.
- Apertura automática de tandas, con override manual de ADMIN.
- Gate en `crearReserva` para eventos con `esAltaDemanda = true`.
- Pantalla de fila en el SPA: estado público, turno propio con polling, validación del token y override ADMIN (2026-11-20).
- Tests unitarios, de integración con Redis real y de concurrencia.
- Prueba de carga con informe (E2-TST-03) — **hecho**: matriz 25/50/100 normal y con fila, más los reintentos post-fix; ver `docs/testing/carga.md`.

**No incluye**

- Tablas MySQL de fila ni mensajería (RabbitMQ). `VentaQueueProducer` y `VentaQueueConsumer` siguen vacíos.
- Expiración de posiciones por inactividad.
- Devolución del turno cuando la reserva vence o el pago se rechaza (ver D6).
- Notificaciones push fuera de la pestaña: el front refresca la posición por polling cada 5 s.

## 2. Estado de partida (chequeo único backlog ↔ código)

Esta auditoría **no se repite**. Desde acá, cada US actualiza sus propios docs.

> **Registro histórico (2026-11-19).** La tabla refleja el estado previo a la implementación. Hoy `E2-FILA-01` (FIL-01…FIL-04), `API-04` (fila en OpenAPI), `DOC-MS-VEN` (sección `/fila`), el contrato de `specs/endpoints.md` y las reglas de `specs/reglas-de-negocio.md` están actualizados. `E2-TST-03` también quedó **hecho** con la matriz 25/50/100 y los reintentos post-fix.
>
> **Estado actual (2026-11-22):** los “parcial”, “pendiente” y “en curso” que aparecen en esta tabla son del **snapshot del 2026-11-19**; no describen el estado vigente. La fila está implementada y `E2-FILA-01`/`E2-FILA-02` figuran como `hecho` en el backlog.

| Entrada | Backlog / doc | Código real | Resultado |
|---|---|---|---|
| `E2-FILA-01` | `specs/backlog.md` pendiente, "simplificable con justificación" | Sin controller, service ni DTO de fila en ventas. El gateway **sí** rutea (`APIGateway/src/main/resources/application.yml`) | **[V]** coincide |
| `API-04` | `backlog.md` parcial, "falta fila y notificaciones" | Sin `/fila` en `docs/openapi/*.json` | **[V]** coincide |
| `DOC-MS-VEN` | `backlog.md` parcial | `msvc-ventas/docs/endpoints.md` sin sección `/fila` | **[V]** coincide |
| `MOD-06` | `backlog.md` "falta cuando entren fila, notificaciones y recomendaciones" | Notificaciones y recomendaciones **ya están** en `specs/diagramas/modelo.md` y `modelo-por-microservicio.md` | **[V]** NO coincide: solo falta la fila |
| Contrato | `specs/endpoints.md` | No implementado | **[V]** coincide como diseño |
| Docs de estado | `docs/api/gateway.md` y `docs/openapi/README.md` dicen "no implementada" | Sin implementación | **[V]** coincide |
| Comunicación / BD | `specs/comunicacion.md`, `specs/bases-de-datos.md` | Redis disponible (`RedisConfig.java`), sin tabla de fila en `init.sql` | **[V]** coincide |
| Reglas de negocio | `specs/reglas-de-negocio.md` "pendiente E2"; decisión 16 abierta en `reglas-de-negocio.md` §9 | Sin implementación | **[V]** coincide |
| `E2-TST-03` | `backlog.md` pendiente; la consigna lo exige (`consigna.md`) | Sin informe ni `.jmx` versionado | **[V]** entregable en cero *(hoy **hecho**: `docs/testing/carga.md`)* |

**Estado de las inconsistencias detectadas (actualizado 2026-11-20):** ya resueltas — `docs/openapi/README.md` documenta fila y reportes; `specs/microservicios.md` y `specs/arquitectura.md` reflejan el estado implementado; `specs/documentacion-y-validacion.md` y `specs/reglas-de-negocio.md` ya no marcan fila, preferencias ni notificaciones como pendientes. Queda como registro histórico el resumen de `backlog.md`, que conserva filas anteriores a E2-PRE/E2-NOT; los READMEs ya reflejan los gates activos.

## 3. Decisiones de diseño

Marcar cada casilla cuando el equipo esté de acuerdo.

- [x] **D1. Redis-only.** Fila, posiciones, tanda y token viven en Redis (`StringRedisTemplate`, `RedisConfig.java`). Sin tablas MySQL nuevas ni RabbitMQ. Justificación formal en `consigna.md`.
- [x] **D2. Gate solo si `esAltaDemanda = true`.** Protege el flujo E1 y los 229 requests de Postman. El flag existe en `Evento.java` y `EventoMapper.java`, pero no llega a ventas: se agrega a `EventosClient.EventoDetalleFeignDTO`.
- [x] **D3. Fail-closed en toda la fila, incluido el gate.** Si Redis falla: **503** en `ingreso`, `posicion`, `estado`, `validacion`, `habilitacion` y en `POST /reservas` **de eventos de alta demanda**. Los eventos normales no se ven afectados, porque no consultan Redis para la fila. *Motivo:* la fila es control de acceso. Su propósito es evitar que todos golpeen el lock de MySQL; con Redis caído, el fail-open produciría exactamente eso. La integridad de la venta la garantiza MySQL en ambos casos, así que lo que se decide es la equidad. Es además más simple de implementar y de testear (sin bitácora especial). Se diverge a propósito de `StockCache`, que es fail-open porque es un caché.
- [x] **D4. Apertura de tandas automática, con override de ADMIN.**
  - Un job programado avanza la tanda en +N posiciones cada X segundos, topeado por el cupo disponible (sección 7).
  - `POST /fila/habilitacion` (solo ADMIN) fija la tanda a mano y funciona siempre, con o sin job.
  - Es compatible con el contrato: `specs/endpoints.md` indica "ADMIN/Sistema" como quien habilita **[V, según el análisis previo]**.
  - La decisión 16 de `reglas-de-negocio.md` queda cerrada con esto.
- [x] **D5. Token de un solo uso, consumido de forma atómica al inicio del gate y restaurado si la transacción no confirma.** Evita el doble uso concurrente y no quema el turno si la reserva falla (sección 6).
- [x] **D6. Un turno por cliente y evento.** Al consumir el token, el cliente queda en el SET `usados`. No puede volver a ingresar ni pedir otro token para ese evento. *Consecuencia:* si la reserva vence o el pago se rechaza, el turno **no se devuelve**. Es una simplificación explícita. La extensión posible (liberar el turno en `rechazarVenta` y `expireOne`) queda fuera de alcance.
- [x] **D7. TTL único.** Todas las claves de la fila de un evento (`zset`, `seq`, `tanda`, `usados`) comparten el mismo TTL, renovado por toda escritura y sembrado en el primer ingreso. La única excepción es el token, que tiene un TTL propio, más corto.
- [x] **D8. Cache del flag del evento.** `ingreso`, `posicion`, `estado`, `validacion` y el job consultan `msvc-eventos` (estado PUBLICADO y `esAltaDemanda`) a través de una caché en memoria de pocos segundos, para no acoplar el polling de la fila a la disponibilidad de eventos. No cachea stock ni cupos: la regla de no cachear disponibilidad (`backlog.md`) se mantiene.
- [x] **D9. UI de fila implementada (2026-11-20).** El SPA suma `/eventos/:id/fila` (estado público, turno con polling, validación y panel ADMIN de habilitación) y el checkout exige/envía `tokenFila` en `POST /reservas`. La decisión original de priorizar el backend no cambia el contrato: la UI consume los mismos 5 endpoints y las reglas siguen en Redis.
- [x] **D10. "Posición" es el número de turno, inmutable.** Es el orden de llegada asignado con un contador atómico y no cambia aunque otros compren. Quien está habilitado cumple `posicion ≤ tanda`. Para mostrar cuánto falta, las respuestas incluyen `delante = max(0, posicion − tanda)`: cantidad de posiciones que faltan habilitar para que sea su turno.
- [x] **D11. Pausa/reanudación por estado del evento (2026-11-20).** Al salir de `PUBLICADO` (suspender/cancelar/agotar/finalizar) o al entrar (publicar/republicar), `msvc-eventos` llama vía Feign a `POST /internas/fila/{id}/invalidacion` para descartar de inmediato la caché de D8. La fila queda inactiva porque todas las operaciones y el job exigen `PUBLICADO && esAltaDemanda`; no se borran las claves Redis (los turnos y tokens sobreviven con su TTL). Si ventas no responde, la caché converge por su TTL (10 s). La sincronización es best-effort y nunca revierte la publicación.

**Estados del cliente:** `EN_ESPERA` (posición mayor a la tanda) · `HABILITADO` (dentro de la tanda, sin token) · `CON_TOKEN` · `TURNO_USADO`.

## 4. Modelo de datos en Redis

Prefijo configurable, por defecto `fila` (`ticketya.fila.key-prefix`).

| Clave | Tipo | Contenido | TTL |
|---|---|---|---|
| `fila:{e}` | ZSET | miembro = `userId` (`X-User-Id`), score = posición | `ttl-hours` |
| `fila:{e}:seq` | String | contador de llegada (`INCR`) | `ttl-hours` |
| `fila:{e}:tanda` | String | última posición habilitada | `ttl-hours` |
| `fila:{e}:usados` | SET | clientes que ya consumieron su turno | hereda el TTL restante del ZSET |
| `fila:{e}:token:{u}` | String | token vigente del cliente | `token-ttl-minutes` |
| `fila:eventos` | SET | eventos con fila activa (índice para el job) | sin TTL; el job quita los eventos cuyo ZSET ya no existe |
| `fila:auto:lock` | String | lock del job (`SET NX PX`) | `auto.lock-ms` |

`{e}` es el `idEvento`. **Regla de TTL (D7):** `ingresar` y `habilitar` renuevan con el mismo `ttl-hours` las claves `zset`, `seq`, `tanda` y `usados` que existan; `ingresar` siembra `tanda = 0` con `SET NX`, de modo que las cuatro existen desde el primer ingreso. `usados` nace heredando el TTL restante del ZSET.

### Scripts Lua

Se ejecutan con `StringRedisTemplate.execute(script, keys, args)`. **Validados por `FilaRedisStoreIT` con Redis real (Testcontainers); ver sección 14.**

**`ingresar`** — idempotente; devuelve la posición, si fue creada en esta llamada y la tanda vigente.

```lua
-- KEYS: [1]=fila:{e}  [2]=fila:{e}:seq  [3]=fila:{e}:tanda  [4]=fila:{e}:usados  [5]=fila:eventos
-- ARGV: [1]=userId    [2]=ttlSegundos   [3]=idEvento
-- Retorno: {posición (>0), creado (1|0), tanda}  |  {-1} si el turno ya fue usado
if redis.call('SISMEMBER', KEYS[4], ARGV[1]) == 1 then return {-1} end
local pos = redis.call('ZSCORE', KEYS[1], ARGV[1])
local creado = 0
local n
if pos then
  n = tonumber(pos)
else
  n = redis.call('INCR', KEYS[2])
  redis.call('ZADD', KEYS[1], n, ARGV[1])
  creado = 1
end
redis.call('SET', KEYS[3], 0, 'NX')
for i = 1, 4 do redis.call('EXPIRE', KEYS[i], ARGV[2]) end
redis.call('SADD', KEYS[5], ARGV[3])
return {n, creado, redis.call('GET', KEYS[3])}
```

**`habilitar`** — la tanda nunca retrocede, ni siquiera con dos escritores concurrentes.

```lua
-- KEYS: [1]=tanda [2]=zset [3]=seq [4]=usados
-- ARGV: [1]=hastaPosicion [2]=ttlSegundos
-- Retorno: nueva tanda  |  -1 si se pidió un valor menor al actual
local actual = tonumber(redis.call('GET', KEYS[1]) or '0')
local pedido = tonumber(ARGV[1])
if pedido < actual then return -1 end
redis.call('SET', KEYS[1], pedido)
for i = 1, 4 do redis.call('EXPIRE', KEYS[i], ARGV[2]) end
return pedido
```

**`validar`** — emite el token o devuelve el vigente (get-or-create atómico).

```lua
-- KEYS: [1]=zset [2]=tanda [3]=token [4]=usados
-- ARGV: [1]=userId [2]=tokenNuevo [3]=ttlTokenSegundos
-- Retorno (lista): {codigo, posicion, tanda, token, ttlRestante}
--   codigo: 1 ok | -1 turno usado | -2 no ingresó | -3 fuera de tanda
if redis.call('SISMEMBER', KEYS[4], ARGV[1]) == 1 then return {-1} end
local pos = redis.call('ZSCORE', KEYS[1], ARGV[1])
if not pos then return {-2} end
pos = tonumber(pos)
local tanda = tonumber(redis.call('GET', KEYS[2]) or '0')
if pos > tanda then return {-3, pos, tanda} end
local t = redis.call('GET', KEYS[3])
if not t then
  redis.call('SET', KEYS[3], ARGV[2], 'EX', ARGV[3])
  t = ARGV[2]
end
return {1, pos, tanda, t, redis.call('TTL', KEYS[3])}
```

La lista mezcla enteros y strings: el store debe convertir cada elemento de forma explícita.

**`consumirToken`** — compare-and-delete que además marca el turno como usado, en una sola operación atómica.

```lua
-- KEYS: [1]=token [2]=usados [3]=zset
-- ARGV: [1]=token [2]=userId
-- Retorno: 1 consumido | 0 token inexistente, vencido o distinto
if redis.call('GET', KEYS[1]) == ARGV[1] then
  redis.call('DEL', KEYS[1])
  redis.call('SADD', KEYS[2], ARGV[2])
  local ttl = redis.call('PTTL', KEYS[3])
  if ttl > 0 then redis.call('PEXPIRE', KEYS[2], ttl) end
  return 1
end
return 0
```

**`restaurarToken`** — deshace el consumo cuando la transacción no confirmó.

```lua
-- KEYS: [1]=token [2]=usados
-- ARGV: [1]=token [2]=userId [3]=ttlTokenSegundos
redis.call('SREM', KEYS[2], ARGV[2])
redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3])
return 1
```

Al restaurar se usa `SET` sin `NX`: mientras el cliente estuvo en `usados`, `validar` no pudo emitirle otro token, así que no hay nada que pisar.

**Lecturas no atómicas** (suficientes para mostrar datos): `posicion` (`ZSCORE`, `GET tanda`, `EXISTS token`, `SISMEMBER usados`), `estado` (`ZCARD − SCARD usados`, `GET tanda`) y el resumen del job (`GET seq`, `GET tanda`, `ZCOUNT 0..tanda`, `SCARD usados`).

## 5. Contrato de los 5 endpoints

Rutas por el gateway (`application.yml` ya rutea `/api/eventos/*/fila/**` a `ms-ventas:8083`). En ventas: `@RequestMapping("/eventos/{idEvento}/fila")`. La identidad llega en `X-User-Id`, que el gateway inyecta desde el JWT.

> **[I]** `specs/endpoints.md` solo da la firma de una línea por endpoint. El formato de body y response de abajo se define acá y se versiona en OpenAPI en FIL-04. **Validar** nombres de campos contra ese archivo.

| Método y ruta | Rol | Descripción |
|---|---|---|
| `POST /api/eventos/{id}/fila/ingreso` | CLIENTE | Ingresa a la fila (idempotente) |
| `GET /api/eventos/{id}/fila/posicion` | CLIENTE | Posición propia |
| `GET /api/eventos/{id}/fila/estado` | Público | Estado general, sin identidades |
| `POST /api/eventos/{id}/fila/validacion` | CLIENTE | Emite o devuelve el token |
| `POST /api/eventos/{id}/fila/habilitacion` | **ADMIN** | Fija la tanda a mano (override) |

### 5.1 Ingreso y posición

`POST .../ingreso` (sin body): **201** la primera vez, **200** si ya estaba. `GET .../posicion`: **200**.

```json
{
  "idEvento": "…",
  "posicion": 57,
  "posicionHabilitadaHasta": 50,
  "delante": 7,
  "estado": "EN_ESPERA"
}
```

Errores:

- **401** sin JWT.
- **404** el evento no existe (en `posicion`: **404** `FILA_NO_INGRESADO`).
- **409** `FILA_EVENTO_NO_ELEGIBLE`: el evento no está PUBLICADO o no es de alta demanda.
- **409** `FILA_TURNO_USADO` (solo `ingreso`; en `posicion` el estado informado es `TURNO_USADO`).
- **503** `FILA_NO_DISPONIBLE`.

### 5.2 Estado (público)

`GET .../estado` → **200**. `totalEnFila` es la cantidad de clientes que aún no usaron su turno (`ZCARD − SCARD usados`). `abierta` indica que el evento es de alta demanda y está PUBLICADO. No expone identidades.

```json
{ "idEvento": "…", "totalEnFila": 340, "posicionHabilitadaHasta": 100, "abierta": true }
```

Errores: **404** evento inexistente · **503**.

Para que sea público, el gateway necesita una regla que lo permita sin JWT (hoy `/fila/**` exige token). Se incluye en FIL-01 con su test (sección 8). Ver punto abierto 2.

### 5.3 Validación (emisión del token)

`POST .../validacion` → **200**. Idempotente: con un token vigente devuelve el mismo, aun con llamadas concurrentes.

```json
{ "idEvento": "…", "token": "b7d1c9f0-…", "expiraEn": "2026-11-19T15:30:00Z", "posicion": 57, "delante": 0 }
```

Errores: **404** `FILA_NO_INGRESADO` · **409** `FILA_FUERA_DE_TANDA` (el cuerpo incluye `posicion`, `posicionHabilitadaHasta` y `delante`) · **409** `FILA_TURNO_USADO` · **409** `FILA_EVENTO_NO_ELEGIBLE` · **503**.

### 5.4 Habilitación manual (solo ADMIN)

`POST .../habilitacion`, body `{ "hastaPosicion": 100 }` → **200** `{ "idEvento": "…", "posicionHabilitadaHasta": 100, "modo": "MANUAL" }`.

Errores:

- **400** `hastaPosicion` ausente o menor a 1 (`@Min(1)`).
- **403** el rol no es ADMIN.
- **409** `FILA_TANDA_NO_REDUCIBLE`: menor a la tanda actual.
- **409** `FILA_EVENTO_NO_ELEGIBLE`.
- **503**.

Repetir el mismo valor es válido (**200**). El ADMIN puede fijar un valor mayor al total actual de la fila (abre la venta por anticipado); el job automático nunca supera el total.

### 5.5 Reserva (campo nuevo)

`POST /api/reservas` recibe un tercer componente opcional `String tokenFila` en `ReservaCreateRequestDTO`. **Se conserva el constructor de 2 argumentos** para no romper llamadas existentes:

```java
public ReservaCreateRequestDTO(String idEvento, List<Item> items) {
    this(idEvento, items, null);
}
```

## 6. Gate en `crearReserva`

Archivo: `msvc-ventas/src/main/java/ventas/service/VentaServiceImpl.java`. Orden de pasos:

1. **Idempotencia**: un reintento con la misma `Idempotency-Key` devuelve la reserva existente **sin pedir token**.
2. **Evento**: consulta a `EventosClient`, validación de PUBLICADO y lectura de `esAltaDemanda`.
3. **Si `esAltaDemanda = true`:**
   1. Sin `tokenFila` o vacío → **409** `FILA_TOKEN_REQUERIDO`.
   2. `consumirToken` (atómico). Devuelve 0 → **409** `FILA_TOKEN_INVALIDO` (inexistente, vencido, ya usado o de otro cliente). Error de Redis → **503** `FILA_NO_DISPONIBLE` (D3).
   3. Si se consumió, registrar de inmediato una `TransactionSynchronization`:
      - `afterCompletion(status)`: si `status != STATUS_COMMITTED`, ejecutar `restaurarToken` (el turno vuelve a quedar disponible).
      - `afterCommit`: registrar en bitácora `FILA_TOKEN_CONSUMIDO` con el `idReserva`.
4. Flujo actual sin cambios: límite por cliente, `lockCupo`, descuento de cupo, asientos y hold en Redis.

Todo el paso 3 va **antes de `lockCupo`**: sin token no se descuenta cupo ni se toman locks de MySQL.

**Evento normal** (`esAltaDemanda` falso o `null`): comportamiento idéntico al actual. **[I]** Los mocks de `VentaServiceImplTest` devuelven `false` para el flag nuevo, así que no debería haber regresión; se confirma corriendo la suite completa de ventas.

Garantías:

- **Un token = una reserva**, aun con requests concurrentes: el consumo es un compare-and-delete atómico.
- **El turno no se pierde por un fallo transitorio**: unique key de asientos, límite por cliente, excepción o cualquier rollback restauran el token.
- **Si la restauración falla** (Redis caído justo en `afterCompletion`): se registra `error`. No hay daño de integridad: el cliente sigue en `usados`, y pierde el turno (D6).

## 7. Apertura automática de tandas

Componente: `FilaTandaScheduler` en ventas, `@Scheduled(fixedDelayString = "${ticketya.fila.auto.intervalo-ms}")`. El patrón de jobs ya existe (`ReservaExpirationJob`, `EmisionPendienteJob`, `@EnableScheduling` en `VentasApplication.java`).

En cada tick, si `ticketya.fila.auto.enabled = true`:

1. Tomar el lock: `SET fila:auto:lock <idInstancia> NX PX <auto.lock-ms>`. Si no se obtiene, otra instancia ya corre este tick: salir.
2. Para cada evento de `fila:eventos`:
   - Si su ZSET ya no existe, quitarlo del índice y seguir.
   - Consultar el flag del evento (caché de D8). Si no está PUBLICADO o no es de alta demanda, o si no hay dato ni caché, saltearlo este tick.
   - Leer `seq`, `tanda` y `activos = ZCOUNT(0..tanda) − SCARD(usados)`: los habilitados que todavía no usaron su turno.
   - `cupo = SUM(cantidad_disponible)` de `cupo_sector` del evento (consulta nueva en el repositorio **[I]**).
   - `espacio = cupo − activos` ; `paso = min(tamanio, max(0, espacio))` ; `nueva = min(tanda + paso, seq)`.
   - Si `nueva > tanda`: ejecutar `habilitar(nueva)` y registrar `FILA_HABILITACION` (modo `AUTO`, valor anterior y nuevo). Un resultado `-1` (un ADMIN se adelantó) se ignora.
3. Cualquier excepción de Redis o de Feign se captura por evento: se registra `warn` y el tick sigue. **El scheduler nunca propaga la excepción.**

Propiedades:

- La **tanda es acumulativa por posición** y nunca decrece: así se cierra la decisión 16.
- El tope por cupo asume 1 entrada por cliente habilitado. Es un freno conservador de equidad, no una garantía de disponibilidad.
- Los habilitados que no compran retienen espacio del tope (no hay expiración por inactividad, sección 13). El ADMIN puede forzar la tanda con el endpoint manual.
- Con `enabled = false`, la fila funciona en modo 100 % manual. Es útil para tests deterministas.

## 8. Seguridad y roles

| Endpoint | Gateway | Control adicional en `msvc-ventas` |
|---|---|---|
| ingreso, posición, validación | JWT, rol CLIENTE | Lee `X-User-Id` y rechaza si falta; sello `X-Internal-Token` |
| estado | **Ruta pública** | Sin identidad; sello `X-Internal-Token` |
| habilitación | JWT, **solo ADMIN** | **Verifica `X-User-Role = ADMIN`** + sello `X-Internal-Token` |

**Acceso directo a `msvc-ventas`:** `InternalTokenFilter` exige el token compartido en `/eventos/{id}/fila/**` y el gateway lo inyecta (`JwtAuthenticationFilter.marcarComoGateway`). Un cliente que llame a `:8083` con `X-User-Role: ADMIN` recibe **401**. El resto de las rutas de ventas (por ejemplo `/eventos/{id}/cupos`, que usan eventos y entradas por Feign) queda fuera del sello para no romper esas llamadas. `SecurityConfig` sigue vacío, pero la superficie de la fila ya no depende solo de los headers.

**Cambio en el gateway:** `GET /api/eventos/*/fila/estado` es ruta pública; el test en `JwtGatewayIntegrationTest` verifica que no se propagan `X-User-*` del cliente y que **sí** se agrega el sello interno.

## 9. Configuración (`application.yml`)

Se agrega junto al bloque existente de ventas (`application.yml`). **Usar el prefijo real de ese bloque**; se asume `ticketya.fila.*` (punto abierto 7).

```yaml
ticketya:
  fila:
    key-prefix: ${TICKETYA_FILA_PREFIX:fila}
    ttl-hours: ${TICKETYA_FILA_TTL_HOURS:24}                       # TTL único de las claves de la fila (D7)
    token-ttl-minutes: ${TICKETYA_FILA_TOKEN_TTL_MINUTES:10}
    evento-cache-ttl-seconds: ${TICKETYA_FILA_EVENTO_CACHE_TTL_SECONDS:10}
    auto:
      enabled: ${TICKETYA_FILA_AUTO_ENABLED:true}
      tamanio: ${TICKETYA_FILA_AUTO_TAMANIO:25}
      intervalo-ms: ${TICKETYA_FILA_AUTO_INTERVALO_MS:10000}
      lock-ms: ${TICKETYA_FILA_AUTO_LOCK_MS:8000}
```

El TTL de la reserva es de 10 min (`application.yml`) y el del token se alinea con él.

## 10. Errores de negocio

| Código | HTTP | Cuándo |
|---|---|---|
| `FILA_EVENTO_NO_ELEGIBLE` | 409 | El evento no está PUBLICADO o no es de alta demanda |
| `FILA_NO_INGRESADO` | 404 | El cliente no está en la fila |
| `FILA_FUERA_DE_TANDA` | 409 | Posición mayor a la tanda |
| `FILA_TURNO_USADO` | 409 | El cliente ya consumió su turno en este evento |
| `FILA_TANDA_NO_REDUCIBLE` | 409 | `hastaPosicion` menor a la tanda actual |
| `FILA_TOKEN_REQUERIDO` | 409 | Reserva de alta demanda sin `tokenFila` |
| `FILA_TOKEN_INVALIDO` | 409 | Token inexistente, vencido, usado o de otro cliente |
| `FILA_NO_DISPONIBLE` | 503 | Redis no responde |

Todo error de Redis (`DataAccessException`) se envuelve en `503 FILA_NO_DISPONIBLE`. **Nunca hay fallback a MySQL.** Se usa `FilaException` + `GlobalExceptionHandler`, que agregan `code`/`extras` al payload.

## 11. Observabilidad

`Bitacora.registrar` con correlation id (`E2-NFR-03`, `backlog.md`) en: `FILA_INGRESO`, `FILA_HABILITACION` (modo `AUTO` o `MANUAL`, valor anterior y nuevo), `FILA_TOKEN_EMITIDO`, `FILA_TOKEN_CONSUMIDO` (con `idReserva`, en `afterCommit`), `FILA_TOKEN_RESTAURADO` y `FILA_GATE_RECHAZO`. Los logs de acceso del gateway (`trace.http`) ya cubren método, ruta, código y tiempo.

## 12. Plan de entrega: US, criterios y pasos

> **Registro histórico del plan (2026-11-19).** Los estados “parcial” y “en curso” que aparecen en
> esta sección describen el avance **planificado** en ese momento; hoy `FIL-01`…`FIL-04` y
> `E2-TST-03` están `hecho`. No son estados pendientes.

**Numeración:**

**`0a` → `FIL-01` → `FIL-02` → `FIL-03` → `FIL-04` → `TST-CARGA-01` (Partes A y B)**

> Numeración efectivamente usada en los commits del 2026-11-19. `FIL-04` es el cierre de contrato
> (docs only); la corrida de carga se ejecutó después, el 2026-11-21.

**Reglas de trabajo**

- Una US = un commit: código, tests y docs de su alcance.
- Antes de cada commit: compila, tests verdes y `git status --short` limpio. No se pasa al bloque siguiente con la verificación sucia.
- **Docs: cada archivo se edita como máximo dos veces** (al iniciar, como parcial; y al cerrar, como hecho), salvo las filas de endpoints, que se marcan una sola vez, en la US que los implementa.

| Archivo | Cuándo se toca |
|---|---|
| `specs/backlog.md` | `0a` (MOD-06) · TST/A (E2-TST-03 parcial) · FIL-01 (E2-FILA-01 en curso) · TST/B (E2-TST-03 hecho) · FIL-04 (cierre: E2-FILA-01, API-04, DOC-MS-VEN, MOD-06, resumen) |
| `specs/endpoints.md` y `msvc-ventas/docs/endpoints.md` | Cada US marca los endpoints que implementa (una sola vez cada uno) |
| `docs/api/gateway.md` | Solo FIL-04 |
| `specs/reglas-de-negocio.md` | FIL-02 (cierra la decisión 16) |

### BLOQUE `0a` — Base documental (docs only)

**Criterios de aceptación**

- **CA-0a.1** Este documento queda en `Backend/specs/fila-virtual.md`, validado por el equipo (sección 3 completa).
- **CA-0a.2** `specs/backlog.md` (MOD-06) pasa a: `Falta sólo la fila (FIL-01…FIL-04); notificaciones y recomendaciones ya documentadas en los diagramas.`
- **CA-0a.3** Se cierran los puntos abiertos 1–7 y 11 de la sección 16; los puntos 8–10 quedaron **[V]** en la corrida de carga del 2026-11-21 (§16).
- **CA-0a.4** Cero cambios de código.

**Pasos:** `git status --short` vacío → crear `specs/fila-virtual.md` → corregir `backlog.md` → `git diff --check` → commit.

**Commit:** `docs(fila): base de specs y corrección MOD-06 (0a)`

### BLOQUE `TST-CARGA-01 / Parte A` — Compra normal

> Se hace **antes** de la fila: no depende de ninguna `FIL-*` y asegura cuanto antes el entregable que exige la consigna (`consigna.md`). Los pasos detallados están en la sección 15.

**Criterios de aceptación**

- **CA-A.1** Plan JMeter versionado (`docs/testing/carga/compra-normal.jmx`) que ejecuta el flujo E1 por el gateway con JWT de CLIENTE: reserva → pago → verificación de emisión **[I]**, copiando el recorrido de la colección Postman E1.
- **CA-A.2** Parámetros configurables: usuarios concurrentes (p. ej. 25 / 50 / 100), ramp-up y duración. Duración **menor a 10 min** para que no entren expiraciones de reserva (TTL de 10 min).
- **CA-A.3** Evento **no** de alta demanda, con **sector general** y cupo suficiente para no agotarlo.
- **CA-A.4** Informe `docs/testing/carga.md` con entorno, usuarios concurrentes, ramp-up, duración, p50 / p95 / p99, RPS, errores agrupados por código HTTP y la verificación de integridad.
- **CA-A.5** `backlog.md` pasa a `parcial`: "escenario normal hecho; falta escenario con fila".

**Commit:** `test(carga): escenario compra normal + informe mínimo (E2-TST-03)`

### BLOQUE `FIL-01` — Ingreso, posición y estado

*Como cliente, quiero ingresar a la fila de un evento de alta demanda y saber cuánto falta; como cualquier visitante, quiero ver el estado general de la fila.*

**Criterios de aceptación**

- **CA-1.1** `POST .../ingreso` de un cliente autenticado asigna posición en el servidor: **201** la primera vez.
- **CA-1.2** Reingresar devuelve **200 con la misma posición**, sin `ClassCastException`. Dos hilos concurrentes del mismo cliente no duplican la entrada.
- **CA-1.3** N clientes distintos que ingresan a la vez reciben posiciones **únicas, consecutivas** y en orden de llegada al servidor.
- **CA-1.4** Evento no PUBLICADO o sin `esAltaDemanda` → **409** `FILA_EVENTO_NO_ELEGIBLE`. Cliente en `usados` → **409** `FILA_TURNO_USADO`.
- **CA-1.5** `GET .../posicion`: **200** con `posicion`, `posicionHabilitadaHasta`, `delante` y `estado`; **404** `FILA_NO_INGRESADO` si no ingresó.
- **CA-1.6** `GET .../estado` es público (sin JWT, vía gateway), devuelve `totalEnFila`, `posicionHabilitadaHasta` y `abierta`, y no expone identidades.
- **CA-1.7** Después del primer ingreso, `zset`, `seq` y `tanda` existen y tienen un TTL de `ttl-hours` (D7).
- **CA-1.8** Con la caché del flag: dos requests dentro del TTL hacen **una sola** llamada Feign. Si `msvc-eventos` cae y hay dato en caché, responde con el dato; si no hay dato, **503**.
- **CA-1.9** Redis caído → **503** `FILA_NO_DISPONIBLE`, sin excepciones sin controlar y sin fallback a MySQL.
- **CA-1.10** El ingreso queda en bitácora con el correlation id.

**Pasos y archivos** (`msvc-ventas/src/main/java/ventas/…`)

1. `application.yml` : bloque de la sección 9.
2. `redis/FilaRedisStore.java`: claves y scripts `ingresar` y lecturas de la sección 4, con el envoltorio `DataAccessException` → `503` en **todos** los métodos.
3. `clients/EventosClient.java` : `private Boolean esAltaDemanda;` en `EventoDetalleFeignDTO` (Lombok ya está), más el componente de caché del flag (TTL `evento-cache-ttl-seconds`).
4. `service/FilaService.java` y `FilaServiceImpl.java`: `ingresar` (consulta el flag; PUBLICADO es el valor `"PUBLICADO"`, `EventoServiceImpl.java`), `posicion` y `estado`.
5. `controller/FilaController.java` (`@RequestMapping("/eventos/{idEvento}/fila")`) con `@PostMapping("/ingreso")`, `@GetMapping("/posicion")` y `@GetMapping("/estado")`; DTOs `FilaIngresoResponseDTO`, `FilaPosicionResponseDTO` y `FilaEstadoResponseDTO`.
6. Gateway: regla pública de `GET /api/eventos/*/fila/estado`.

**Tests**

- Unitarios de `FilaServiceImpl` con el store mockeado.
- `FilaControllerTest` con MockMvc standalone (patrón `EntradaControllerValidacionTest.java`).
- **IT `FilaRedisStoreIT` con Redis en Testcontainers** (sección 14).
- `JwtGatewayIntegrationTest`: la ruta pública de `estado`.

**Docs:** `msvc-ventas/docs/endpoints.md` (sección "Fila virtual"), `specs/endpoints.md` marcados como implementados, `backlog.md` → "en curso (FIL-02…FIL-04 pendientes)".

**Commit:** `feat(fila): ingreso, posición y estado con Redis (FIL-01)`

### BLOQUE `FIL-02` — Tandas: automática y override ADMIN

*Como sistema, quiero abrir la compra por tandas sin intervención humana; como administrador, quiero poder forzar la tanda.*

**Criterios de aceptación**

- **CA-2.1** `POST .../habilitacion` con `{"hastaPosicion": N}` fija la tanda y devuelve **200**. Repetir el mismo valor devuelve 200.
- **CA-2.2** CLIENTE, ORGANIZADOR y PERSONAL_ACCESO reciben **403**, **incluso si el gateway los deja pasar**. ADMIN recibe 200.
- **CA-2.3** Un valor menor a la tanda actual devuelve **409** `FILA_TANDA_NO_REDUCIBLE`. `hastaPosicion` ausente o menor a 1 devuelve **400**.
- **CA-2.4** Dos habilitaciones concurrentes dejan como resultado **el valor mayor**.
- **CA-2.5** Con el job habilitado, cada tick avanza la tanda en `min(tamanio, espacio, seq − tanda)`. Con `espacio ≤ 0` no abre nada, y la tanda nunca supera `seq`.
- **CA-2.6** Con dos instancias, solo una ejecuta cada tick (lock `fila:auto:lock`).
- **CA-2.7** Con `auto.enabled = false` el job no abre nada. Una excepción de Redis o de Feign no escapa del scheduler. Un evento no PUBLICADO se saltea.
- **CA-2.8** Cada habilitación queda en bitácora con modo (`AUTO` o `MANUAL`), valor anterior y nuevo.
- **CA-2.9** Se cierra la decisión 16: "habilita el Sistema (automático) y el ADMIN (override); la tanda es acumulativa por posición y nunca decrece".

**Pasos y archivos**

1. `FilaRedisStore`: script `habilitar` y métodos de resumen y lock del job.
2. Repositorio de cupos: consulta `SUM(cantidad_disponible)` por evento **[I]**.
3. `FilaTandaScheduler` (sección 7).
4. `FilaHabilitacionRequestDTO` con `@Min(1) Integer hastaPosicion`; `@PostMapping("/habilitacion")` leyendo `X-User-Role` y rechazando con 403 si no es ADMIN.

**Tests:** servicio; controller con los cuatro roles; unitario del cálculo del job; IT de `habilitar` concurrente y del lock con dos "instancias"; `JwtGatewayIntegrationTest` con ADMIN 200 y ORGANIZADOR 403 (patrón de test del gateway).

**Docs:** `specs/reglas-de-negocio.md`, `specs/endpoints.md` y `msvc-ventas/docs/endpoints.md`.

**Commit:** `feat(fila): apertura automática de tandas y override ADMIN (FIL-02)`

### BLOQUE `FIL-03` — Token y gate ← riesgo concentrado

*Como cliente habilitado, quiero un token que me permita reservar; como sistema, quiero que sin él no se pueda reservar un evento de alta demanda.*

**Criterios de aceptación**

*Validación*

- **CA-3.1** Con posición dentro de la tanda, `POST .../validacion` devuelve **200** con token, `expiraEn`, `posicion` y `delante = 0`.
- **CA-3.2** Revalidar con un token vigente devuelve **el mismo token**, también con llamadas concurrentes.
- **CA-3.3** Fuera de tanda → **409** `FILA_FUERA_DE_TANDA`. Sin ingreso → **404**. Turno usado → **409** `FILA_TURNO_USADO`. Token vencido y cliente aún habilitado → emite uno nuevo.

*Gate*

- **CA-3.4** Evento de alta demanda con token válido: la reserva se crea como hoy y el token queda consumido.
- **CA-3.5** Sin token, o con token inválido, vencido, usado o de otro cliente → **409** y **no se descuenta cupo** (`cantidad_disponible` sin cambio).
- **CA-3.6** Con 20 requests concurrentes y el mismo token se crea **exactamente una** reserva.
- **CA-3.7** Si la reserva falla y la transacción hace rollback (límite por cliente, unique key de asientos, excepción), el token **se restaura** y el cliente puede reintentar.
- **CA-3.8** Tras una reserva confirmada, el cliente queda en `usados`: no puede ingresar ni validar de nuevo en ese evento.
- **CA-3.9** Redis caído en un evento de alta demanda → **503** y no se descuenta cupo. Un evento normal con Redis caído funciona igual (D3).
- **CA-3.10** Evento normal: comportamiento idéntico al actual. **La suite completa de ventas y los 229 requests de Postman siguen pasando.**
- **CA-3.11** Un reintento con la misma `Idempotency-Key` no exige token.

**Pasos y archivos**

1. `FilaRedisStore`: scripts `validar`, `consumirToken` y `restaurarToken`.
2. `FilaValidacionResponseDTO`, `@PostMapping("/validacion")` y `FilaService.validarToken`.
3. `dto/ReservaCreateRequestDTO.java`: tercer componente `tokenFila` y constructor de 2 argumentos (sección 5.5).
4. `VentaServiceImpl.java`: gate de la sección 6, inyectando `FilaService`. Cuidar los tests que instancian el constructor (`VentaServiceImplTest`, `ReservaAsientoConcurrenciaTest`).

**Tests:** `FilaServiceTest` (vigente, vencido, fuera de tanda, usado); casos nuevos en `VentaServiceImplTest` (alta demanda con y sin token, token inválido, Redis caído, evento normal). Para el rollback, inicializar `TransactionSynchronizationManager.initSynchronization()` y disparar `afterCompletion(STATUS_ROLLED_BACK)` a mano. IT de consumo concurrente. **Suite completa de `msvc-ventas`.**

**Docs:** `specs/endpoints.md`, `specs/comunicacion.md`, sección nueva en `specs/diagramas/diagramas-secuencia.md` (ingreso → tandas → validación → reserva, con la rama de rollback) y `msvc-ventas/docs/endpoints.md`.

**Commit:** `feat(fila): token temporal y gate por alta demanda (FIL-03)`

### BLOQUE `TST-CARGA-01 / Parte B` — Escenario con fila

> Recién acá existen el gate y las tandas. Los pasos detallados están en la sección 15.

**Criterios de aceptación**

- **CA-B.1** Thread Group de CLIENTES sobre un evento `esAltaDemanda = true` con sector general: `ingreso → posicion (poll) → validacion → reserva`.
- **CA-B.2** **B1, tanda automática (por defecto):** el tamaño y el intervalo se configuran por variables de entorno del contenedor de ventas. No hace falta hilo ADMIN.
- **CA-B.3** **B2, opcional, solo manual:** con `TICKETYA_FILA_AUTO_ENABLED=false`, un hilo con **JWT de ADMIN** sube `hastaPosicion` en incrementos. Sin ese hilo la validación se traba y la corrida no avanza.
- **CA-B.4** Los clientes con posición fuera de tanda hacen poll hasta obtener token o hasta un timeout configurable. El timeout se cuenta como error, no como éxito.
- **CA-B.5** Métricas separadas: latencia de `ingreso`, tiempo en fila (de `ingreso` a token), `validacion`, `crearReserva`, RPS y errores (`409` fuera de tanda, `503`, timeouts), con la comparación de overhead contra la Parte A.
- **CA-B.6** Misma verificación de no doble venta que la Parte A, más las comprobaciones de Redis de la sección 15.
- **CA-B.7** `backlog.md` pasa a `hecho`.

**Commit:** `test(carga): escenario con fila, tandas automática y manual (E2-TST-03)`

### BLOQUE `FIL-04` — Cierre de contrato (docs only)

**Criterios de aceptación**

- **CA-4.1** OpenAPI regenerado (`docs/openapi/msvc-ventas.json` y `openapi-bundle.json`) con los 5 endpoints, ejemplos, códigos de error y `bearerAuth`. `docs/openapi/README.md` corregidos (la parte `/fila`).
- **CA-4.2** Colección Postman: carpeta nueva "Fila virtual" con los 5 requests. El de `habilitacion` usa JWT de ADMIN y body `{"hastaPosicion": N}` (`docs/postman/README.md`).
- **CA-4.3** `docs/api/gateway.md` pasan a "implementada", con el RBAC y la ruta pública de `estado`.
- **CA-4.4** `specs/diagramas/modelo*.md`, `docs/modelo-datos.md` y `specs/bases-de-datos.md`: nota de que la fila vive en Redis y **no** entra al ER de MySQL, con la justificación de D1.
- **CA-4.5** `specs/backlog.md`: E2-FILA-01 hecho, API-04 “falta sólo notificaciones”, DOC-MS-VEN, MOD-06 cerrado y resumen. `reglas-de-negocio.md` actualizado.
- **CA-4.6** Checklist final (sección 17) sin coincidencias en el grep de "no implementada" fuera de la tabla histórica de §2.

**Commit:** `docs(fila): cierre de contrato y backlog (FIL-04)`

## 13. Simplificaciones, riesgos y limitaciones

**Simplificaciones justificadas** (`consigna.md` permite simplificar justificando)

| Recorte | Motivo | Consecuencia |
|---|---|---|
| Solo Redis, sin MySQL | La fila es efímera; el orden no requiere durabilidad transaccional | Un reinicio de Redis sin persistencia vacía la fila |
| Posición inmutable (turno) | Simplifica la atomicidad y evita recalcular rangos | El cliente lee `delante` en vez de un lugar real |
| Un turno por cliente y evento | Corta el bucle de turnos con poca complejidad | Una reserva vencida o un pago rechazado no devuelve el turno |
| Sin expiración por inactividad | Evita jobs y estados intermedios | Un habilitado que no compra retiene espacio del tope |
| Tope de cupo con 1 entrada por habilitado | Heurística barata y conservadora | No garantiza stock, solo frena la apertura |
| Gate solo en alta demanda | No afecta el flujo E1 | Los eventos normales no tienen orden de acceso |
| UI mínima en el SPA | Se agregó pantalla de fila; no hay avisos push ni estimación de espera | El cliente ve posición/tanda en la pestaña y actualiza por polling cada 5 s |

**Riesgos y limitaciones conocidas**

1. **Evicción de Redis.** `redis.conf` define `maxmemory 256mb` con `volatile-lru`. Como todas las claves de la fila llevan TTL, Redis podría desalojarlas si el caché de stock llena la memoria. La corrida de carga midió `evicted_keys=0`; si apareciera evicción, subir `maxmemory` o separar la fila en otra instancia (fuera de alcance).
2. **Persistencia de Redis.** `redis.conf` tiene `appendonly yes` (AOF); un reinicio no debería borrar posiciones, tanda, tokens ni `usados`.
3. **Scripts Lua.** Validados por `FilaRedisStoreIT` con Redis real (Testcontainers).
4. **Desfase del flag.** Una suspensión o cancelación del evento puede tardar hasta `evento-cache-ttl-seconds` en reflejarse en la fila.
5. **Polling sin límite.** `posicion` y `estado` no tienen rate limit.
6. **Lecturas no atómicas.** `posicion` y `estado` pueden mostrar un valor desfasado por un instante. No afecta decisiones: las decisiones se toman dentro de scripts atómicos.

## 14. Estrategia de pruebas

| Nivel | Qué cubre | US |
|---|---|---|
| Unitario (Mockito) | Lógica de `FilaServiceImpl`, validaciones, errores, cálculo del job | FIL-01 a 03 |
| **IT con Redis (Testcontainers, imagen `redis:7`)** | Los scripts Lua contra un Redis real | FIL-01 a 03 |
| Controller (MockMvc standalone) | Contratos, códigos HTTP y los cuatro roles | FIL-01 a 03 |
| Gateway (`JwtGatewayIntegrationTest`) | Ruta pública de `estado`; ADMIN 200 y ORGANIZADOR 403 en `habilitacion` | FIL-01, 02 |
| Regresión de ventas | Suite completa de `msvc-ventas` y Postman E1 | FIL-03 |
| Carga | Escenarios A y B con informe | TST-CARGA-01 |

Mockito sobre `ZSetOperations` o `ValueOperations` verifica llamadas, no el Lua. **El IT es obligatorio.** Se anota con `@Testcontainers(disabledWithoutDocker = true)` para que `mvn test` no falle en máquinas sin Docker.

**Casos del IT (`FilaRedisStoreIT`)**

- **Ingreso:** 100 hilos con usuarios distintos → posiciones únicas y consecutivas del 1 al 100. Reingreso → misma posición y tipo numérico correcto. Mismo usuario con 20 hilos → una sola entrada.
- **TTL:** tras operar, `zset`, `seq`, `tanda` y `usados` tienen TTL positivo y equivalente; `usados` no supera al del ZSET.
- **Habilitar:** dos escritores concurrentes con valores distintos → queda el mayor. Un valor menor devuelve `-1`.
- **Validar:** 20 hilos pidiendo token → un único token. Fuera de tanda → código `-3`. Con `usados` → código `-1`.
- **Consumir y restaurar:** 20 hilos con el mismo token → exactamente un `1`. Tras `restaurarToken`, el token vuelve a ser válido y el cliente sale de `usados`.
- **Lock del job:** dos tomas simultáneas → solo una gana.

## 15. Runbook de la prueba de carga

**Reglas de la carga**

- Reservar por **`POST /api/reservas`**, la ruta que expone el gateway (verificar en `docs/api/gateway.md`).
- **No enviar `X-User-Id`** desde JMeter: el gateway lo inyecta desde el JWT y descarta el que mande el cliente (`JwtAuthenticationFilter.java`).
- Usar un **pool de usuarios** (CSV), por el límite de 6 entradas por cliente y evento.
- Usar un **sector general (no numerado)**: en uno numerado, 50 hilos chocan por los mismos asientos.
- Ejecutar los comandos de MySQL contra el servicio `db-mysql-ventas` y los de Redis contra `db-redis-ventas` (nombres según `docker compose ps`).
- El hilo ADMIN solo es obligatorio en el escenario B2 (tandas manuales).

Antes de cada corrida, para partir de un seed conocido: `cd Backend && docker compose down -v && docker compose up -d --build`. **`down -v` borra los datos de todas las bases.**

### Parte A — Compra normal

**T-A.1 Entorno.** Levantar el stack y esperar a que todos los servicios estén "Up" y las bases "healthy". Smoke: `curl -sf http://localhost:8080/actuator/health` **[V]**.

**T-A.2 Datos y baseline.**

1. Elegir un evento PUBLICADO con `esAltaDemanda = false` y un **sector general** con cupo suficiente. Evitar los seeds agotados (`init.sql`).
2. Calcular el cupo necesario: `usuarios × reservas por usuario × entradas por reserva`.
3. Guardar el snapshot inicial en `docs/testing/carga.md` (tabla "Antes"):

```sql
SELECT id_sector, capacidad_total, cantidad_disponible,
       cantidad_reservada, cantidad_vendida
FROM cupo_sector
WHERE id_evento = :idEvento AND id_sector = :idSector;
```

Comando **[V]** (usuario `ticketya` / clave `${MYSQL_PASSWORD:-ticketya_dev}`, según `msvc-ventas/docker-compose.yml` y `.env.example`): `docker compose exec -T db-mysql-ventas mysql -uticketya -pticketya_dev ventas -e "<SQL>"`.

**T-A.3 Pool de usuarios.**

- `usuarios.csv` con columnas `email,token`: un JWT por usuario virtual, firmado (HS256) con el secreto compartido del gateway. Verificar los nombres de los claims y el formato del secreto contra un token real obtenido por login. Debe tener **tantas filas como usuarios concurrentes**, y cada hilo toma una fila distinta.
- Con `max-entradas-cliente-evento = 6`, un mismo usuario recibe 409 muy pronto. Diseñar `iteraciones × entradas por reserva ≤ 6`, o ampliar el pool.
- **[V]** Ningún servicio valida al usuario contra `msvc-usuarios` en el flujo de compra (JWT stateless); solo `msvc-notificaciones` resuelve el email de forma *fail-soft*, así que los usuarios del pool no necesitan existir para la carga.

**T-A.4 Plan `docs/testing/carga/compra-normal.jmx`.**

- Variables: `-Jusuarios`, `-Jramp`, `-Jduracion`, `-JidEvento`, `-JidSector`.
- `HTTP Request Defaults` → `http://localhost:8080`.
- `CSV Data Set Config` → `usuarios.csv`.
- `HTTP Header Manager` → `Content-Type: application/json`, `Authorization: Bearer ${token}`, `X-Correlation-Id: carga-normal-${__threadNum}`. **Sin `X-User-Id`.**
- Secuencia: `POST /api/reservas` → pago → verificación de emisión **[I]**, mismo recorrido que la colección Postman E1.
- `JSON Extractor` para `idReserva`; `Response Assertion` para 200 / 201; un `Constant Timer` corto.
- Verificar abriendo el `.jmx` una vez en la GUI y corriendo con 1 usuario.

**T-A.5 Ejecutar** (sin GUI: la interfaz consume recursos y distorsiona los números):

```bash
jmeter -n -t Backend/docs/testing/carga/compra-normal.jmx \
  -JidEvento=<uuid> -JidSector=<uuid> \
  -Jusuarios=50 -Jramp=30 -Jduracion=300 \
  -l Backend/docs/testing/carga/resultados-normal.jtl \
  -e -o Backend/docs/testing/carga/report-normal
```

No versionar `.jtl` ni `report-*` (agregarlos a `.gitignore` en este commit). Sí versionar `.jmx`, el CSV de ejemplo y el informe.

**T-A.6 Métricas.** Del reporte HTML: p50 / p95 / p99, RPS, errores por código HTTP y transacciones OK / KO.

**T-A.7 Verificación de no doble venta** (repetir el SQL de T-A.2 y comparar contra el baseline):

- `capacidad_total = cantidad_disponible + cantidad_reservada + cantidad_vendida` en ambos snapshots.
- Ningún contador negativo.
- `Δ(cantidad_reservada + cantidad_vendida)` coincide con las operaciones exitosas que informa JMeter (la duración menor a 10 min evita expiraciones, `backlog.md`).
- **Sector general:** cruzar el contador contra `detalle_reserva` y compras en estado `APROBADA` (`VentaServiceImpl.java`, `ReporteServiceImpl.java`).
- **Sector numerado (solo si se prueba uno):** la consulta de duplicados debe devolver 0 filas, con respaldo en `uk_asiento_evento_sector_fila_numero` (`init.sql`):

```sql
SELECT id_sector, fila, numero, COUNT(*) AS veces
FROM asiento_reservado
WHERE id_evento = :idEvento
GROUP BY id_sector, fila, numero
HAVING COUNT(*) > 1;
```

- Redis: `docker compose exec -T db-redis-ventas redis-cli INFO stats | grep evicted_keys` → **0**.

Estos chequeos complementan al IT existente (`docs/testing/concurrencia.md`, `AsientoReservadoConcurrenciaIT`); no lo reemplazan.

**T-A.8 Informe y backlog.** `docs/testing/carga.md` con las secciones `Entorno`, `Parámetros`, `Resultados (normal)`, `Verificación de integridad` y `Conclusión`. Aclarar como limitación que la carga y los 17 contenedores corren en la misma máquina. `backlog.md` → parcial. `git diff --check` y commit.

### Parte B — Escenario con fila

**T-B.1 Preparación.** Evento PUBLICADO con `esAltaDemanda = true`, **sector general** y cupo suficiente. Mismo baseline SQL y mismo pool de usuarios. Configurar el contenedor de ventas con `TICKETYA_FILA_AUTO_TAMANIO`, `TICKETYA_FILA_AUTO_INTERVALO_MS` y `TICKETYA_FILA_AUTO_ENABLED`: el compose de `msvc-ventas` ya los recibe desde `.env`/shell (`msvc-ventas/docker-compose.yml`, bloque `TICKETYA_FILA_*`).

**T-B.2 Thread Group CLIENTES** (`docs/testing/carga/compra-con-fila.jmx`). Por hilo:

1. `POST /api/eventos/{id}/fila/ingreso` (JWT del cliente) → guardar `posicion`.
2. `GET /api/eventos/{id}/fila/posicion` en poll (intervalo y timeout configurables) hasta que `estado = HABILITADO`.
3. `POST /api/eventos/{id}/fila/validacion` → guardar `token`.
4. `POST /api/reservas` con `tokenFila`.
5. Pago y verificación, como en T-A.4.

Headers: `Authorization` del cliente y `X-Correlation-Id: carga-fila-${__threadNum}`. Sin `X-User-Id`.

**T-B.3 Hilo ADMIN (solo escenario B2).** Con `AUTO_ENABLED=false`: obtener el JWT de ADMIN y, en bucle, `POST .../habilitacion` con `{"hastaPosicion": N}`, incrementando `N` (p. ej. +25 cada 10 s, con un `Counter`). El ADMIN corre en paralelo desde el inicio. Si los clientes agotan el timeout antes de que el ADMIN habilite, se cuenta como error.

**T-B.4 Ejecutar:**

```bash
jmeter -n -t Backend/docs/testing/carga/compra-con-fila.jmx \
  -JidEvento=<uuid> -JidSector=<uuid> \
  -Jusuarios=50 -Jramp=30 -Jduracion=300 \
  -l Backend/docs/testing/carga/resultados-fila.jtl \
  -e -o Backend/docs/testing/carga/report-fila
```

**T-B.5 Métricas específicas:** latencia de `ingreso`, tiempo en fila, `validacion`, `crearReserva`, RPS, y errores (`409` fuera de tanda, `503`, timeouts). Comparar contra la línea base de la Parte A para cuantificar el overhead del gate.

**T-B.6 Integridad.** Repetir T-A.7 sobre el evento de alta demanda y sumar las comprobaciones de la fila:

```bash
docker compose exec -T db-redis-ventas redis-cli ZCARD fila:<idEvento>          # = clientes que ingresaron
docker compose exec -T db-redis-ventas redis-cli SCARD fila:<idEvento>:usados   # = reservas exitosas (clientes distintos)
docker compose exec -T db-redis-ventas redis-cli GET fila:<idEvento>:tanda      # tanda final
docker compose exec -T db-redis-ventas redis-cli INFO stats | grep evicted_keys # = 0
```

Agregar la sección `Resultados (con fila)` a `docs/testing/carga.md`, `backlog.md` → `hecho` y commit.

## 16. Puntos abiertos para el equipo

**Resueltos el 2026-11-19 (acuerdo del equipo):**

1. **D3 y D4 confirmados:** fail-closed también en el gate, y tanda automática con override ADMIN.
2. **`estado` es público:** el gateway necesita la regla pública (incluida en FIL-01).
3. **Código del gate:** **409**. Queda así en OpenAPI.
4. **TTL del token:** **10 minutos**, igual que la reserva.
5. **Turno único (D6):** alcanza. No se libera el turno cuando la reserva vence o el pago se rechaza; la liberación queda fuera de alcance.
6. **El ADMIN puede fijar `hastaPosicion` mayor al total.** El job automático nunca supera el total.
7. **Prefijo de configuración real:** `ticketya.fila.*`. El bloque existente de ventas es `ticketya.ventas.*` (`application.yml`).
11. **US de Trello:** `0a` versiona esta especificación de todos modos; no bloquea.

**Verificados en la corrida de carga (2026-11-21):**

8. **Configuración real de Redis:** `maxmemory 256mb` con `volatile-lru`; `evicted_keys=0` en todas las corridas.
9. **Pool de usuarios:** `generar-usuarios.sh` registró los usuarios y renovó los JWT (1 h) antes de cada corrida.
10. **Credenciales de MySQL** del compose usadas para los snapshots de cupo/entradas.

## 17. Checklist después de cada commit

```bash
grep -rn '/fila' Backend/msvc-ventas/docs Backend/docs/openapi
grep -n 'E2-FILA-01\|E2-TST-03' Backend/specs/backlog.md
grep -rniE 'sin impl[e]mentar|no implem[e]ntada' Backend/docs Backend/specs | grep -i fila   # solo la tabla histórica de §2 (FIL-04); [e] evita que el propio comando coincida
mvn -q -f Backend/msvc-ventas/pom.xml test     # ejecutado en FIL-01…FIL-04
mvn -q -f Backend/APIGateway/pom.xml test      # ejecutado en FIL-01…FIL-04
git diff --check && git status --short
```

## 18. Matriz de trazabilidad

> Numeración **final**. La corrida de carga real se ejecutó después de FIL-04; resultados en
> `docs/testing/carga.md`.

| Commit | US | Código | Tests | Docs que se tocan |
|---|---|---|---|---|
| 1 | `0a` | — | — | `specs/fila-virtual.md`, `backlog.md` |
| 2 | `FIL-01` | store Redis (Lua), service, controller, ingreso/posición/estado público, caché del flag, regla pública del gateway | unitarios, `FilaRedisStoreIT`, `FilaControllerTest`, gateway | `endpoints.md` (ingreso/posición/estado), docs de ventas, `backlog.md` (en curso) |
| 3 | `FIL-02` | `habilitar`, `FilaTandaScheduler`, lock de instancia y RBAC ADMIN | servicio, IT concurrente y de lock, `JwtGatewayIntegrationTest` | `reglas-de-negocio.md`, `endpoints.md` |
| 4 | `FIL-03` | `validar`, consumir/restaurar token y gate fail-closed en `POST /reservas` | `FilaServiceTest`, `VentaServiceImplTest`, IT de consumo, suite completa | `endpoints.md`, `comunicacion.md`, diagrama de secuencia §9 |
| 5 | `FIL-04` | — (docs only): cierre de contrato y checklist final | `mvn test` de ventas y gateway | OpenAPI, Postman, `gateway.md`, modelos, `backlog.md`, `reglas-de-negocio.md` |
| 6 | `TST-CARGA-01/A` | — | JMeter compra normal + SQL + `evicted_keys` | `docs/testing/carga*`, `.gitignore`, `backlog.md` (parcial) |
| 7 | `TST-CARGA-01/B` | — | JMX con fila (B1 auto, B2 admin) + verificaciones SQL/Redis; matriz 25/50/100 ejecutada | `docs/testing/carga*` (hecho, incluye post-fix) |

## 19. No verificado y supuestos

- **[V]** Maven sí se ejecutó en cada bloque: `mvn -q -f Backend/msvc-ventas/pom.xml test` (212 tests en esa corrida, 0 fallos/errores; hoy la suite tiene 221) y `mvn -q -f Backend/APIGateway/pom.xml test` (OK).
- **[V]** Docker y JMeter: matriz **25/50/100** ejecutada (normal y con fila) y reintentos post-fix; `E2-TST-03` **hecho** y `docs/testing/carga.md` completo. Se midió `evicted_keys=0` y se corrigieron los hallazgos de emisión duplicada y timeout→503.
- **[V]** Configuración real de Redis: `redis.conf` define `maxmemory 256mb`, `volatile-lru` y `appendonly yes`; la corrida de carga midió `evicted_keys=0`.
- **[V]** Credenciales de MySQL en el compose: usuario `ticketya` y `MYSQL_PASSWORD=${MYSQL_PASSWORD:-ticketya_dev}` en `db-mysql-ventas` (ver `.env.example`). **[V]** Endpoint de health y `TICKETYA_FILA_AUTO_*` se inyecta por `msvc-ventas/docker-compose.yml` desde `.env`/shell.
- **[V]** El mecanismo de errores de ventas es `FilaException` (con `code`, `HttpStatus` y `extras`) + `GlobalExceptionHandler`, que agrega `code`/extras al payload cuando existen; está documentado en `ApiError` del OpenAPI.
- **[NV]** Si el tablero de Trello tiene una US de fila con CA propios (no verificable desde el repo; §16.11 fija que no bloquea).
- **[V]** Formato de body y response de los 5 endpoints: definido acá, implementado y documentado en OpenAPI/Postman.
- **[V]** Consulta `SUM(cantidad_disponible)` por evento: agregada al repositorio y usada por `FilaTandaScheduler`.
- **[V]** Secuencia de pago y verificación de emisión en los planes JMeter: copiada de la colección Postman E1 (sin ejecutar).
- **[V]** Los mocks de `VentaServiceImplTest` cubren `esAltaDemanda=true` (gate) y `false` (flujo normal).
- **[V]** `specs/endpoints.md` marca ADMIN/Sistema la habilitación, `"PUBLICADO"` el flag de elegibilidad y los estados de reserva/compra quedaron verificados por los tests del bloque FIL-03.

## 20. Referencias

- Consigna: `specs/consigna.md`
- Backlog: `specs/backlog.md`
- Contrato y docs: `specs/endpoints.md` · `specs/comunicacion.md` · `specs/bases-de-datos.md` · `specs/reglas-de-negocio.md` · `docs/api/gateway.md` · `docs/openapi/README.md`
- Gateway: `APIGateway/src/main/resources/application.yml` · `JwtAuthenticationFilter.java`
- Ventas: `VentaServiceImpl.java` · `VentasApplication.java` · `StockCache.java` · `RedisConfig.java` · `EventosClient.java` · `VentaController.java` · `ReporteController.java`
- Eventos: `Evento.java` · `EventoMapper.java` · `EventoServiceImpl.java`
- BD: `infrastructure/mysql/ventas/init.sql`
