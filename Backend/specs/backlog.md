# TicketYa — Backlog

Estado: `hecho` | `parcial` | `pendiente`  
Alcance de entregas según `docs/consigna.md`.  
Este archivo es la lista de trabajo del repo; no reemplaza la consigna.

---

## Leyenda de entregas

| Entrega | Foco |
| --- | --- |
| E1 | Core: eventos, lugares, publicación, búsqueda, reserva temporal, compra, pago mock, emisión QR, no doble venta, devoluciones, cancelaciones. Sin auth formal (id por header/param). Apps en local; DBs pueden ir en Docker. |
| E2 | Fila virtual, control de acceso QR, preferencias/recs (NoSQL), notificaciones, reportes, JWT+roles, cache, resiliencia, trazabilidad, compose completo, tests integración/seguridad/carga. |
| Transversal | Infra, docs de repo, higiene — sirve a ambas entregas. |

---

## 0. Infraestructura y scaffolding del monorepo (hecho / parcial)

Tareas de base ya encaradas. No son features de negocio; desbloquean E1/E2.

| ID | Tarea | Entrega | Estado | Notas |
| --- | --- | --- | --- | --- |
| INF-01 | Monorepo `Frontend/` + `Backend/` + `.gitignore` + `.env.example` | Transversal | hecho | FE ya no vacío: ingresó SPA React (`ticketya-client`) en `Frontend/`. |
| INF-02 | Proyectos Spring Boot independientes: `APIGateway`, `msvc-usuarios`, `msvc-eventos`, `msvc-ventas`, `msvc-entradas`, `msvc-preferencias`, `msvc-notificaciones` | Transversal | hecho | Cada uno con `pom.xml` propio. |
| INF-03 | Árbol de paquetes por msvc (controller, service, repository, entity\|document, dto, config, security/redis/queue donde aplica) | Transversal | hecho | Entidades/repos MySQL alineados al init (INF-27). Services/DTOs de negocio aún stubs. |
| INF-04 | Starters alineados al diseño: JPA+MySQL (usuarios/eventos/ventas/entradas/notificaciones), Mongo Data (preferencias), Redis (ventas), Spring Cloud Gateway, Security en usuarios | Transversal | hecho | Preferencias: `spring-boot-starter-data-mongodb` + docs alineados a init. |
| INF-05 | Smoke `GET /` → texto ok en gateway y todos los msvc (incl. notificaciones) | Transversal | hecho | Evita Whitelabel; no es API de negocio. Notifs a veces 404 en `/` según imagen/docker — revisar al cablear. |
| INF-06 | `Dockerfile` multi-stage por app (gateway + msvcs) | Transversal | hecho | |
| INF-07 | `docker-compose.yml` por msvc (app + DB) con nombres `ms-*` / `db-*`, healthchecks y `depends_on` | Transversal | hecho | Puertos host de DB únicos. MySQL monta `infrastructure/mysql/*/init.sql` en `docker-entrypoint-initdb.d`. |
| INF-08 | Compose raíz `Backend/docker-compose.yml` + `infrastructure/docker-compose.yml` con `include` | Transversal | hecho | Orquestación lista a nivel estructura. |
| INF-09 | Matriz de puertos apps 8080–8086 y DBs documentada en `docs/infraestructura.md` | Transversal | hecho | Incluye diagrama Docker Mermaid. |
| INF-10 | Carpeta `infrastructure/` (mysql/mongodb/redis/nginx + readme) | Transversal | hecho | MySQL init + Mongo init + **redis.conf** montado en `db-redis-ventas` + nginx conf → `ms-gateway`. |
| INF-11 | `SecurityConfig` temporal `permitAll` donde hay starter Security (smoke temprano) | Transversal | hecho | Cerrar en E2 con JWT. |
| INF-12 | Docs transversales base: `arquitectura.md`, `estructura.md`, `infraestructura.md`, `bases-de-datos.md`, `consigna.md` + `comunicacion.md` / `microservicios.md` | Transversal | hecho | Higiene fina de `arquitectura`/`bases-de-datos` (notifs) sigue en INF-19. |
| INF-13 | `docs/endpoints.md` — listado de rutas, enrutado gateway y solapes `/eventos/**` | E1/E2 | parcial | Faltan params, status HTTP, JSON de ejemplo y OpenAPI. FE alineado en `Frontend/docs/endpoints-front.md` ↔ back. |
| INF-14 | `docs/diagramas/modelo.md` — ER Mermaid **solo MySQL** (preferencias fuera) | E1 | hecho | Preferencias/historial sacados del ER; FKs explícitas en atributos + relaciones. Mongo → `modelo-mongodb.md`. |
| INF-15 | Seeds / init de datos de prueba por contexto | E1 | hecho | Via `infrastructure/mysql/*/init.sql` (no DataLoader Java). UUIDs fijos cross-msvc documentados en `mysql/README.md`. Passwords BCrypt demo. |
| INF-16 | README raíz / Backend con instrucciones de ejecución verificables | E1 | hecho | `Backend/README.md` + README monorepo (light): compose, puertos, smoke curl, link a docs. |
| INF-17 | Colección Postman (o similar) de endpoints | E1 | hecho | `docs/postman/` (colección v2.1 E1 + environment local). Actualizar en E2. Sin ejecutar en verificación 2026-11-10 (Docker apagado). |
| INF-18 | Higiene compose preferencias: URI Mongo con `${MONGO_PASSWORD:-…}` (sin literal frágil) | Transversal | hecho | `msvc-preferencias/docker-compose.yml` usa `${MONGO_PASSWORD:-mongo_dev}`. |
| INF-19 | Alinear docs (`arquitectura`, `bases-de-datos`) con `msvc-notificaciones` y conteo real de servicios | Transversal | pendiente | |
| INF-20 | Ingreso SPA cliente en `Frontend/`: React 19 + Vite + Router, `src/` (api/auth/pages/routes/domain/…), `package.json`, `.env.example` (`VITE_API_BASE_URL`, `VITE_USE_MOCK`) | Transversal | hecho | Copia del simulacro; `node_modules`/`dist` locales presentes. |
| INF-21 | README de ejecución del front (`Frontend/README.md`) + scripts npm (`dev`/`build`/`lint`) | Transversal | hecho | No sustituye README raíz/Backend (INF-16). |
| INF-22 | Contrato front documentado: `Frontend/docs/endpoints-front.md` + `endpoints-back.md` | Transversal | hecho | Paths front alineados al contrato back mandatorio. |
| INF-23 | Facades `src/api/*` + capa mock LS (`VITE_USE_MOCK=true` por defecto) con misma forma de llamada que gateway | Transversal | hecho | Demock **selectivo** vía `VITE_USE_MOCK_AUTH/EVENTOS/COMPRAS/ENTRADAS=false` (`.env` típico demock). Toast cuentas demo siempre en `/login` (mock o seed gateway) + reopen. Global mock residual: admin usuarios, scan E2, transferencia. |
| INF-24 | Shell UI por roles (login/register, home, eventos, checkout, mis-entradas, soporte, gestión, acceso/scan, 404) | Transversal | hecho | Flujos con mock; varios paneles son shell hasta API real. |
| INF-25 | `Dockerfile` multi-stage del Frontend + servicio en compose/nginx del monorepo | Transversal | hecho | `Frontend/Dockerfile` + `ms-frontend` :3000; conf nginx proxy `/api/` → `ms-gateway`. Include en `Backend/docker-compose.yml`. |
| INF-27 | **Spring reconoce init.sql** (datasource + JPA validate + entidades/repos = schema) | E1 | hecho | 5 msvc MySQL: `ddl-auto=validate`, JDBC a `db-mysql-*`, entidades/repos al init, arranque OK + smoke `GET /`. Seeds solo init.sql. Verificado 2026-11-06. |
| INF-28 | init Mongo preferencias (`infrastructure/mongodb/preferencias/init.js`) colecciones + índices según `modelo-mongodb.md` | E2 | hecho | Colecciones `preferencias_clientes` + `interacciones_eventos`; índices idCliente/idEvento; seed demo alineado a UUIDs MySQL. Montado en `db-mongo-preferencias`. URI compose con `${MONGO_PASSWORD:-mongo_dev}`. Verificado mongosh 2026-11-06. |

