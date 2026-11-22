# E2-TST-01 — Integración entre microservicios

> **Estado:** `hecho` — Niveles 1 y 2 implementados y verificados con Docker el 2026-11-21 ·
> **Fecha:** 2026-11-21 · **Alcance:** flujos que cruzan dos o más microservicios (Feign + RabbitMQ +
> gateway) · **No incluye:** carga (E2-TST-03) ni autorización por endpoint (E2-TST-02).

## 1. Objetivo

Verificar de punta a punta los flujos que atraviesan más de un microservicio, con infraestructura
real (MySQL/Mongo/Redis/RabbitMQ) y sin mocks de los servicios involucrados en el camino crítico.

La meta no es reemplazar los unitarios ni los ITs por servicio, sino detectar fallas de **contrato,
serialización, routing, idempotencia y consistencia eventual** que un test aislado no ve.

## 2. Alcance

**Dentro del alcance**

- Gateway → servicio → Feign → otro servicio.
- Publicación/consumo por RabbitMQ: `evento.#`, `compra.confirmada`, `devolucion.aprobada`.
- Persistencia cruzada en MySQL, MongoDB y Redis.
- Idempotencia ante reentrega de mensajes.
- Efectos observables: entrada emitida, cupo liberado, historial, notificación, bandeja.
- Smoke de correlación (`X-Correlation-Id`) en el camino completo.

**Fuera del alcance**

- Pruebas de carga y throughput (E2-TST-03).
- Matriz completa de roles (E2-TST-02).
- UI del frontend.
- Pruebas de contrato OpenAPI exhaustivas (ya cubiertas por los specs y unitarios).
- Fallos de infraestructura no reproducibles (caída real de Docker, red, disco).

## 3. Estado actual

| Cobertura existente | Qué prueba | Qué no prueba |
|---|---|---|
| `CupoSectorConcurrenciaIT` / `AsientoReservadoConcurrenciaIT` | MySQL real, locks y unique key de ventas | No cruza servicios |
| `FilaRedisStoreIT` | Redis real, scripts Lua de la fila | No cruza servicios |
| `ValidacionIngresoConcurrenciaIT` | MySQL real, validación QR atómica | No cruza servicios |
| `CompraConfirmadaIngestIT` | Rabbit + Mongo reales para la ingesta de preferencias; `EventosClient` mockeado | No publica desde ventas ni verifica notificaciones |
| `MsvcUsuariosApplicationIT` / `MsvcEventosApplicationIT` / `MsvcNotificacionesApplicationIT` | Contexto completo con MySQL real | No ejecutan flujos entre servicios |
| `JwtGatewayIntegrationTest` / `AutorizacionPorRolMatrixTest` | Filtro JWT y RBAC del gateway con tokens reales | Downstreams mockeados; no es un flujo de negocio completo |
| Postman (carpetas 07, 09, 10, 14) | Flujos reales contra el stack, con login y datos seed | Ejecución manual; no forma parte de `mvn test` ni de CI |

**Brecha original (previa a esta implementación):** no existía un test automatizado que arrancara
dos o más microservicios y verificara un flujo de negocio completo. Los ITs de borde prueban cada
servicio contra su infraestructura, pero no el contrato entre servicios. **Hoy queda cubierta por el
Nivel 2 (`e2e-tests`, F1–F5).**

## 4. Estrategia

Se adopta un enfoque en dos niveles.

### Nivel 1 — ITs de borde con Testcontainers (obligatorio)

Un único servicio Spring Boot por test, con infraestructura real y el servicio remoto mockeado o
reemplazado por un publicador real.

- RabbitMQ real (`rabbitmq:3.13-management`).
- MySQL/Mongo/Redis reales según el servicio.
- El lado remoto se simula publicando el mismo mensaje que produce el servicio real.
- Objetivo: contratos async, idempotencia, DLQ y persistencia local.

Ya existe una base (`CompraConfirmadaIngestIT`). El Nivel 1 implementado agrega:

| Test nuevo | Servicio bajo prueba | Infra real | Remoto simulado |
|---|---|---|---|
| `NotificacionMensajesConsumeIT` | notificaciones | MySQL + Rabbit | publica `evento.*`, `compra.confirmada` y `devolucion.aprobada`; cubre PUSH/EMAIL, matching e idempotencia |
| `CompraConfirmadaPublisherIT` | ventas | MySQL + Rabbit | verifica que el evento se publica **después del commit** y no en rollback |
| `DevolucionAprobadaPublisherIT` | entradas | MySQL + Rabbit | verifica publicación después del commit y no publicación en rollback |

