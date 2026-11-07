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
| INF-17 | Colección Postman (o similar) de endpoints | E1 | pendiente | Actualizar en E2. |
| INF-18 | Higiene compose preferencias: URI Mongo con `${MONGO_PASSWORD:-…}` (sin literal frágil) | Transversal | hecho | `msvc-preferencias/docker-compose.yml` usa `${MONGO_PASSWORD:-mongo_dev}`. |
| INF-19 | Alinear docs (`arquitectura`, `bases-de-datos`) con `msvc-notificaciones` y conteo real de servicios | Transversal | pendiente | |
| INF-20 | Ingreso SPA cliente en `Frontend/`: React 19 + Vite + Router, `src/` (api/auth/pages/routes/domain/…), `package.json`, `.env.example` (`VITE_API_BASE_URL`, `VITE_USE_MOCK`) | Transversal | hecho | Copia del simulacro; `node_modules`/`dist` locales presentes. |
| INF-21 | README de ejecución del front (`Frontend/README.md`) + scripts npm (`dev`/`build`/`lint`) | Transversal | hecho | No sustituye README raíz/Backend (INF-16). |
| INF-22 | Contrato front documentado: `Frontend/docs/endpoints-front.md` + `endpoints-back.md` | Transversal | hecho | Paths front alineados al contrato back mandatorio. |
| INF-23 | Facades `src/api/*` + capa mock LS (`VITE_USE_MOCK=true` por defecto) con misma forma de llamada que gateway | Transversal | hecho | Demock **selectivo** vía `VITE_USE_MOCK_AUTH/EVENTOS/COMPRAS/ENTRADAS=false`. Global mock sigue true (gestión admin usuarios, scan E2, etc.). |
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
| DOC-MS-USU | Specs + diseño de `msvc-usuarios/docs/` | E1 (mínimo id) / E2 (JWT) | pendiente | E1 puede documentar identidad liviana; E2 auth formal. |
| DOC-MS-EVE | Specs + diseño de `msvc-eventos/docs/` | E1 | pendiente | Eventos, lugares, sectores, participantes, publicación, búsqueda. |
| DOC-MS-VEN | Specs + diseño de `msvc-ventas/docs/` | E1 (+ fila E2) | pendiente | Reserva, compra, pago mock, idempotencia, anti doble venta; fila virtual en E2. |
| DOC-MS-ENT | Specs + diseño de `msvc-entradas/docs/` | E1 (emisión/devo) / E2 (validación acceso) | pendiente | |
| DOC-MS-PRE | Specs + diseño de `msvc-preferencias/docs/` (documentos Mongo, recs) | E2 | pendiente | Incluir justificación NoSQL. Modelo doc: MOD-04. |
| DOC-MS-NOT | Specs + diseño de `msvc-notificaciones/docs/` | E2 | pendiente | Canales, plantillas, disparos async. |
| DOC-MS-SYNC | Mantener specs versionadas en Git y usarlas como guía antes de implementar cada feature (flujo Spec-Driven de la consigna) | Transversal | pendiente | Propuesta → requisitos → diseño → tareas → código. |

---

## 2. Modelo de datos

| ID | Tarea | Entrega | Estado | Notas |
| --- | --- | --- | --- | --- |
| MOD-01 | ER global MySQL en `docs/diagramas/modelo.md` (Mermaid) | E1 | hecho | Sin preferencias/historial (Mongo). FKs explícitas. Link a `modelo-mongodb.md`. |
| MOD-02 | Partir / complementar el modelo en **un DER (o vista) por microservicio** con claves y ownership de datos | E1 | parcial | Ownership implícito en `init.sql` por carpeta msvc; falta diagrama/doc por contexto (opcional si el init + ER global bastan). |
| MOD-03 | Documentar **entidades solapadas** entre BDs (mismos IDs lógicos sin FK cruzada: `idEvento`, `idCliente`, etc.) | E1 | parcial | UUIDs seed fijos en `infrastructure/mysql/README.md` + comentarios `ref lógica` en init. Completar párrafo formal en docs si hace falta. |
| MOD-04 | **Modelo de preferencias en MongoDB** (`docs/diagramas/modelo-mongodb.md`) | E2 | hecho | Doc + init.js INF-28. Código Spring Data / recs = E2-PRE-*. |
| MOD-05 | Modelo local notificaciones (MySQL) alineado a ER + `notificaciones/init.sql` | E2 | hecho | Init + JPA (Plantilla/Notificacion/IntentoEnvio) en INF-27. Specs DOC-MS siguen pendientes. |
| MOD-06 | Actualizar diagramas E2 cuando entren fila, validaciones, notifs y recs | E2 | pendiente | |