---

## 1. Documentación por microservicio (specs)

Punto aparte: cada msvc (y el gateway) tiene `docs/` propio. Ahí viven las **specs del contexto** (OpenSpec u otro), contratos del bounded context, decisiones DDD y lo que alimenta OpenAPI/tests.  
Hoy esas carpetas están vacías (`.gitkeep` o sin contenido útil).

| ID | Tarea | Entrega | Estado | Notas |
| --- | --- | --- | --- | --- |
| DOC-MS-00 | Definir plantilla común de `docs/` por msvc (alcance del contexto, agregados, API pública, modelo local, eventos/mensajes, criterios de aceptación, cómo correr) | Transversal | pendiente | Una sola convención para no divergir. |
| DOC-MS-GW | Specs + diseño de `APIGateway/docs/` (rutas, filtros, correlación, qué no hace el gateway) | E1 | pendiente | |
| DOC-MS-EVE | Specs + diseño de `msvc-eventos/docs/` | E1 | parcial | `endpoints.md` msvc vivo (alta/publicación/cupo Feign). Falta plantilla DOC-MS-00 completa + OpenAPI. |
| DOC-MS-VEN | Specs + diseño de `msvc-ventas/docs/` | E1 (+ fila E2) | parcial | `endpoints.md` msvc vivo (+ POST cupos). Falta plantilla completa + fila E2. |
| DOC-MS-ENT | Specs + diseño de `msvc-entradas/docs/` | E1 (emisión/devo) / E2 (validación acceso) | parcial | `endpoints.md` msvc con emisión/QR/devo y validación de acceso (E2-ACC-01). |
| DOC-MS-USU | Specs + diseño de `msvc-usuarios/docs/` | E1 (mínimo id) / E2 (JWT) | parcial | `endpoints.md` auth JWT. Falta plantilla DOC-MS-00 completa. |
| DOC-MS-PRE | Specs + diseño de `msvc-preferencias/docs/` (documentos Mongo, recs) | E2 | pendiente | Incluir justificación NoSQL. Modelo doc: MOD-04. |
| DOC-MS-NOT | Specs + diseño de `msvc-notificaciones/docs/` | E2 | pendiente | Canales, plantillas, disparos async. |
| DOC-MS-SYNC | Mantener specs versionadas en Git y usarlas como guía antes de implementar cada feature (flujo Spec-Driven de la consigna) | Transversal | pendiente | Propuesta → requisitos → diseño → tareas → código. |

---

## 2. Modelo de datos

| ID | Tarea | Entrega | Estado | Notas |
| --- | --- | --- | --- | --- |
| MOD-01 | ER global MySQL en `docs/diagramas/modelo.md` (Mermaid) | E1 | hecho | Sin preferencias/historial (Mongo). FKs explícitas. Link a `modelo-mongodb.md`. |
| MOD-02 | Partir / complementar el modelo en **un DER (o vista) por microservicio** con claves y ownership de datos | E1 | hecho | `specs/diagramas/modelo.md` reescrito (2026-11-10): `classDiagram` global con un `namespace` por msvc (5 MySQL + Mongo), columnas reales verificadas contra `init.sql` y entidades JPA. + `modelo-por-microservicio.md`: 5 `erDiagram` (uno por msvc MySQL; Mongo en `modelo-mongodb.md`) con refs lógicas a entidades externas. Sintaxis Mermaid validada; render visual a revisar en el visor. |
| MOD-03 | Documentar **entidades solapadas** entre BDs (mismos IDs lógicos sin FK cruzada: `idEvento`, `idCliente`, etc.) | E1 | hecho | Las 19 referencias lógicas entre msvc quedan dibujadas como líneas punteadas (columnas `REF`) en el diagrama de `modelo.md` (incluye `asiento_reservado`). |
| MOD-04 | **Modelo de preferencias en MongoDB** (`docs/diagramas/modelo-mongodb.md`) | E2 | hecho | Doc + init.js INF-28. Código Spring Data / recs = E2-PRE-*. |
| MOD-05 | Modelo local notificaciones (MySQL) alineado a ER + `notificaciones/init.sql` | E2 | hecho | Init + JPA (Plantilla/Notificacion/IntentoEnvio) en INF-27. Specs DOC-MS siguen pendientes. |
| MOD-06 | Actualizar diagramas E2 cuando entren fila, validaciones, notifs y recs | E2 | pendiente | |

---

## 3. Producto — Features, user stories y criterios de aceptación

| ID | Tarea | Entrega | Estado | Notas |
| --- | --- | --- | --- | --- |
| US-01 | Features + user stories de **todo el sistema** (E1+E2) con roles CLIENTE / ORGANIZADOR / ADMIN / PERSONAL_ACCESO | E1 | hecho | Artefacto de consigna §5.A. Se mantiene en el tablero Trello del equipo (enlazado en el README raíz). |
| US-02 | Criterios de aceptación por historia del núcleo E1 | E1 | hecho | En el tablero Trello del equipo, junto a las historias. |
| US-03 | Completar/actualizar CA de historias E2 y de las que cambien | E2 | pendiente | |

---

## 4. Arquitectura y diseño (docs transversales)