Los publishers se prueban a nivel de `@TransactionalEventListener`, sin levantar el flujo HTTP completo;
eso queda para el Nivel 2.

### Nivel 2 — E2E de stack con Docker Compose (obligatorio para cerrar E2-TST-01)

Un módulo `Backend/e2e-tests` que habla **solo con el gateway** (`http://localhost:8080/api`) y usa
el stack real del compose. No levanta contextos Spring de los servicios: prueba el despliegue real.

- Runner: JUnit 5 + REST Assured (o WebTestClient) + Awaitility.
- Perfil Maven `e2e` (o el script `Backend/scripts/e2e.sh`).
- Ciclo:
  1. `docker compose down -v` (base limpia).
  2. `docker compose up -d --build`.
  3. Esperar healthchecks de bases y `/` de cada app.
  4. Ejecutar los escenarios F1–F5.
  5. `docker compose down -v` (deja el entorno limpio).
- Comando:
  ```bash
  cd Backend
  ./scripts/e2e.sh
  # o: mvn -f e2e-tests/pom.xml -Pe2e verify
  ```
- El test no corre con `mvn test` ni con `-Pit`; requiere Docker y red para las imágenes.

**Decisión:** el Nivel 2 es el que cierra la brecha “entre microservicios”. El Nivel 1 se mantiene
porque da diagnósticos más precisos y no depende de todo el stack.

### Implementación actual del Nivel 1 (2026-11-21)

- `NotificacionMensajesConsumeIT` (5 tests): `evento.cancelado`, `evento.suspendido`,
  `evento.modificado`, `evento.publicado` con matching, `compra.confirmada` y
  `devolucion.aprobada`; verifica filas PUSH/EMAIL, agrupación por `clave_mensaje` e idempotencia.
- `CompraConfirmadaPublisherIT` (2 tests): publicación después del commit y ausencia de mensaje
  en rollback, contra RabbitMQ real.
- `DevolucionAprobadaPublisherIT` (2 tests): misma semántica para `devolucion.aprobada`.
- Los tres corren con `mvn -Pit verify` y `@Testcontainers(disabledWithoutDocker = true)`.
- Corrida real 2026-11-21 con Docker: notificaciones **6 OK**, ventas **22 OK**, entradas **5 OK**
  y preferencias **5 OK** (incluye los ITs de concurrencia de cada módulo).

### Implementación actual del Nivel 2 (2026-11-21)

- Módulo [`Backend/e2e-tests`](../e2e-tests/pom.xml): JUnit 5 + REST Assured + Awaitility.
  Habla solo con el gateway; no levanta contextos Spring.
- `E2EBase`: login por rol, alta de cliente aislado, creación/publicación de eventos, flujo
  reserva → compra → pago, esperas de emisión/historial/notificaciones, MailHog y disponibilidad.
- Escenarios: `CompraConfirmadaE2E` (F1), `DevolucionAprobadaE2E` (F2),
  `EventoNotificacionesE2E` (F3), `EventoPublicadoE2E` (F4) y `ReporteOrganizadorE2E` (F5).
- [`scripts/e2e.sh`](../scripts/e2e.sh): `down -v` → `compose build` → levanta bases e
  infraestructura y espera `healthy` → levanta gateway + 6 msvc → healthchecks HTTP →
  `mvn -f e2e-tests/pom.xml -Pe2e verify` → `down -v` (trap). Soporta `--keep` y `-Dit.test=`.
- Documentación de corrida: [`docs/testing/integracion-msvc.md`](../docs/testing/integracion-msvc.md).
- Corrida real 2026-11-21 sobre volúmenes limpios: **5 tests, 0 fallos, 0 errores, 0 omitidos**
  (`BUILD SUCCESS`).

## 5. Escenarios E2E

### F1 — Compra confirmada (ventas → entradas → notificaciones → preferencias)

1. Login `demo@ticketya.com`.
2. Reservar 2 entradas del evento seed Metallica con `Idempotency-Key`.
3. Confirmar reserva.
4. Crear compra con `Idempotency-Key`.
5. Pagar `APROBAR`.

**Assertions**