---

## 3. Producto — Features, user stories y criterios de aceptación

| ID | Tarea | Entrega | Estado | Notas |
| --- | --- | --- | --- | --- |
| US-01 | Features + user stories de **todo el sistema** (E1+E2) con roles CLIENTE / ORGANIZADOR / ADMIN / PERSONAL_ACCESO | E1 | pendiente | Artefacto de consigna §5.A. |
| US-02 | Criterios de aceptación por historia del núcleo E1 | E1 | pendiente | |
| US-03 | Completar/actualizar CA de historias E2 y de las que cambien | E2 | pendiente | |

---

## 4. Arquitectura y diseño (docs transversales)

| ID | Tarea | Entrega | Estado | Notas |
| --- | --- | --- | --- | --- |
| ARQ-01 | Definir responsabilidades finales de cada msvc y APIs públicas (cierre de `microservicios.md` / `comunicacion.md`) | E1 | pendiente | Scaffold de nombres ya existe. |
| ARQ-02 | Diagrama de módulos de la arquitectura | E1 | pendiente | Complementa diagrama Docker ya existente. |
| ARQ-03 | Diagrama de secuencia: compra de entrada **numerada** (éxito) | E1 | pendiente | |
| ARQ-04 | Misma secuencia: **pago rechazado** y **vencimiento de reserva** | E1 | pendiente | |
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
| API-05 | Implementar en Gateway el enrutado real según `endpoints.md` (incl. excepciones `/eventos/{id}/disponibilidad`, `/fila/**`, `/validaciones`, `/recomendaciones`) | E1 (rutas core) / E2 (resto) | hecho | Routes en `application.yml` (Gateway MVC). StripPrefix `/api`. Excepciones order 10–13. Compose: `TICKETYA_URI_*`. Doc: `APIGateway/docs/endpoints.md`. |

---

## 6. Entrega 1 — Implementación del núcleo

Identificación E1: sin JWT formal; `X-User-Id` / header de rol (o equivalente) documentado en specs.

**Prerrequisito de implementación:** INF-27 (Spring + init) antes de abrir GETs de negocio.

### 6.1 msvc-eventos

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| E1-EVE-00 | Datasource + JPA validate contra `eventos/init.sql` (parte de INF-27) | hecho | EMF OK; JDBC `db-mysql-eventos`. |
| E1-EVE-01 | Entidades JPA reales alineadas al init: Evento, Lugar, Sector, Asiento, PrecioSector/PrecioAsiento (+ Participante si se agrega al schema) | hecho | 6 entidades + repos. Participante no está en init → no creado. |
| E1-EVE-02 | CRUD lugares y sectores; asientos en sector numerado | hecho | GET/POST/PUT lugares; POST `/lugares/{id}/sectores`; PUT `/sectores/{id}`; GET asientos físicos (seed). Alta embebida también desde POST evento. |
| E1-EVE-03 | Eventos en borrador; configuración sectores/precios; publicación con validaciones | hecho | POST/PUT/DELETE evento; PUT precio sector; POST publicacion (nombre/fecha/lugar/≥1 precio). Body alineado form FE gestión. |
| E1-EVE-04 | **GETs** búsqueda/catálogo: tipo, ciudad, fechas; orden; paginación — `GET /eventos`, `GET /eventos/{id}` | hecho | + `estado=TODOS` para gestión admin. Default PUBLICADO. Doc: `msvc-eventos/docs/endpoints.md`. |
| E1-EVE-05 | Patch estado (suspender/cancelar) y fecha; listado organizador | hecho | PATCH estado/fecha; GET `/organizadores/me/eventos`. CANCELADO → Feign reintegro entradas best-effort. ORG: SUSPENDIDO propios; resto ADMIN. |
| E1-EVE-06 | Participantes y asociación a evento | pendiente | Hoy no están en init; sumar schema si se implementan. |