| ID | Tarea | Entrega | Estado | Notas |
| --- | --- | --- | --- | --- |
| ARQ-01 | Definir responsabilidades finales de cada msvc y APIs públicas (cierre de `microservicios.md` / `comunicacion.md`) | E1 | pendiente | Scaffold de nombres ya existe. |
| ARQ-02 | Diagrama de módulos de la arquitectura | E1 | hecho | `docs/arquitectura.md` (Mermaid, con las llamadas Feign reales entre servicios). Complementa el diagrama Docker (`diagramas/img.png`). |
| ARQ-03 | Diagrama de secuencia: compra de entrada **numerada** (éxito) | E1 | hecho | `diagramas/diagramas-secuencia.md` §1 (compra exitosa) y §5 (asiento concreto: `asiento_reservado`, `RESERVADO` → `VENDIDO`, errores 400/409, devolución). Flujo (SEAT-01…05): elegir asientos → `POST /reservas` con `asientos` (lock de cupo + `asiento_reservado`) → compra → pago → emisión de una entrada por asiento. |
| ARQ-04 | Misma secuencia: **pago rechazado** y **vencimiento de reserva** | E1 | hecho | `diagramas/diagramas-secuencia.md` §2 (pago rechazado) y §3 (vencimiento por job y lazy). Quedan en diagramas separados; la consigna pide uno solo que contemple ambas variantes, pendiente de consolidar. |
| ARQ-05 | Estrategias de consistencia entre servicios y compensaciones (saga / pasos de deshacer) | E2 | pendiente | |
| ARQ-06 | Justificar tecnologías (Mongo, Redis, Gateway, etc.) y patrones usados | E2 | pendiente | |

---

## 5. Diseño de API REST y OpenAPI

| ID | Tarea | Entrega | Estado | Notas |
| --- | --- | --- | --- | --- |
| API-01 | Completar `endpoints.md`: método, ruta, params, roles (E1: header), códigos de respuesta | E1 | pendiente | Base de rutas ya escrita. |
| API-02 | Ejemplos JSON request/response de flujos E1 (eventos, reserva, compra, entradas, devolución) | E1 | pendiente | |
| API-03 | OpenAPI / Swagger por msvc o agregado vía gateway — descripciones, ejemplos, errores | E1 | pendiente | |
| API-04 | Actualizar OpenAPI con endpoints E2 y security JWT | E2 | pendiente | |
| API-05 | Implementar en Gateway el enrutado real según `endpoints.md` (incl. excepciones `/eventos/{id}/disponibilidad`, `/cupos`, `/fila/**`, `/validaciones`, `/recomendaciones`) | E1 (rutas core) / E2 (resto) | hecho | Routes en `application.yml` (Gateway MVC). StripPrefix `/api`. Excepciones order 10–13 (+ **cupos POST** → ventas). Compose: `TICKETYA_URI_*`. Doc: `APIGateway/docs/endpoints.md`. |

---

## 6. Entrega 1 — Implementación del núcleo

Identificación E1: sin JWT formal; `X-User-Id` / header de rol (o equivalente) documentado en specs.

**Prerrequisito de implementación:** INF-27 (Spring + init) antes de abrir GETs de negocio.

### 6.1 msvc-eventos

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| E1-EVE-00 | Datasource + JPA validate contra `eventos/init.sql` (parte de INF-27) | hecho | EMF OK; JDBC `db-mysql-eventos`. |
| E1-EVE-01 | Entidades JPA reales alineadas al init: Evento, Lugar, Sector, Asiento, PrecioSector/PrecioAsiento (+ Participante) | hecho | + Participante, EventoParticipante (2026-11-07). `Sector` suma `filas`/`asientosPorFila` (SEAT-01); `Asiento` y `PrecioAsiento` se eliminaron (SEAT-07). |
| E1-EVE-02 | CRUD lugares y sectores; asientos en sector numerado | hecho | GET/POST/PUT lugares; POST `/lugares/{id}/sectores`; PUT `/sectores/{id}`; GET asientos **virtuales** calculados desde el layout del sector (SEAT-01; antes: asientos físicos del seed). Sector numerado exige `filas` + `asientosPorFila` y deriva la capacidad. Alta embebida también desde POST evento. |
| E1-EVE-03 | Eventos en borrador; configuración sectores/precios; publicación con validaciones | hecho | POST/PUT/DELETE evento; PUT precio sector; POST publicacion. **2026-11-07:** borrador laxo (nombre; sin límite fecha); publicar exige lugar real + ≥1 sector precio + **capacidad ≥ 1** (no cupo ilimitado). Al publicar: Feign ventas `POST /eventos/{id}/cupos` **fail-hard**. Body alineado form FE. |
| E1-EVE-04 | **GETs** búsqueda/catálogo: tipo, ciudad, fechas; orden; paginación — `GET /eventos`, `GET /eventos/{id}` | hecho | + `estado=TODOS` para gestión admin. Default PUBLICADO. + filtro `participante=` (id artista). Doc: `msvc-eventos/docs/endpoints.md`. |
| E1-EVE-05 | Patch estado (suspender/cancelar) y fecha; listado organizador | hecho | PATCH estado/fecha; GET `/organizadores/me/eventos`. CANCELADO → Feign reintegro entradas best-effort. ORG: SUSPENDIDO propios; resto ADMIN. |
| E1-EVE-06 | Participantes (artistas/bandas) y asociación a evento | hecho | Schema `participante`+`evento_participante`; CRUD `/participantes`; GET/PUT `/eventos/{id}/participantes`; filtro `GET /eventos?participante=`. Authz: GET público; write ORG/ADMIN; asociar dueño/ADMIN. FE: autocomplete catálogo (guarda **id**) + CTA crear + principal obligatorio / teloneros +. Seed demo: Metallica/Gojira, UCL (equipos), Iron Maiden/Ghost. |

### 6.2 msvc-ventas

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| E1-VEN-00 | Datasource + JPA validate contra `ventas/init.sql` (INF-27) | hecho | EMF OK; Redis host compose ok. |
| E1-VEN-01 | Modelo: CupoSector, Reserva, DetalleReserva, Compra, Pago (+ asientos reservados si numerado) | hecho | 5 entidades + repos. `AsientoReservado` + tabla `asiento_reservado` agregadas en SEAT-02 (asientos virtuales; solo los tomados). |
| E1-VEN-02 | Disponibilidad comercial por evento | hecho | `GET /eventos/{id}/disponibilidad` → cupo_sector. Doc msvc. |
| E1-VEN-02b | Apertura de cupo comercial al publicar evento | hecho | `POST /eventos/{id}/cupos` body `{sectores:[{idSector,capacidadTotal≥1}]}` idempotente. Feign desde msvc-eventos en `doPublicar`. GW: excepción `Path=/api/eventos/*/cupos` Method=POST → ventas. |
| E1-VEN-03 | Reserva temporal (TTL p.ej. 10 min), confirmación, cancelación, renovación | hecho | PENDIENTE/CONFIRMADA; job expire 30s + lazy; Redis hold. |
| E1-VEN-04 | Compra desde reserva + **Idempotency-Key**; límite de entradas por cliente/evento | hecho | Key obligatoria (Redis 24h); max 6/cliente/evento. |
| E1-VEN-05 | Pasarela de pagos **mock** (aprueba/rechaza) | hecho | `POST /compras/{id}/pagos` body resultado APROBAR\|RECHAZAR. |
| E1-VEN-06 | Mecanismo **anti doble venta** (restricciones unicidad, locking/versionado y/o Redis) usable con varias instancias | hecho | PESSIMISTIC_WRITE cupo + UK evento/sector; Redis hold auxiliar. |
| E1-VEN-07 | Liberación de cupo/asiento si pago rechazado o reserva vencida | hecho | Reject/cancel/expire liberan reservada→disponible y borran los `asiento_reservado` de la reserva (SEAT-02). Pago aprobado → asientos VENDIDO. |
| E1-VEN-08 | Cableado Redis según decisión de diseño (stock/TTL); hoy clases vacías | hecho | `StringRedisTemplate` + `StockCache` (hold + idempotency). starter data-redis. |

