# Documentación formal de entrega — TicketYa

Esta carpeta es la **documentación que se entrega** (profe / repositorio formal).

La documentación de **trabajo diario del equipo** (backlog, specs vivas, reglas colaborativas, borradores) vive en:

```
Backend/specs/
```

## Contenido (primera entrega)

| Artefacto | Estado | Dónde |
| --- | --- | --- |
| Features y user stories (+ criterios de aceptación) | listo | Tablero Trello del equipo: [US TPE](https://trello.com/b/HhT3lZdK/us-tpe) |
| Arquitectura (módulos, responsabilidades, APIs, comunicación) | listo | [`arquitectura.md`](./arquitectura.md) |
| Diagramas de secuencia de compra | listo | [`diagramas-secuencia.md`](../specs/diagramas/diagramas-secuencia.md) — éxito, pago rechazado, reserva vencida, devolución y entrada numerada |
| Modelo de datos (ER por servicio, relaciones y solapes) | listo | [`modelo-datos.md`](./modelo-datos.md) |
| Diseño de API (endpoints + ejemplos) | listo | [`../specs/endpoints.md`](../specs/endpoints.md) y `<msvc>/docs/endpoints.md` |
| OpenAPI / Swagger | listo | [`openapi/`](./openapi/) |
| Colección Postman | listo | [`postman/`](./postman/) (155 requests, 0 fallos, incluye asientos, control de acceso y reportes) |
| Informe de concurrencia | listo | [`testing/concurrencia.md`](./testing/concurrencia.md) |
| Trazabilidad y bitácora (correlation id, log por servicio) | listo | [`observabilidad.md`](./observabilidad.md) |
| README de ejecución | listo | [`../../README.md`](../../README.md) y [`../README.md`](../README.md) |
| Datos de prueba | listo | seeds en `Backend/infrastructure/` (ver `infrastructure/mysql/README.md`) |

## Regla de oro

1. Se discute y se acuerda en `specs/` (sobre todo `specs/reglas-de-negocio.md` y `specs/backlog.md`).  
2. Cuando esté estable, se **pasa en limpio** a esta carpeta `docs/`.  
3. No inventar reglas en la entrega que no estén en specs o en código.

## Decisiones y limitaciones declaradas (Entrega 1)

- **Asientos numerados:** implementados como asientos virtuales (layout `filas` × `asientosPorFila`; solo se persisten los tomados en `ventas.asiento_reservado`). Los sectores generales siguen vendiéndose por cantidad. Ver `specs/diagramas/diagramas-secuencia.md` §5. Las tablas `asiento` y `precio_asiento` de eventos quedaron sin uso (limpieza pendiente, SEAT-07).
- **Prueba de concurrencia del mismo asiento:** unitaria (lock emulado) y de integración con MySQL real (`AsientoReservadoConcurrenciaIT`, perfil `-Pit`). Ver `testing/concurrencia.md` §3.3.
- **Devoluciones:** el plazo de 72 h se cuenta desde la emisión de la entrada (la consigna dice "antes del evento"). Decisión pendiente en `specs/reglas-de-negocio.md` §9.13.
- **Pago rechazado:** libera el cupo y cancela la reserva; para reintentar hay que crear una reserva nueva.
- **Cancelación de evento:** el aviso de reintegro a entradas es *best-effort*.