### 6.2 msvc-ventas

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| E1-VEN-00 | Datasource + JPA validate contra `ventas/init.sql` (INF-27) | hecho | EMF OK; Redis host compose ok. |
| E1-VEN-01 | Modelo: CupoSector, Reserva, DetalleReserva, Compra, Pago (+ asientos reservados si numerado) | hecho | 5 entidades + repos. Sin tabla asiento_reservado en init → no inventada. |
| E1-VEN-02 | Disponibilidad comercial por evento | hecho | `GET /eventos/{id}/disponibilidad` → cupo_sector. Doc msvc. |
| E1-VEN-03 | Reserva temporal (TTL p.ej. 10 min), confirmación, cancelación, renovación | hecho | PENDIENTE/CONFIRMADA; job expire 30s + lazy; Redis hold. |
| E1-VEN-04 | Compra desde reserva + **Idempotency-Key**; límite de entradas por cliente/evento | hecho | Key obligatoria (Redis 24h); max 6/cliente/evento. |
| E1-VEN-05 | Pasarela de pagos **mock** (aprueba/rechaza) | hecho | `POST /compras/{id}/pagos` body resultado APROBAR\|RECHAZAR. |
| E1-VEN-06 | Mecanismo **anti doble venta** (restricciones unicidad, locking/versionado y/o Redis) usable con varias instancias | hecho | PESSIMISTIC_WRITE cupo + UK evento/sector; Redis hold auxiliar. |
| E1-VEN-07 | Liberación de cupo/asiento si pago rechazado o reserva vencida | hecho | Reject/cancel/expire liberan reservada→disponible. |
| E1-VEN-08 | Cableado Redis según decisión de diseño (stock/TTL); hoy clases vacías | hecho | `StringRedisTemplate` + `StockCache` (hold + idempotency). starter data-redis. |

### 6.3 msvc-entradas

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| E1-ENT-00 | Datasource + JPA validate contra `entradas/init.sql` (INF-27) | hecho | EMF OK. |
| E1-ENT-01 | Modelo Entrada (QR único), Devolucion, ValidacionAcceso | hecho | 3 entidades + repos. **ValidacionAcceso sin API** (E2-ACC-01). Seed intacto. |
| E1-ENT-02 | Emisión de entradas con QR tras compra confirmada | hecho | `POST /entradas/emision`; Feign ventas post-pago **best-effort** (si falla, compra queda APROBADA sin QR — log). Idempotente por idCompra. Precio solo en memoria proceso hasta restart. |
| E1-ENT-03 | **GET** entradas del cliente (`GET /entradas`, `GET /entradas/{id}`) | hecho | + GET `/{id}/qr`. DTO enriquece `eventoNombre`/`lugar*`/`sectorNombre` vía Feign msvc-eventos. **GET /{id}: dueño o ADMIN** (un método). FE admin: `/gestion/entradas/:id` (no mis-entradas). |
| E1-ENT-04 | Solicitud de devolución (plazo 72 h, no usada); approve/reject admin; liberación + reintegro mock | hecho | 72h desde **emisión**; ADMIN→DEVUELTA. **Cupo ventas no se reabre**. Monto devolución depende de precio en memoria (0 tras restart del msvc). FE admin cola: `USE_MOCK_ENTRADAS`. |
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
| E1-TST-01 | Unitarias: compra, devoluciones, búsqueda | pendiente | |
| E1-TST-02 | Prueba de concurrencia: mismo asiento / último cupo → una sola venta exitosa | pendiente | |
| E1-REL-01 | Datos de prueba + Postman + README ejecución del núcleo | pendiente | Datos base = init.sql (INF-15); Postman = INF-17. |

---

## 7. Entrega 2 — Funcionalidades restantes y NFR

### 7.1 Funcional

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| E2-FILA-01 | Fila virtual (alta demanda): ingreso, posición, tandas, token de compra | pendiente | Simplificable con justificación. |
| E2-ACC-01 | Control de acceso QR: validar existencia, evento, no reutilizada; marcar usada | pendiente | PERSONAL_ACCESO. |
| E2-PRE-01 | Preferencias cliente en **Mongo** (categorías, ciudad, rango precio, artistas) | pendiente | Init INF-28 hecho; falta dominio Spring Data + API. |
| E2-PRE-02 | Recomendaciones (propias y/o similares por evento); puede ser simplificado | pendiente | |
| E2-PRE-03 | Historial de cliente / ingesta desde compras o eventos de dominio | pendiente | Colección `interacciones_eventos` en modelo Mongo. |
| E2-NOT-01 | Dominio notificaciones + plantillas + intentos de envío | pendiente | init MySQL plantillas ya; completar pom JPA + código. |
| E2-NOT-02 | Disparos: compra confirmada, cancelación/modificación evento, artista favorito, devolución aprobada | pendiente | Async justificado. |
| E2-REP-01 | Reportes organizador: vendidas/devueltas por sector, ocupación, recaudación | pendiente | |

### 7.2 Seguridad, resiliencia, observabilidad, despliegue, testing E2