### 6.3 msvc-entradas

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| E1-ENT-00 | Datasource + JPA validate contra `entradas/init.sql` (INF-27) | hecho | EMF OK. |
| E1-ENT-01 | Modelo Entrada (QR único), Devolucion, ValidacionAcceso | hecho | 3 entidades + repos. ValidacionAcceso con API desde E2-ACC-01. Seed intacto. |
| E1-ENT-02 | Emisión de entradas con QR tras compra confirmada | hecho | `POST /entradas/emision`; Feign ventas post-pago con **reintentos + emision_estado PENDIENTE/EMITIDA/FALLIDA + job** (ya no fire-and-forget). Idempotente por idCompra. **`entrada.precio` persistido**. |
| E1-ENT-03 | **GET** entradas del cliente (`GET /entradas`, `GET /entradas/{id}`) | hecho | + GET `/{id}/qr`. DTO enriquece `eventoNombre`/`lugar*`/`sectorNombre` vía Feign msvc-eventos. **GET /{id}: dueño o ADMIN** (un método). FE admin: `/gestion/entradas/:id` (no mis-entradas). |
| E1-ENT-04 | Solicitud de devolución (plazo 72 h, no usada); approve/reject admin; liberación + reintegro mock | hecho | 72h desde **emisión**; ADMIN→DEVUELTA + **Feign ventas libera 1 cupo** fail-hard. Monto desde `entrada.precio`. FE admin cola: `USE_MOCK_ENTRADAS`. |
| E1-ENT-05 | Efecto de cancelación de evento sobre entradas vendidas (reintegro) — al menos contrato E1 | hecho | `POST /eventos/{id}/reintegro-cancelacion` → A_REINTEGRAR. No notifs; no devuelve cupo a ventas. |

### 6.4 API Gateway y usuarios (mínimo E1)

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| E1-USU-00 | Datasource + JPA validate contra `usuarios/init.sql` (INF-27) | hecho | Usuario+Rol+M2M; seed 4+4 intacto. |
| E1-GW-01 | Rutas E1: auth/usuarios (si aplica), eventos/lugares/…, reservas/compras/pagos, entradas/devoluciones | hecho | También preferencias/notifs + excepciones `/eventos/**`. Ver `APIGateway/src/main/resources/application.yml`. |
| E1-GW-02 | Propagar identidad (headers) a msvcs | hecho | Tras JWT: `X-User-Id`, `X-User-Email`, `X-User-Role`, `X-User-Roles`. Filtro `JwtAuthenticationFilter` (patrón gatewayejemplo → MVC). |
| E1-GW-03 | CORS hacia origen Vite (dev) | hecho | `SecurityConfig` CorsConfigurationSource: :5173 + :3000 (`TICKETYA_CORS_ORIGINS`). |
| E1-USU-01 | Identidad mínima E1 (alta/login + JWT) | hecho | Auth login/register/refresh/logout + me + admin list/estado/roles. JwtUtil emisión (secret compartida GW). Shape FE `{token,user}`. Doc: `msvc-usuarios/docs/endpoints.md`. |

### 6.5 Testing y entregable E1

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| E1-TST-01 | Unitarias: compra, devoluciones, búsqueda | hecho | JUnit5+Mockito por msvc. Verificado 2026-11-10: gateway 23, usuarios 51, eventos 101, entradas 48 OK. Ventas 109 OK (re-verificado, ver HIG-04). Preferencias/notificaciones sin unitarias reales (E2, HIG-01/03). |
| E1-TST-02 | Prueba de concurrencia: mismo asiento / último cupo → una sola venta exitosa | hecho | Unitario + IT Testcontainers MySQL (`CupoSectorConcurrenciaIT`) + informe `docs/testing/concurrencia.md`. IT no re-ejecutado 2026-11-10 (requiere Docker). **Por asiento:** `ReservaAsientoConcurrenciaTest` (20 hilos, mismo asiento → 1 éxito; lock emulado); IT con MySQL real pendiente (SEAT-06). |
| E1-REL-01 | Datos de prueba + Postman + README ejecución del núcleo | hecho | Datos = un único `init.sql` por base (INF-15); Postman = INF-17; README raíz con arranque por Docker Compose y `npm run dev` (2026-11-11). |

### 6.6 Higiene detectada en verificación 2026-11-10

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| HIG-01 | `msvc-notificaciones`: `contextLoads` falla sin MySQL (único test del módulo) | pendiente | Hacerlo independiente de DB (slice/H2/mock) o reemplazar por unitarias reales. |
| HIG-02 | Frontend `npm run lint`: 12 errores (3 `opts` sin usar, 3 `react-refresh/only-export-components`, 6 `setState` síncrono en effect) | pendiente | `npm run build` OK. 2026-11-11: siguen los mismos 12 errores preexistentes; los cambios de esa fecha no agregaron ninguno. |
| HIG-03 | `msvc-preferencias`: sin tests | pendiente | Hacerlo junto con E2-PRE-*. |
| HIG-04 | Entorno local: `~/.m2/.../org.jacoco.build-0.8.8.pom` truncado → `mvn test` en ventas no resolvía jacoco | hecho | Causa: la corrida en paralelo de 7 `mvn` (2026-11-10) escribió a la vez el mismo archivo del repo local. Re-ejecutado en solitario: ventas 109 tests OK. Evitar correr varios `mvn` en paralelo sobre el mismo `~/.m2`. |
| HIG-05 | READMEs raíz/Backend: texto "sin API de negocio aún / scaffold" desactualizado | hecho | README raíz y de Frontend reescritos (2026-11-11); ya no dicen "sin API de negocio aún". |
| HIG-06 | Verificación con stack Docker (smoke, IT Testcontainers, compose completo) no hecha en 2026-11-10 | hecho | 2026-11-11: stack recreado desde cero (`down -v` + `up -d --build`): 15 contenedores Up, bases healthy, sin errores de arranque; login JWT real, reserva por asiento (201), misma reserva (409) y cancelación por el gateway; front en :3000 con proxy `/api`. No se re-ejecutaron los IT Testcontainers ni la colección Postman completa. |
| HIG-07 | Un único `init.sql` por base; eliminar `migrations/` (02, 03, 04) | hecho | 2026-11-11: los `init.sql` ya incluían todo el esquema y el seed; los `03-reseed` eran idénticos al seed salvo las tablas de SEAT-07. Cambio de esquema = recrear volumen (`docker compose down -v`). README de mysql y docs actualizados. |