- Compra en estado `APROBADA`.
- 2 entradas `VENDIDA` en `GET /api/entradas` (una por asiento/cantidad).
- Historial en `GET /api/clientes/{id}/historial` con la compra.
- Notificación `CompraConfirmada` en `GET /api/notificaciones/me` con `canal=EMAIL`.
- El job de envío la pasa a `ENVIADA` y MailHog tiene el correo.
- `noLeidas` incrementado.
- `X-Correlation-Id` presente en la respuesta del gateway.

### F2 — Devolución aprobada (entradas → ventas → notificaciones)

1. Login ADMIN.
2. Aprobar la devolución seed pendiente.
3. Consultar la entrada y la disponibilidad del sector.
4. Consultar la bandeja del cliente demo.

**Assertions**

- Entrada `DEVUELTA` y QR invalidado.
- Cupo del sector incrementado en 1 (o asiento liberado).
- Notificación `DevolucionAprobada` con `canales=["PUSH","EMAIL"]`.
- Una sola entrada en la bandeja por mensaje.
- MailHog con el correo de devolución.

### F3 — Cancelación / modificación de evento (eventos → notificaciones)

1. Login ORGANIZADOR o ADMIN.
2. Modificar la fecha del evento seed.
3. Suspenderlo.
4. Cancelarlo.

**Assertions**

- Tres mensajes distintos (`EventoModificado`, `EventoSuspendido`, `EventoCancelado`) en la bandeja.
- Cada uno con `canales=["PUSH","EMAIL"]` y una sola entrada.
- Sin duplicados al reenviar el mismo mensaje (idempotencia por `messageId`).

### F4 — Evento publicado + matching (eventos → preferencias → notificaciones)

1. Crear un evento nuevo en Buenos Aires con Metallica.
2. Asociar el participante.
3. Publicarlo.

**Assertions**

- `NuevoEventoArtistaFavorito` una sola vez para el cliente demo.
- `canales=["PUSH","EMAIL"]`.
- Contenido con nombre del evento y ciudad.
- El evento abre cupos en ventas (disponibilidad > 0).

### F5 — Reporte del organizador (ventas → entradas + eventos)

1. Login ORGANIZADOR dueño.
2. Consultar `GET /api/reportes/eventos/{id}` y `/ventas`, `/sectores`, `/ocupacion`, `/recaudacion`.

**Assertions**

- `vendidas + devueltas + vigentes` cierra con el total.
- La recaudación neta = bruta − devuelta.
- Con una dependencia caída (simulada) responde `503`, no un reporte parcial.

### F6 — Fila virtual + reserva (opcional, si el tiempo lo permite)

1. Ingresar a la fila de un evento de alta demanda.
2. Obtener token.
3. Reservar con `tokenFila`.
4. Reutilizar el token.

**Assertions**

- Primera reserva `201`.
- Segunda reserva con el mismo token `409 FILA_TOKEN_INVALIDO`.
- Sin token `409 FILA_TOKEN_REQUERIDO`.
- El cupo no se descuenta en los rechazos.

## 6. Datos e infraestructura

| Recurso | Imagen / versión | Uso |
|---|---|---|
| MySQL | `mysql:8.4` | 5 bases: usuarios, eventos, ventas, entradas, notificaciones |
| MongoDB | `mongo:7` | preferencias |
| Redis | `redis:7` | holds, idempotencia y fila |
| RabbitMQ | `rabbitmq:3.13-management` | eventos de dominio |
| MailHog | `mailhog/mailhog` | verificación de emails |
| Servicios | imágenes del compose | gateway + 6 msvc |

- Datos iniciales: `infrastructure/mysql/*/init.sql`, `infrastructure/mongodb/preferencias/init.js`.
- IDs fijos del seed (evento Metallica, cliente demo, sector Campo, devolución pendiente).
- Cada corrida debe partir de volúmenes vacíos; el script hace `down -v` antes y después.
- Para escenarios que mutan datos, usar ids únicos por corrida (`$timestamp`/UUID) y no depender del
  orden de ejecución.
- Limpiar colas/MailHog entre escenarios o usar ids de correlación para filtrar.

## 7. Estructura implementada

```text
Backend/
├── e2e-tests/
│   ├── pom.xml
│   └── src/test/java/e2e/
│       ├── E2EBase.java          # REST Assured, login, alta de datos, esperas, MailHog
│       ├── CompraConfirmadaE2E.java
│       ├── DevolucionAprobadaE2E.java
│       ├── EventoNotificacionesE2E.java
│       ├── EventoPublicadoE2E.java
│       └── ReporteOrganizadorE2E.java
└── scripts/e2e.sh
```