| ID | Tarea | Estado | Notas |
| --- | --- | --- | --- |
| E2-SEC-01 | JWT + roles (CLIENTE, ORGANIZADOR, ADMIN, PERSONAL_ACCESO); emitir en usuarios; validar en gateway | hecho | Emisión msvc-usuarios + validación/RBAC gateway (misma `jwt.secret`). |
| E2-SEC-02 | Autorización por endpoint según rol | pendiente | |
| E2-NFR-01 | Cache: qué, dónde, TTL, invalidación; qué no cachear (disponibilidad asientos) | pendiente | |
| E2-NFR-02 | Resiliencia: timeouts, reintentos, fallback ante externos | pendiente | |
| E2-NFR-03 | Trazabilidad HTTP + log centralizado de operaciones críticas con correlation id | pendiente | |
| E2-DEP-01 | Verificar stack completo `docker compose up` (apps + DBs + gateway) con dominio real | pendiente | Estructura compose ya existe (INF-08). DBs con init = INF-10/15. |
| E2-TST-01 | Pruebas de integración entre msvcs (Testcontainers o compose) | pendiente | |
| E2-TST-02 | Pruebas de autorización por rol | pendiente | |
| E2-TST-03 | Prueba de carga flujo compra (JMeter u otro) + informe (usuarios, latencias, errores) | pendiente | |
| E2-UI-01 | (Opcional valorado) UI mínima cliente vía gateway: buscar, elegir sector/asiento, comprar, mis entradas | parcial | SPA con esos flujos en **mock**. Falta BE real. |
| E2-UI-02 | Cablear front a gateway (`VITE_USE_MOCK=false`) cuando existan endpoints E1 reales | E2/E1-late | parcial | Demock selectivo: AUTH/EVENTOS/COMPRAS/**ENTRADAS**=false. Global mock true. Checkout→QR→mis-entradas BE. Falta: mock-off gestión usuarios admin, scan E2-ACC. DTO entradas ya trae nombre/lugar (Feign). |
| E2-UI-03 | Instrucciones en README raíz del monorepo para levantar FE + gateway juntos | E2 | hecho | README raíz: gateway :8080 + `Frontend` npm dev + `VITE_USE_MOCK`. |

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
10. E1-TST + E1-REL + INF-17 (Postman).  
11. E2: PRE → NOT → FILA/ACC/REP → NFR → DEP/TST.  
12. FE residual: mock-off gestión admin usuarios / E2-ACC scan. E1-EVE-06 participantes si se suma schema.

Specs (DOC-MS / US / ARQ) en paralelo.

---

## 9. Resumen de estado

| Bloque | Hecho | Parcial | Pendiente |
| --- | --- | --- | --- |
| Infra / scaffold / smoke / compose | INF-01…12, 14–16, 18, **25**, **27**, **28** | INF-13 | INF-17, 19 |
| Frontend (ingreso SPA) | INF-20…25, E2-UI-03 | E2-UI-01/02 demock selectivo auth+eve+ven+ent | mock residual gestión/usuarios admin, E2-ACC |
| Docs por msvc (specs) | — | — | Todo DOC-MS-* |
| Modelo datos | MOD-01, MOD-04 (Mongo doc+init), MOD-05 (notif JPA) | MOD-02, 03 | MOD-06 |
| MySQL init schema+seed | INF-10 (mysql), INF-15 | — | — |
| Mongo init preferencias | INF-28 + docs Spring | — | dominio E2-PRE-* |
| Redis conf ventas | INF-10 (redis.conf) + **E1-VEN-08 StockCache** | — | — |
| Auth / gateway | E1-GW-01/02/03, API-05, E1-USU-01, E2-SEC-01 | — | E2-SEC-02 RBAC fino por endpoint |
| US / arquitectura negocio / OpenAPI vivo | comunicacion + microservicios | endpoints listado + contratos FE | OpenAPI vivo, US-01/02, ARQ-01…04 |
| Código de negocio E1 | E1-EVE-02…05; E1-VEN-02…08; E1-ENT-02…05 (+DTO Feign) | — | E1-EVE-06 participantes; E2-ACC |
| Testing / Postman / README | README raíz + Backend + FE | — | INF-17, E1-TST, E1-REL |

**Lectura (2026-11-07):**  
- **Hecho E1 core:** eventos GET+**write** (02/03/05); DTO entradas enriquecido (ENT-03 Feign); JWT; ventas; entradas QR/devo; FE demock selectivo (+ admin list `estado=TODOS`).  
- **Deuda natural (documentada):** emisión Feign best-effort; precio devolución RAM; approve sin reopen cupo; ValidacionAcceso=E2-ACC; cancel evento→reintegro entradas best-effort; cupo ventas no auto al publicar. Ver docs msvc-entradas/eventos.  
- **Gap E1 formal restante:** E1-EVE-06 participantes (sin schema); E1-TST / INF-17 Postman / E1-REL.  
- **Próximo:** tests/Postman **o** E2-ACC.  