### 6.7 Asientos virtuales (sectores numerados)

Decisión (2026-11-10): sector numerado se vende por asiento concreto; sector general sigue por cupo. Los asientos son **virtuales**: el sector guarda `filas` x `asientos_por_fila` y solo se persiste el asiento **tomado** (`asiento_reservado`), nunca los libres. `asiento` / `precio_asiento` quedan sin uso (limpieza a decidir); el precio es por sector.

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| SEAT-01 | eventos: `sector.filas` + `asientos_por_fila`; capacidad = filas x asientosPorFila; `GET .../asientos` calculado | hecho | `eventos/init.sql`. Límites 1..200 filas, 1..500 por fila. Tests eventos 119 OK. |
| SEAT-02 | ventas: tabla `asiento_reservado` (única por evento+sector+fila+número); reservar por asiento; liberar al cancelar/rechazar/expirar; VENDIDO al pagar; `asientosOcupados` en disponibilidad; liberar asiento en devolución | hecho | `ventas/init.sql`. Lock de cupo + pre-chequeo + clave única como 2ª defensa. Tests ventas 139 OK. |
| SEAT-03 | entradas: `entrada.fila` + `numero`; emisión una entrada por asiento; devolución libera el asiento en ventas | hecho | `entradas/init.sql`. DTO expone `fila` y `asiento`. Tests entradas 54 OK. |
| SEAT-04 | Gateway / OpenAPI / Postman | hecho | Gateway sin cambios (las rutas existentes cubren todo). OpenAPI regenerado el 2026-11-10 con los asientos. Postman: carpeta `09 Asientos numerados` (23 requests: reserva por asiento, 409/400, pago aprobado/rechazado, entrada con fila y número, devolución). Colección completa 127/376/0 con el flag en `false` y en `true`. |
| SEAT-05 | Frontend: selector de asientos + enviar `filas`/`asientosPorFila` al crear sector numerado | hecho | Forms Nuevo/Editar (filas × asientos, cupo derivado de solo lectura, borrador laxo / publicación estricta); `SeatPicker` en dos pasos (fila → asientos, hasta 6); 409 quita solo los asientos en conflicto; detalle muestra layout; mock mínimo. Verificado: lint sin errores nuevos, build OK, 15 checks de dominio y render del picker. **No** probado contra el BE real (Docker apagado). |
| SEAT-06 | IT con MySQL real del flujo de asientos (unique key, rollback de contadores) | hecho | `AsientoReservadoConcurrenciaIT` (4 casos, perfil `-Pit`, carga el `init.sql` real). Verificado quitando la clave única: fallan los casos que la ejercitan. Ver `docs/testing/concurrencia.md` §3.3. |
| SEAT-07 | Limpieza: eliminar `asiento`, `precio_asiento` (tablas, entidades, repos) | hecho | Quitadas de `init.sql`, entidades y repos de eventos, y de los 3 modelos (`modelo.md`, `modelo-por-microservicio.md`, `docs/modelo-datos.md`). El endpoint calculado `GET /lugares/{id}/sectores/{sectorId}/asientos` se mantiene. Tests eventos 119 OK. |
| SEAT-08 | Documentación actualizada | hecho | `reglas-de-negocio.md` (reglas + pregunta 6 resuelta), `documentacion-y-validacion.md` (D-16/R-05, changelog 1.2), `endpoints.md`, `msvc-{eventos,ventas,entradas}/docs/endpoints.md`, `infrastructure/readme.md` + `mysql/README.md` (migraciones y asientos virtuales), `docs/testing/concurrencia.md`, `docs/postman/README.md`, `docs/openapi/README.md`, `Frontend/docs/endpoints-{back,front}.md`, diagramas (`modelo.md`, `modelo-por-microservicio.md`). `consigna.md` no se toca. |

---

## 7. Entrega 2 — Funcionalidades restantes y NFR

### 7.1 Funcional

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| E2-FILA-01 | Fila virtual (alta demanda): ingreso, posición, tandas, token de compra | pendiente | Simplificable con justificación. |
| E2-ACC-01 | Control de acceso QR: validar existencia, evento, no reutilizada; marcar usada | hecho | 2026-11-11. `POST /eventos/{id}/validaciones` (PERSONAL_ACCESO o ADMIN): 200 `VALIDA` (entrada → USADA) o 409 `YA_USADA` / `OTRO_EVENTO` / `NO_VALIDA` / `EVENTO_CANCELADO`; 404 QR inexistente; 400 QR vacío. Atómico por QR (`SELECT … FOR UPDATE`), probado con MySQL real (8 puertas, un solo ingreso; `ValidacionIngresoConcurrenciaIT`, `-Pit`). Todo intento sobre una entrada existente queda en `validacion_acceso`. `GET /eventos/{id}/validaciones` para ADMIN y ORGANIZADOR dueño. Tests entradas 74 OK; Postman carpeta `10 Control de acceso QR` (19 requests, sin fallos). FE: pantalla de escaneo con selector de evento (gate `accesoScan`). **Simplificación:** cualquier PERSONAL_ACCESO valida cualquier evento (ver X-ACC-01). |
| E2-PRE-01 | Preferencias cliente en **Mongo** (categorías, ciudad, rango precio, artistas) | pendiente | Init INF-28 hecho; falta dominio Spring Data + API. |
| E2-PRE-02 | Recomendaciones (propias y/o similares por evento); puede ser simplificado | pendiente | |
| E2-PRE-03 | Historial de cliente / ingesta desde compras o eventos de dominio | pendiente | Colección `interacciones_eventos` en modelo Mongo. |
| E2-NOT-01 | Dominio notificaciones + plantillas + intentos de envío | pendiente | init MySQL plantillas ya; completar pom JPA + código. |
| E2-NOT-02 | Disparos: compra confirmada, cancelación/modificación evento, artista favorito, devolución aprobada | pendiente | Async justificado. |
| E2-REP-01 | Reportes organizador: vendidas/devueltas por sector, ocupación, recaudación | hecho | 2026-11-11. `GET /reportes/eventos/{id}` (+ `/ventas`, `/sectores`, `/ocupacion`, `/recaudacion`) en `msvc-ventas`; ORGANIZADOR dueño o ADMIN. Devoluciones e ingresos por Feign a entradas (`GET /eventos/{id}/entradas/resumen`, interno); dueño por Feign a eventos; 503 si fallan (sin reporte parcial). Tests ventas 160 / entradas 84; Postman carpeta `11 Reportes del organizador` (9 requests). FE: pantalla `/gestion/eventos/:id/reportes` (gate `reportes`). |