- El módulo no depende de los `pom.xml` de los servicios; solo de JUnit, REST Assured, Awaitility,
  Jackson y SLF4J.
- El `pom.xml` declara el perfil `e2e` y Failsafe con `**/*E2E.java`; sin perfil, `mvn test` no los
  ejecuta.
- `E2EBase` centraliza: `baseUrl`, login por rol, alta de cliente/evento, flujo de compra, esperas
  de efectos asincrónicos, MailHog y disponibilidad.

## 8. Criterios de aceptación

- [x] **Nivel 1:** ITs de borde con Testcontainers implementados y ejecutados
  (`NotificacionMensajesConsumeIT`, `CompraConfirmadaPublisherIT`, `DevolucionAprobadaPublisherIT`,
  `CompraConfirmadaIngestIT`): 37 tests OK en total.
- [x] **Nivel 2:** módulo `e2e-tests`, escenarios F1–F5 y `scripts/e2e.sh` implementados y
  ejecutados contra un stack limpio.
- [x] F1–F5 pasan contra un stack limpio (`down -v` + `up -d`): 5/5.
- [x] Los tests no usan mocks de los servicios que participan en el flujo.
- [x] Los mensajes Rabbit se verifican por su efecto observable (DB, inbox, MailHog), no solo por el log.
- [x] La reentrega del mismo `messageId`/`compraId`/`devolucionId` no duplica datos (ITs de idempotencia).
- [x] Cada escenario es independiente y repetible.
- [x] El fallo de una dependencia se refleja en el status esperado (`503`) y no deja datos parciales
  (cubierto por `ReporteServiceTest` y `ReporteControllerTest` de ventas: excepción propia + cuerpo
  de error estándar; el E2E no tumba dependencias a mitad de corrida).
- [x] El script deja el entorno limpio al terminar (incluso si falla).
- [x] `mvn test` sigue sin Docker; `mvn -Pe2e verify` (o `scripts/e2e.sh`) es el único que levanta el stack.
- [x] Se documenta la corrida en `docs/testing/integracion-msvc.md` con fecha, comandos y resultados.

## 9. Evidencia

- Reporte Failsafe/Surefire por escenario.
- Logs del gateway y servicios con el mismo `X-Correlation-Id`.
- Consultas a MySQL/Mongo/Redis o endpoints de verificación.
- API de MailHog (`/api/v2/messages`) para emails.
- Panel/API de Rabbit para colas y DLQ.

## 10. Plan de implementación

| Fase | Tarea | Estimación |
|---|---|---|
| T1 | Módulo `e2e-tests`, perfil `e2e`, script `e2e.sh` y health wait | 4–6 h |
| T2 | F1 compra confirmada | 4–6 h |
| T3 | F2 devolución aprobada | 3–4 h |
| T4 | F3 evento modificado/suspendido/cancelado | 3–4 h |
| T5 | F4 evento publicado + matching | 3–4 h |
| T6 | F5 reporte del organizador | 2–3 h |
| T7 | Documento de resultados y ajuste de backlog | 2 h |
| T8 (opcional) | F6 fila virtual + reserva | 3–4 h |

**Total:** 2–4 días de trabajo, dependiendo del tiempo de arranque del stack y de la estabilidad de
los healthchecks.

## 11. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Flakiness por arranque de 17 contenedores | esperar healthchecks y reintentos con Awaitility; timeout configurable |
| Docker no disponible en el entorno | `mvn test` no depende de Docker; los E2E se ejecutan en local/CI con Docker |
| Datos seed mutados entre corridas | `down -v` antes y después; ids únicos por corrida |
| MailHog/Rabbit tardan en reflejar el mensaje | polling con timeout, nunca `sleep` fijo |
| Costo de build de imágenes | cachear capas; en CI usar `docker compose build` una vez |
| El test E2E se vuelve lento | separar por escenarios y permitir `-Dgroups=...` o tags JUnit |

## 12. Definición de hecho

`E2-TST-01` pasa a `hecho` cuando:

1. Existe el módulo/script E2E y corre con un comando documentado.
2. F1–F5 están verdes en una corrida limpia.
3. Hay evidencia en `docs/testing/integracion-msvc.md`.
4. El backlog y el README de testing quedan actualizados.
