Veredicto: el plan es viable. El enfoque (Redis, ZSET, token y gate por esAltaDemanda) es el correcto y cumple la consigna (consigna.md:63,148). Tiene un bug concreto en el Lua, dos problemas de diseño en FIL-04 y varios huecos en la prueba de carga. No ejecuté nada; lo verifiqué leyendo el código.

Alineación con mi idea

No tengo ningún diseño de la fila guardado de antes. En mi memoria la fila solo figura como "candidata E2", así que no puedo afirmar que coincida con una idea previa. Contra lo que habría propuesto hoy, el núcleo coincide: Redis-only, ZSET con contador atómico, token con TTL y gate antes de lockCupo. Donde diverjo es en el fail-open, el consumo del token y la habilitación manual (detalle abajo).

Bugs

1. Lua de ingreso (C.3) devuelve tipos mezclados: return pos es un string (ZSCORE) y return n es un integer. Con DefaultRedisScript<Long> el reingreso idempotente lanza ClassCastException. Se arregla con return tonumber(pos).
2. Token con doble uso (F.4/F.5). El token se valida al inicio y se consume al final. Dos crearReserva concurrentes con el mismo token pasan ambos.
3. Token quemado antes del commit. crearReserva es @Transactional, así que "consumir tras confirmar" corre antes del commit. Si el flush falla (unique key de asientos), el token se pierde. Hay que consumir en afterCommit, o consumir de forma atómica al inicio y restaurar en afterCompletion si hay rollback.
4. Bucle de turnos. Con el token consumido, la posición sigue siendo ≤ tanda, así que validacion emite uno nuevo. El usuario puede reservar, dejar expirar y repetir. Falta un estado "turno usado", por ejemplo un SET fila:{e}:usados.
5. TTLs inconsistentes. El ZSET se renueva en cada ingreso nuevo, pero tanda solo al habilitar. Pueden expirar por separado: habilitados que vuelven a bloquearse, o seq que se reinicia en 1.
6. Eviction de Redis. redis.conf tiene maxmemory 256mb y volatile-lru, y todas las claves de la fila llevan TTL. Si el stock cache llena memoria, Redis puede desalojar la fila. Probabilidad baja, pero silenciosa y grave, y hay que documentarla.

Decisiones discutibles

- Fail-open en crearReserva. StockCache es fail-open porque es un caché. La fila es control de acceso, y su propósito es evitar que todos golpeen el lock de MySQL. Con Redis caído, el fail-open produce justo eso. La integridad la garantiza MySQL en ambos casos, así que lo que está en juego es la equidad. Yo haría fail-closed (503) también en el gate. Es más simple (sin bitácora especial) y más fácil de testear.
- Habilitación solo manual por ADMIN. No tiene sentido operativo para un evento real, y la prueba de carga depende de un hilo ADMIN, lo que la hace frágil. La regla 16 ("quién habilita") sigue abierta, así que la decisión es tuya.
- estado/ingreso llaman por Feign a msvc-eventos en cada request. El endpoint de polling queda acoplado a la disponibilidad de eventos. Conviene cachear el flag unos segundos.
- "Posición" es el número de turno, no el lugar real. Nadie sale de la fila y los que ya compraron siguen contando. El cliente necesita "N delante", que sale de score - tanda.

Prueba de carga: errores en el plan

- Usa POST /api/ventas/reservas, pero el gateway rutea /api/reservas.
- Pide enviar X-User-Id desde JMeter. El gateway lo inyecta desde el JWT.
- Falta un pool de usuarios (CSV). max-entradas-cliente-evento=6 hace que un mismo usuario reciba 409 muy pronto.
- No aclara que el sector debe ser general. En uno numerado, 50 hilos chocan por los mismos asientos.

Deuda futura

- Mockito sobre ZSetOperations no prueba el Lua, que es la parte crítica. Hace falta un IT con Testcontainers Redis. Con Docker apagado hoy, es difícil.
- 8 commits con docs repetidas: endpoints.md, backlog.md y gateway.md se tocan en 4 commits y se rehacen en FIL-05.

Propuesta mejor

Mantendría el plan, el contrato de 5 endpoints y el orden "carga normal primero", con estos cambios:
1. Arreglar el Lua y agregar el SET usados. Hacer el consumo atómico con restauración por rollback.
2. Gate fail-closed para eventos de alta demanda.
3. Tanda automática configurable (+N cada X segundos, topeada por cupo disponible), con el endpoint ADMIN como override. Cumple "por tandas" sin humano y simplifica el JMeter.
4. Un solo TTL coherente para las 4 claves, sembrado en el ingreso.
5. Fusionar FIL-01 y FIL-02, y reducir FIL-05 a OpenAPI y Postman.
6. Corregir el runbook de carga (rutas, headers, pool de usuarios, sector general).