### 7.2 Seguridad, resiliencia, observabilidad, despliegue, testing E2

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| E2-SEC-01 | JWT + roles (CLIENTE, ORGANIZADOR, ADMIN, PERSONAL_ACCESO); emitir en usuarios; validar en gateway | hecho | Emisión msvc-usuarios + validación/RBAC gateway (misma `jwt.secret`). |
| E2-SEC-02 | Autorización por endpoint según rol | parcial | GW RBAC por path/rol existe. 2026-11: `PATCH /eventos/{id}/estado` ya no es admin-only en GW (ORG llega a msvc; BE limita SUSPENDIDO vs CANCELADO). Falta auditoría fina resto paths + tests E2-TST-02. |
| E2-NFR-01 | Cache: qué, dónde, TTL, invalidación; qué no cachear (disponibilidad asientos) | pendiente | |
| E2-NFR-02 | Resiliencia: timeouts, reintentos, fallback ante externos | pendiente | |
| E2-NFR-03 | Trazabilidad HTTP + log centralizado de operaciones críticas con correlation id | hecho | 2026-11-11. `X-Correlation-Id` generado en el gateway (o el del cliente si es válido), propagado a los 6 servicios y por Feign, en cada línea de log y en la respuesta; línea de acceso `trace.http`; bitácora (`bitacora`) de login, eventos, pagos, entradas, devoluciones y validaciones. Verificado de punta a punta (gateway → ventas → entradas con el mismo id). "Centralizado" = `docker compose logs` con formato común (sin Loki/ELK). Ver `docs/observabilidad.md`. |
| E2-DEP-01 | Verificar stack completo `docker compose up` (apps + DBs + gateway) con dominio real | parcial | 2026-11-11: el stack completo levanta desde cero y el flujo E1 funciona de punta a punta (ver HIG-06). Falta repetirlo cuando `msvc-preferencias` y `msvc-notificaciones` tengan dominio real (E2-PRE/NOT). |
| E2-TST-01 | Pruebas de integración entre msvcs (Testcontainers o compose) | pendiente | |
| E2-TST-02 | Pruebas de autorización por rol | parcial | 2026-11-11: `JwtGatewayIntegrationTest` (gateway, 10 casos con `JwtUtil` real y tokens armados como los emite usuarios): firma/expiración, 403 por rol, propagación de `X-User-*`, límite de la Entrega 1 y secreto igual en gateway y usuarios. Falta recorrer cada endpoint por rol. Roles de validaciones de acceso: `EntradaControllerValidacionTest` + Postman carpeta 10. |
| E2-TST-03 | Prueba de carga flujo compra (JMeter u otro) + informe (usuarios, latencias, errores) | pendiente | |
| E2-UI-01 | (Opcional valorado) UI mínima cliente vía gateway: buscar, elegir sector/asiento, comprar, mis entradas | parcial | Flujos cliente demockeados. Checkout **bloquea** sin disponibilidad/cupo. Mis entradas QR + cancel. **Elegir asiento hecho** (SEAT-05: `SeatPicker` fila → asientos, hasta 6). 2026-11-11: probado contra el BE real (reserva por asiento 201, doble reserva 409; compra desde el navegador OK). Mock residual cero con `VITE_USE_MOCK=false`. Pantalla de escaneo cableada (E2-ACC-01). |
| E2-UI-02 | Cablear front a gateway (`VITE_USE_MOCK=false`) cuando existan endpoints E1 reales | hecho | 2026-11-11: un solo flag `VITE_USE_MOCK` (false = todo contra el gateway; true = todo mock; sin mezcla) y `Frontend/.env` versionado. Admin usuarios: listar/estado/rol reales (gate `adminUsuariosListado`); alta de personal de acceso, eliminar y asignar eventos quedan OFF (no existen en BE). Lo que no tiene BE (scan ACC, transferencia, reportes, preferencias, X-ORG-01) queda deshabilitado por `featureGates`. |
| E2-UI-03 | Instrucciones en README raíz del monorepo para levantar FE + gateway juntos | E2 | hecho | README raíz: gateway :8080 + `Frontend` npm dev + `VITE_USE_MOCK`. |

### 7.3 Extra / nice-to-have (no consigna — baja prioridad)

Fuera del alcance formal del TP. Solo si sobra tiempo o se quiere UX de producto.

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| X-UI-01 | SPA: pantalla **Mi perfil / cuenta** desde el ícono persona; editar datos propios | hecho | Ruta JWT `/perfil`: vista + form `nombre/apellido/ciudad`. Nav persona → `/perfil`. Facade `getMe`/`patchMe` mock+real (auth demock). Email/rol solo lectura. CTA org (X-ORG-01) gated OFF en perfil CLIENTE. |
| X-UI-02 | Login: copy roles + guía “¿Sos organizador?” | hecho | Tab “Cuenta cliente”; CTA → `/cuenta/organizador` (guía CLIENTE self-service; ORG/ADMIN/ACCESO no self-register). Register copy solo cliente. |
| X-UI-03 | Gestión: **Editar evento** en BORRADOR (PUT) | hecho | Listado → Editar → `/gestion/eventos/:id/editar`; solo BORRADOR + dueño/admin; `updateEvento` PUT. |
| X-UI-04 | Feature gates FE: deshabilitar CTAs no cableados / no permitidos por rol | hecho | Gates finos + **roles puros**: `canScanAccess` / `/acceso*` solo PERSONAL_ACCESO (admin de plataforma no tiene CTA puerta). Hub admin «Usuarios» ≠ panel Acceso. |
| X-QR-01 | Política QR: no exponer secreto si entrada no usable | hecho | BE: DTO/`GET …/qr` solo VENDIDA + evento ≠ CANCELADO; approve/reintegro → `INV-*`. FE detalle sin QR si no aplica. E2-ACC puerta sigue pendiente. |
| X-QR-02 | Mis entradas: cancelación distinguible + trámites unificados | hecho | Lista activas + A_REINTEGRAR (badge, sin QR, aviso). Ver devoluciones = voluntarias + reintegro cancelación. BE reintegro crea devolucion EN_CURSO. |
| X-UI-05 | Toast cuentas demo seed en `/login` (reopen) | hecho | Siempre visible en login (no solo mock); seed gateway mismos pass; dismiss sessionStorage + CTA reopen. |
| X-UI-06 | SPA: badge cupo (Agotado / últimos / &lt;50 / &lt;100) en listado + detalle | hecho | BE: batch `GET /eventos/disponibilidad?ids=` + DTO `disponibleTotal`/`nivelCupo`. FE: EventList 1 batch; cards + detalle; Comprar por sector si disponible≥1. |
| X-ORG-01 | Extensión cuenta CLIENTE → ORGANIZADOR (solicitud + auth admin) | pendiente | **No se implementa ahora.** Diseño §7.3.1. FE: CTA «Solicitar cuenta de organizador» en `/perfil` (solo CLIENTE) con `FEATURE.solicitudOrganizador=false` + guía `/cuenta/organizador`. Falta: API solicitudes, form, cola admin, approve→rol, refresh JWT. |
| X-ORG-02 | Política de roles: cuenta base CLIENTE; privilegios los otorga ADMIN | pendiente | Doc/producto (no code). CLIENTE = register; ORG = extensión (X-ORG-01) o admin asigna rol; ADMIN/ACCESO = solo admin. Seeds demo siguen. Opcional: reflejar en US-01 cuando se escriba. |
| X-ACC-01 | Asignar eventos a cada PERSONAL_ACCESO (solo puede validar los asignados) | descartado | Decisión 2026-11-11: no se modela (suma tabla de asignación, endpoints de alta/asignación, chequeo Feign en cada escaneo y pantalla de admin; la consigna no lo pide). Hoy cualquier PERSONAL_ACCESO valida cualquier evento; se documenta como simplificación. Si se retoma, el cambio se limita a un chequeo extra en la validación. |

