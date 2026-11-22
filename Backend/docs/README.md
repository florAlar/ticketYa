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
| Colección Postman | listo | [`postman/`](./postman/) (216 requests en 15 carpetas; corrida completa 2026-11-21 por el proxy Nginx: 216 requests, 587 assertions, 0 fallos, incluidas las carpetas 13 y 14) |
| Informe de concurrencia | listo | [`testing/concurrencia.md`](./testing/concurrencia.md) |
| Prueba de carga del flujo de compra (`E2-TST-03`) | listo | [`testing/carga.md`](./testing/carga.md) — matriz 25/50/100 normal y con fila; reintentos post-fix sin errores |
| Trazabilidad y bitácora (correlation id, log por servicio) | listo | [`observabilidad.md`](./observabilidad.md) |
| README de ejecución | listo | [`../../README.md`](../../README.md) y [`../README.md`](../README.md) |
| Datos de prueba | listo | seeds en `Backend/infrastructure/` (ver `infrastructure/mysql/README.md`) |
| Índice de pruebas | listo | [`testing/README.md`](./testing/README.md) |

## Contenido adicional (segunda entrega)

| Artefacto | Estado | Dónde |
| --- | --- | --- |
| Consistencia entre servicios y compensaciones (`ARQ-05`) | listo | [`consistencia-y-compensaciones.md`](./consistencia-y-compensaciones.md) |
| Justificación de tecnologías y patrones (`ARQ-06`) | listo | [`decisiones-tecnologicas.md`](./decisiones-tecnologicas.md) |
| Enrutado, rutas públicas y matriz RBAC del Gateway (`E2-SEC-02`) | listo | [`api/gateway.md`](./api/gateway.md) |
| OpenAPI `msvc-notificaciones` (`API-04`) | listo | [`openapi/msvc-notificaciones.json`](./openapi/msvc-notificaciones.json) |
| Verificación de notificaciones | listo | [`testing/notificaciones.md`](./testing/notificaciones.md) |
| Integración entre microservicios (`E2-TST-01`) | hecho | [`testing/integracion-msvc.md`](./testing/integracion-msvc.md) — Niveles 1 y 2 implementados; corrida limpia 2026-11-21: E2E 5/5 e ITs 37 OK |
| Contexto por microservicio (`DOC-MS-00`) | hecho | [ver tabla](#contexto-por-microservicio) — revisión final 2026-11-21 (README + endpoints + OpenAPI) — plantilla: [`../specs/plantilla-docs-msvc.md`](../specs/plantilla-docs-msvc.md) |

## Contexto por microservicio

Documento de contexto de cada servicio (alcance, agregados, modelo local, eventos, criterios y
operación). La plantilla común es [`DOC-MS-00`](../specs/plantilla-docs-msvc.md).

| Microservicio | Contexto | Endpoints | OpenAPI |
| --- | --- | --- | --- |
| `msvc-usuarios` | [`README.md`](../msvc-usuarios/docs/README.md) | [`endpoints.md`](../msvc-usuarios/docs/endpoints.md) | [`msvc-usuarios.json`](./openapi/msvc-usuarios.json) |
| `msvc-eventos` | [`README.md`](../msvc-eventos/docs/README.md) | [`endpoints.md`](../msvc-eventos/docs/endpoints.md) | [`msvc-eventos.json`](./openapi/msvc-eventos.json) |
| `msvc-ventas` | [`README.md`](../msvc-ventas/docs/README.md) | [`endpoints.md`](../msvc-ventas/docs/endpoints.md) | [`msvc-ventas.json`](./openapi/msvc-ventas.json) |
| `msvc-entradas` | [`README.md`](../msvc-entradas/docs/README.md) | [`endpoints.md`](../msvc-entradas/docs/endpoints.md) | [`msvc-entradas.json`](./openapi/msvc-entradas.json) |
| `msvc-preferencias` | [`README.md`](../msvc-preferencias/docs/README.md) | [`endpoints.md`](../msvc-preferencias/docs/endpoints.md) | [`msvc-preferencias.json`](./openapi/msvc-preferencias.json) |
| `msvc-notificaciones` | [`README.md`](../msvc-notificaciones/docs/README.md) | [`endpoints.md`](../msvc-notificaciones/docs/endpoints.md) | [`msvc-notificaciones.json`](./openapi/msvc-notificaciones.json) |

## Regla de oro

1. Se discute y se acuerda en `specs/` (sobre todo `specs/reglas-de-negocio.md` y `specs/backlog.md`).  
2. Cuando esté estable, se **pasa en limpio** a esta carpeta `docs/`.  
3. No inventar reglas en la entrega que no estén en specs o en código.

## Decisiones y limitaciones declaradas (Entrega 1)

- **Asientos numerados:** implementados como asientos virtuales (layout `filas` × `asientosPorFila`; solo se persisten los tomados en `ventas.asiento_reservado`). Los sectores generales siguen vendiéndose por cantidad. Ver `specs/diagramas/diagramas-secuencia.md` §5. Las tablas `asiento` y `precio_asiento` de eventos se eliminaron (SEAT-07).
- **Prueba de concurrencia del mismo asiento:** unitaria (lock emulado) y de integración con MySQL real (`AsientoReservadoConcurrenciaIT`, perfil `-Pit`). Ver `testing/concurrencia.md` §3.3.
- **Devoluciones:** se pueden solicitar **hasta 72 h antes del evento** (fechaInicio − 72 h). La fecha se consulta a msvc-eventos; si no está disponible, la solicitud responde **503**.
- **Pago rechazado:** libera el cupo y cancela la reserva; para reintentar hay que crear una reserva nueva.
- **Cancelación de evento:** el aviso de reintegro a entradas es *best-effort*.