### 7.3.1 Diseño acordado — extensión a organizador (X-ORG-01)

**Negocio:** ser organizador requiere **autorización**. No hay alta pública directa de ORGANIZADOR.

**Regla de entrada**
1. El usuario ya es **CLIENTE** (registro normal `/login?tab=register`).
2. Pide **extensión** de cuenta a organizador (solicitud con datos + motivo).
3. Un **ADMIN** aprueba o rechaza.
4. Si aprueba: se asigna rol **ORGANIZADOR** (ideal: conservar CLIENTE + ORG, o reemplazo según política de roles M2M).
5. PERSONAL_ACCESO y ADMIN **no** usan este flujo (alta solo admin / otro admin).

**Actores y pantallas FE**
| Quién | Dónde | Qué |
| --- | --- | --- |
| Anónimo | `/cuenta/organizador` | Guía: “primero cuenta cliente”; CTA login/register; sin form de alta org free. |
| CLIENTE logueado | `/perfil` | CTA **Solicitar cuenta de organizador** (gate `solicitudOrganizador`; OFF hasta API). Guía en `/cuenta/organizador`. |
| CLIENTE | mismo | Estado de solicitud: PENDIENTE / APROBADA / RECHAZADA + n° trámite. |
| ADMIN | `/gestion/usuarios` o `/gestion/solicitudes-org` | Cola: listar pendientes, ver datos, Aprobar / Rechazar. |

**Datos mínimos de la solicitud (negocio)**
- idUsuario (del JWT)
- fechaSolicitud
- motivo / descripción de actividad (texto)
- opcional fase 2: teléfono, nombre fantasía, CUIT, sitio web
- estado: `PENDIENTE` \| `APROBADA` \| `RECHAZADA`
- idAdminResolutor, fechaResolucion, notaAdmin (si rechaza)

**API tentativa (msvc-usuarios)**
- `POST /organizadores/solicitudes` — CLIENTE; body `{ motivo, … }`; 409 si ya PENDIENTE o ya ORG
- `GET /organizadores/solicitudes/me` — CLIENTE; su última/historial
- `GET /organizadores/solicitudes?estado=PENDIENTE` — ADMIN
- `POST /organizadores/solicitudes/{id}/aprobacion` — ADMIN → roles += ORGANIZADOR (o PUT roles)
- `POST /organizadores/solicitudes/{id}/rechazo` — ADMIN + `{ nota? }`

Tabla o colección `solicitud_organizador` (MySQL usuarios). No reusar `devolucion`.

**Estados de UX**
```
CLIENTE sin solicitud → CTA “Quiero organizar eventos”
       ↓ POST solicitud
PENDIENTE → “En revisión”; sin panel Gestión aún
       ↓ admin aprueba
ORGANIZADOR (+ CLIENTE) → homePath /gestion; puede crear eventos
       ↓ admin rechaza
CLIENTE + mensaje; puede volver a pedir (política: cooldown o 1 activa)
```

**Qué no es**
- No es el register con rol org.
- No es mock “crear user ORGANIZADOR” en LS como camino de producción (solo demo TP si hiciera falta).
- Admin/acceso siguen fuera de self-service.

**Orden de implementación cuando se ataque**
1. Modelo + endpoints usuarios + GW RBAC.
2. FE cliente: form + estado solicitud (logueado).
3. FE admin: cola approve/reject (destrabar parte de `adminUsuarios` o hub nuevo).
4. Post-approve: refresh JWT / me para ver rol nuevo sin re-login forzado si el token no trae roles frescos (refresh o re-login).
5. Notif E2 opcional: “tu solicitud fue aprobada”.

**Relación con lo ya hecho**
- Copy y ruta `/cuenta/organizador` (guía) = base UX (**X-UI-02** hecho).
- `X-UI-01` perfil puede alojar el CTA de extensión.
- Seeds `org@` siguen para demo sin pasar por solicitud.
- Política roles base (**X-ORG-02**): CLIENTE self-service; resto lo otorga ADMIN (ORG vía este flujo).

---

## 8. Orden sugerido de ataque (no es el backlog formal)

1. **INF-27** — hecho (datasource + entidades = schema validate).  
2. **INF-28** — hecho (init Mongo preferencias).  
3. **E1-EVE-04** — hecho (GET catálogo/detalle + DTO).  
4. **E1-GW-01/03 + API-05** — hecho (rutas + CORS).  
5. **E1-USU-01 + E1-GW-02 + E2-SEC-01** — hecho (login JWT + headers `X-User-*` + validación en gateway).  
6. **Smoke integración** — **hecho 2026-11-06**.  
7. **E1-VEN-02…08** — **hecho**.  
8. **E1-ENT-02…05** — **hecho** (emisión Feign, GETs/QR, devoluciones 72h, reintegro-cancelación). FE demock auth/eventos/compras/entradas.  
9. **E1-EVE write (02/03/05)** — **hecho** (lugares/sectores, borrador→publicar, patch estado/fecha, listado org).  
10. **FE sesión 2026-11-07 (a)** — X-QR-01/02, X-UI-02/03/04; GW org PATCH estado (E2-SEC-02 parcial).  
11. **FE/BE sesión 2026-11-07 (b)** — **hecho:** cupo al publicar (E1-VEN-02b + Feign EVE); form alta (borrador laxo / publicar completo / sin ilimitado); checkout sin stock bloquea; toast demo; admin sin CTA Acceso; editar borrador.  
12. **FE residual — hecho 2026-11-11:** demock admin usuarios **selectivo** (E2-UI-02) — list/estado/roles reales; alta acceso + eventosAsignados + DELETE quedan OFF.  
13. E1-TST + INF-17 (Postman) — **hecho** (equipo, rama feature/tests); E1-REL parcial (README). Higiene HIG-01…06 abierta.  
14. E2: ACC scan → PRE → NOT → FILA/REP → NFR → DEP/TST; X-ORG-01 si producto.

Specs (DOC-MS / US / ARQ) en paralelo — varios msvc ya tienen `endpoints.md` **parcial**.

---

## 9. Resumen de estado

| Bloque | Hecho | Parcial | Pendiente |
| --- | --- | --- | --- |
| Infra / scaffold / smoke / compose | INF-01…12, 14–16, 18, **25**, **27**, **28** | INF-13 | INF-17, 19 |
| Frontend (ingreso SPA) | INF-20…25, E2-UI-02/03 (flag único de mock, 2026-11-11); **X-UI-01…05**, **X-QR-01/02**, **X-UI-06**, **SEAT-05** (selector de asientos + forms con layout) | E2-UI-01 (scan E2-ACC-01 hecho) | X-ORG-01; transferencia/prefs |
| Docs por msvc (specs) | — | DOC-MS-USU/EVE/VEN/ENT (`endpoints.md` vivo) | DOC-MS-00/GW/PRE/NOT/SYNC plantilla formal |
| Modelo datos | MOD-01, MOD-04 (Mongo doc+init), MOD-05 (notif JPA) | MOD-02, 03 | MOD-06 |
| MySQL init schema+seed | INF-10 (mysql), INF-15 | — | — |
| Mongo init preferencias | INF-28 + docs Spring | — | dominio E2-PRE-* |
| Redis conf ventas | INF-10 (redis.conf) + **E1-VEN-08 StockCache** | — | — |
| Auth / gateway | E1-GW-01/02/03, API-05 (+ruta cupos), E1-USU-01, E2-SEC-01 | **E2-SEC-02** (estado eventos ORG ok; resto fino), **E2-TST-02** (`JwtGatewayIntegrationTest`; falta cada endpoint por rol) | — |
| US / arquitectura negocio / OpenAPI vivo | comunicacion + microservicios; US-01/02 (Trello); ARQ-02…04; OpenAPI regenerado con asientos | endpoints listado + contratos FE | ARQ-01 (cerrar `microservicios.md` / `comunicacion.md`) |
| Código de negocio E1 | E1-EVE-02…**06** (+cupo Feign publicar + participantes); E1-VEN-02…08 + **02b cupos**; E1-ENT-02…05; **SEAT-01…06** (asientos virtuales en sectores numerados; Postman/OpenAPI e IT con MySQL real incluidos) | —; **E2-ACC-01** (control de acceso QR) | E2-PRE-01…03; E2-NOT-01/02; E2-FILA-01; E2-REP-01 |
| Extra producto | X-UI-01…06, X-QR-* | — | X-ORG-01/02 |
| Testing / Postman / README | E1-TST-01/02, INF-17, README raíz + Backend + FE | — | HIG-01…03 (notificaciones/preferencias sin tests, lint FE); E2-TST-01/03 |

**Lectura (2026-11-07, cierre sesión — seed amplio + catálogo AGOTADO):**  
- **Hecho E1 core:** eventos GET+write; ventas (+ cupo al publicar); entradas QR/devo; JWT; FE demock AUTH/EVENTOS/COMPRAS/ENTRADAS.  
- **Hecho E1-EVE-06:** participantes=artistas; API+authz; FE autocomplete por id.  
- **Seed demo:** 14 eventos / 3 estadios (Oasis **AGOTADO** visible); cupos ventas + mocks FE; `init.sql` por base.  
- **Catálogo BE default:** PUBLICADO + **AGOTADO** (home/list sin `estado=`); jar `ms-eventos` redeployed.  
- **Ops:** `init.sql` solo en volumen nuevo; live → reseed `03-*.sql` o borrar volumen; jars `docker cp` si stale.  
- **Demock residual:** transfer/prefs/reportes/X-ORG-01 = sin BE, deshabilitados por gate (el scan de acceso ya está cableado).  
- **Gap E1 formal:** cerrado salvo E1-REL (README). Ver actualización 2026-11-10.  
- **Próxima sesión (acordado):** demock **admin usuarios selectivo** (list/estado/roles reales; alta acceso + eventosAsignados + DELETE OFF).  

**Verificación real 2026-11-10 (tests locales, Docker apagado):**  
- Unitarias OK: gateway 23, usuarios 51, eventos 119, entradas 54, ventas 139 (HIG-04 resuelto; eventos/ventas/entradas incluyen los tests de asientos virtuales). Preferencias sin tests; notificaciones `contextLoads` falla sin MySQL.  
- FE: `build` OK, `lint` 12 errores preexistentes (HIG-02; los cambios de SEAT-05 no agregaron ninguno). Sin test runner de UI: lógica de dominio y render del `SeatPicker` verificados con scripts descartables.  
- Sin verificar: stack compose, smoke, IT Testcontainers, Postman (HIG-06). **Asientos virtuales (SEAT-01…05) sin probar contra MySQL/Redis/gateway reales**: scripts `04-*.sql` sin ejecutar, flujo reserva→pago→emisión→devolución con asientos solo cubierto por unitarios y mocks.  
- **Decisión 2026-11-10 (resuelve la pregunta 6 de `reglas-de-negocio.md`):** modelo híbrido — sector numerado por asiento concreto (virtual, solo se guarda el tomado), sector general por cupo; precio por sector.  
- E2 sin código de negocio: ACC, FILA, PRE (solo docs Mongo + repos), NOT (solo entidades/service stub), REP, NFR-01/02/03.  

**Actualización 2026-11-11 (cierre de sesión):**  
- **Hecho:** SEAT-07 (sin `asiento`/`precio_asiento`, 119 tests eventos OK); un único `init.sql` por base y sin `migrations/` (HIG-07); front con un solo flag de mock y `Frontend/.env` versionado (E2-UI-02); test de integración JWT en el gateway (E2-TST-02 parcial; gateway 35 tests OK); stack completo verificado desde cero (HIG-06, E2-DEP-01 parcial).  
- **Verificado contra el BE real:** login JWT, reserva por asiento (201), doble reserva (409), cancelación; compra con asientos desde el navegador OK.  
- **E2-ACC-01 hecho (control de acceso QR):** `POST/GET /eventos/{id}/validaciones` en `msvc-entradas` (atómico por QR, auditado, 74 tests + IT con MySQL real), scan cableado en el front, Postman carpeta 10 (colección completa: 146 requests, 424 assertions, 0 fallos), OpenAPI y diagrama de secuencia §6. Sin asignación de eventos por persona (X-ACC-01 descartado).  
- **E2-REP-01 y E2-NFR-03 hechos (2026-11-11):** reportes del organizador (API + pantalla) y correlation id + bitácora en los 7 servicios. Tests: gateway 40, usuarios 58, eventos 126, ventas 160, entradas 84. Postman: 155 requests, 448 assertions, 0 fallos.  
- **Queda para la Entrega 2 (sin código de negocio):** E2-PRE-01/02/03 (Mongo), E2-NOT-01/02, E2-FILA-01; NFR: caché y resiliencia (E2-NFR-01/02); tests: integración entre msvcs, autorización por endpoint y carga (E2-TST-01/02/03); docs: ARQ-05/06, API-04, MOD-06, US-03.  
