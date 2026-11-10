# Documentación y Validación — Especificación de trabajo (E1)

> **Versión:** 1.1 · **Fecha:** 2026-11-08 · **Alcance:** Entrega 1 (E1)
> **Estado:** decisiones y alcance ratificados (§3) — bloqueada solo por las aprobaciones de `pom.xml` (§5 A-01/A-02/A-03) y por la salida de Plan mode (P0.1 escribe build en disco)
> **Ruta definitiva:** `Backend/specs/documentacion-y-validacion.md`
> **Artefactos de entrega:** `Backend/docs/`

Esta especificación es la **fuente de verdad** de la línea "Documentación y Validación".
Se ejecuta en pasos atómicos y secuenciales (§6). Ningún paso se considera terminado sin
la verificación del responsable del repositorio y su **commit manual**.

---

## Índice

- [0. Resumen ejecutivo](#0-resumen-ejecutivo)
- [1. Alcance y exclusiones](#1-alcance-y-exclusiones)
- [2. Reglas de ejecución](#2-reglas-de-ejecución)
- [3. Decisiones cerradas](#3-decisiones-cerradas)
- [4. Estado de partida verificado](#4-estado-de-partida-verificado)
- [5. Aprobaciones requeridas (bloqueantes)](#5-aprobaciones-requeridas-bloqueantes)
- [6. Mapa de pasos](#6-mapa-de-pasos)
- [7. Fase 1 — OpenAPI / Springdoc](#7-fase-1--openapi--springdoc)
- [8. Fase 2 — Bundle estático consolidado](#8-fase-2--bundle-estático-consolidado)
- [9. Fase 3 — Tests unitarios JUnit 5 + Mockito](#9-fase-3--tests-unitarios-junit-5--mockito)
- [10. Fase 4 — Concurrencia](#10-fase-4--concurrencia)
- [11. Fase 5 — Postman](#11-fase-5--postman)
- [12. Fase 6 — User Stories](#12-fase-6--user-stories)
- [13. Fase 7 — Cierre de entrega](#13-fase-7--cierre-de-entrega)
- [14. Trazabilidad con el backlog](#14-trazabilidad-con-el-backlog)
- [15. Riesgos y decisiones abiertas](#15-riesgos-y-decisiones-abiertas)
- [16. Changelog](#16-changelog)
- [17. Registro de ejecución](#17-registro-de-ejecución)

---

## 0. Resumen ejecutivo

| # | Entregable | Artefacto | Fase |
|---|---|---|---|
| 1 | Contrato OpenAPI vivo por microservicio | `/v3/api-docs` + Swagger UI por msvc | 1 |
| 2 | Especificación OpenAPI versionada | `Backend/docs/openapi/*.yaml` + bundle consolidado | 2 |
| 3 | Tests unitarios de lógica de negocio | `src/test/java/**` + JaCoCo ≥ 90 % (alcance §9.2) | 3 |
| 4 | Prueba de no doble venta | Unitaria (build normal) + integración MySQL (`-Pit`) | 4 |
| 5 | Colección de requests | `Backend/docs/postman/*.postman_collection.json` + environment | 5 |
| 6 | Features y user stories E1 con criterios de aceptación | Tablero Trello del equipo | 6 |

**Cobertura de endpoints E1 a documentar: 62** (usuarios 11, eventos 24, ventas 17, entradas 10),
más el gateway documentado como capa de ruteo/RBAC, más 6 endpoints smoke `GET /` fuera de contrato.

---

## 1. Alcance y exclusiones

### Incluido

- **Solo endpoints implementados y operables en E1** de `msvc-usuarios`, `msvc-eventos`,
  `msvc-ventas`, `msvc-entradas` y el comportamiento del `APIGateway`.
- Documentación de **ruteo, rutas públicas y matriz RBAC** del gateway (sin springdoc).
- Tests unitarios de la lógica de negocio de esos 4 msvc + `JwtUtil`/filtro del gateway.
- Prueba de concurrencia sobre el mecanismo real anti doble venta (cupo de sector).
- Colección Postman de E1 y user stories E1 (E2 en alto nivel).

### Excluido (queda para tandas posteriores)

- `msvc-preferencias` y `msvc-notificaciones` (sin API de negocio en E1).
- Endpoints E2 diseñados en `specs/endpoints.md` pero **no implementados**: fila virtual,
  validaciones de acceso QR, reportes, recomendaciones, transferencias.
- Diagramas de secuencia (compra OK / pago rechazado / reserva vencida) → tanda posterior.
- Pruebas de integración entre microservicios, seguridad y carga (consigna E2 §F).
- Newman / CI. No requerido en esta tanda.

---

## 2. Reglas de ejecución

### R1 — Ejecución secuencial y atómica

Un paso = un cambio verificable + un commit. No se adelanta el paso siguiente hasta que el
anterior esté **verificado** por el responsable. Prohibido "implementar todo de golpe".

### R2 — Verificación previa y Git manual

Para cada paso se entrega:

1. Lista exacta de archivos **nuevos** y **modificados** (con el diff propuesto).
2. Comandos de verificación ejecutables y su salida esperada.
3. Checklist de aceptación del paso (§6 y fase correspondiente).

El **commit lo hace el responsable del repositorio**. El agente no ejecuta `git add/commit/push`.

### R3 — Inmutabilidad del código existente

- Prohibido modificar líneas existentes de los microservicios sin autorización explícita.
- Si un test o la documentación revela la necesidad de un cambio, se entrega una **solicitud
  de cambio** con: archivo, línea(s), motivo exacto, riesgo y alternativa sin tocar código.
- **Archivos nuevos** (tests, configs, docs, scripts) no requieren aprobación previa.
- **Archivos existentes modificados** (pom.xml, `application.yml`, controladores, servicios,
  `docs/README.md`, etc.) requieren la aprobación listada en §5.

### R4 — Sin supuestos

Cada caso de test se escribe contra el comportamiento real del código (verificado antes de
escribir la aserción). Si el comportamiento real difiere de `specs/endpoints.md`, gana el
código y se registra la diferencia en §15.

### R5 — Sin cambios de comportamiento

La fase de documentación y tests no debe alterar el comportamiento en runtime. Cualquier
cambio de lógica detectado se propone por separado (R3), no se mezcla con esta línea.

### R6 — Trazabilidad

Todo paso referencia IDs (`DOC-API-*`, `TST-*`, `PM-*`, `US-*`) y el ítem del backlog (§14).

### Formato de entrega por paso

```text
PASO: P1.1 — springdoc en msvc-usuarios
TIPO: nuevo (N) / modificado con aprobación A-xx (M)
ARCHIVOS: ...
VERIFICACIÓN: ...
ACEPTACIÓN: ...
```

---

## 3. Decisiones cerradas

| ID | Decisión | Valor |
|---|---|---|
| D-01 | Alcance de endpoints | Solo E1 implementado y operable |
| D-02 | OpenAPI | Springdoc por msvc (Swagger UI + `/v3/api-docs`) + bundle estático en `Backend/docs/openapi/` |
| D-03 | Gateway | Sin agregación ni springdoc; se documenta ruteo, rutas públicas y RBAC |
| D-04 | Tests | Unitarios puros JUnit 5 + Mockito. **Sin AssertJ** |
| D-05 | Módulos testeados | usuarios, eventos, ventas, entradas, gateway |
| D-06 | Concurrencia | Unitaria (repo fake + `ExecutorService`) en build normal **y** integración MySQL con Testcontainers en perfil `-Pit` |
| D-07 | DoD | `mvn test` verde + cobertura ≥ 90 % (alcance §9.2) + casos de la spec cumplidos |
| D-08 | Postman | Colección v2.1 + environment `local` con auto-login por script |
| D-09 | User stories | Formato/convención del proyecto; E1 detalladas, E2 alto nivel; sin diagramas de secuencia |
| D-10 | Ubicación | Spec en `Backend/specs/`; artefactos en `Backend/docs/` |
| D-11 | IDs | `DOC-API-*`, `TST-*`, `PM-*`, `US-*` (+ `P<n>.<m>` para pasos) |
| D-12 | Git | Commit manual del responsable en cada paso |
| D-13 | Estilo OpenAPI (A-01b) | **O2 — anotaciones aditivas** en controladores/DTOs (solo agrega líneas, no cambia lógica) |
| D-14 | Alcance de cobertura (§9.2) | **Ratificado**: 90 % LINE sobre lógica (`service`, `redis`, `queue`, `mapper`, `util`, `filter`); excluye `dto`, `entity`, `config`, `exception`, `clients`, `*Application` |
| D-15 | Toolchain Maven (A-04) | **M1**: builds con `docker run … maven:3.9-eclipse-temurin-17`; sin cambios en el repo |
| D-16 | Concurrencia (R-05) | Se valida el **último cupo de sector** (comportamiento real del negocio); asiento numerado queda como deuda E2. **Actualizado 2026-11-10:** el asiento numerado ya está implementado (asientos virtuales, ver backlog §6.7); la concurrencia de un mismo asiento se prueba con `ReservaAsientoConcurrenciaTest` (unitario, lock emulado). Sigue pendiente su IT con MySQL real (SEAT-06). |

---

## 4. Estado de partida verificado

Verificado el 2026-11-08 sobre el working tree limpio (`main`, último commit `6cbadcb`).

| Ítem | Estado real | Impacto |
|---|---|---|
| Spring Boot (7 apps) | `4.1.2`, `java.version=17` | springdoc debe ser línea 3.x |
| springdoc/swagger/openapi | **No declarado en ningún pom** | Requiere alta de dependencia (§5 A-01) |
| Test starters | Boot 4 los separa: `spring-boot-starter-*-test` ya presentes en los 7 módulos | JUnit 5 + Mockito disponibles sin cambios de pom |
| AssertJ | Transitivo en los starters | **No se importa** en ningún test (regla D-04) |
| JaCoCo | **No configurado** | Requiere plugin (§5 A-02) |
| Testcontainers | **No declarado** | Requiere dependencias + perfil `-Pit` (§5 A-03) |
| Tests existentes | 1: `MsvcNotificacionesApplicationTests` (context load) | Fuera del alcance E1; no se toca |
| Maven local | 3.8.7; `Backend/README.md` exige 3.9+ | **Resuelto por M1**: `maven:3.9-eclipse-temurin-17` (Maven 3.9.16 + JDK 17.0.20) verificado en P0.1 |
| JDK local | 21.0.12.1 con `release 17` | OK |
| Docker | 29.8.2 + Compose v5.6.0 (+ daemon) | Testcontainers y stack local viables |
| Credenciales seed | `demo1234`, `org1234`, `admin1234`, `acceso1234` (verificadas contra los BCrypt de `usuarios/init.sql`) | Base para auto-login de Postman |
| Puertos | Gateway 8080; msvc 8081–8086; DBs 3307–3311, Mongo 27018, Redis 6380 | Base de `servers` y environments |

---

## 5. Aprobaciones requeridas (bloqueantes)

> Sin estas aprobaciones **no se inicia P1**. Todas implican modificar archivos existentes (R3).
> Cada una se aprueba por separado: `A-01 sí/no`, `A-02 sí/no`, etc.

### A-01 — Dependencia springdoc en 4 `pom.xml`

Archivos: `msvc-usuarios/pom.xml`, `msvc-eventos/pom.xml`, `msvc-ventas/pom.xml`,
`msvc-entradas/pom.xml`.

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.2.0</version>
</dependency>
```

Motivo: única dependencia necesaria para generar `/v3/api-docs` y Swagger UI.
Riesgo: incompatibilidad de springdoc 3.2.0 con Boot 4.1.2 → se valida en P1.1 con un módulo
antes de replicar (fail-fast, ver §7.4).
Reversión: quitar el bloque del pom.

### A-01b — Anotaciones OpenAPI en controladores/DTOs (decisión de estilo)

Dos alternativas, elegir una:

| Opción | Qué implica | Pros / contras |
|---|---|---|
| **O1 — Sin tocar código** | Metadatos globales y parches por operación desde clases nuevas (`OpenAPI` bean + `OpenApiCustomizer`) | Cero cambios en código existente; descripciones/examples viven lejos del endpoint; mayor mantenimiento |
| **O2 — Anotaciones aditivas** (recomendada) | Agregar `@Operation`, `@ApiResponse`, `@Schema` en controladores/DTOs existentes | Documentación de calidad junto al contrato; solo **agrega** líneas (no cambia lógica); requiere aprobación por archivo |

Con O2, la lista exacta de archivos a tocar se entrega al inicio de P1.1 y se aprueba por lote.

> **Ratificado 2026-11-08: O2.** Se agregan anotaciones en archivos existentes; cada lote requiere
> aprobación por archivo antes del diff (R3). Sin cambios de lógica ni de firmas.

### A-02 — JaCoCo en 5 `pom.xml`

Archivos: los 4 msvc + `APIGateway/pom.xml`.

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.90</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
                <excludes>
                    <exclude>**/config/**</exclude>
                    <exclude>**/dto/**</exclude>
                    <exclude>**/entity/**</exclude>
                    <exclude>**/exception/**</exclude>
                    <exclude>**/clients/**</exclude>
                    <exclude>**/*Application.class</exclude>
                </excludes>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Ver §9.2**: el 90 % se aplica al alcance declarado, no al módulo completo.

### A-03 — Testcontainers + perfil `-Pit` en `msvc-ventas/pom.xml`

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-mysql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<!-- versión gestionada por spring-boot-dependencies 4.1.2 → Testcontainers BOM 2.0.5 (verificado en P0.2) -->
```

> **P0.2:** Testcontainers 2.x renombró los artefactos (`testcontainers-mysql` y
> `testcontainers-junit-jupiter`, no `mysql`/`junit-jupiter`). Usar los nombres nuevos.

Más: perfil `<profile><id>it</id>…` con `maven-failsafe-plugin` para `*IT.java`, de modo que
`mvn test` no requiera Docker y `mvn verify -Pit` sí ejecute la integración.
Archivo nuevo: `.mvn/` no; se ejecuta con Maven local/Docker.

### A-04 — Toolchain Maven (sin cambios en el repo)

`mvn 3.8.7` no cumple el mínimo declarado (3.9+). Opciones:

| Opción | Acción | Impacto |
|---|---|---|
| **M1 (recomendada)** | Verificar builds con `docker run --rm -v "$PWD":/w -w /w maven:3.9-eclipse-temurin-17 mvn …` | Cero cambios en el repo; requiere descargar imagen |
| M2 | Instalar Maven 3.9+ en el host | Cambio de entorno, no de repo |
| M3 | Maven Wrapper por módulo | Agrega `mvnw` + `.mvn/` en 7 carpetas (ruido) |

**Aprobación requerida:** confirmar M1/M2/M3.

> **Ratificado 2026-11-08: M1.** Todos los comandos de verificación de esta spec se ejecutan con
> `docker run --rm -v "$PWD":/w -w /w maven:3.9-eclipse-temurin-17 mvn …`. Requiere imagen
> descargada una vez; no modifica el repositorio.

### A-05 — `Backend/docs/README.md` (cierre)

Actualizar la tabla de estado de artefactos al final de P7. Se entrega diff; requiere aprobación.

### A-06 — Actualización de `Backend/specs/backlog.md`

No se modifica. En P7 se entrega una **propuesta de tabla** (IDs → estado `hecho`) para que el
responsable la aplique manualmente.

---

## 6. Mapa de pasos

| Paso | Contenido | Depende de | Aprobación | Estimación |
|---|---|---|---|---|
| **P0.1** | Verificar toolchain y build base (sin escribir en repo) | — | — | 15 min |
| **P0.2** | Confirmar versiones springdoc/Testcontainers y plan de build | P0.1 | A-04 | 10 min |
| **P1.1** | springdoc en usuarios + validación de compatibilidad | A-01, A-01b | sí | 45 min |
| **P1.2** | OpenAPI eventos | P1.1 | A-01b | 60 min |
| **P1.3** | OpenAPI ventas | P1.2 | A-01b | 60 min |
| **P1.4** | OpenAPI entradas | P1.3 | A-01b | 45 min |
| **P1.5** | Documento de gateway (ruteo, públicos, RBAC) | P1.4 | — | 45 min |
| **P2.1** | Exportar specs + bundle consolidado + lint | P1.5 | — | 45 min |
| **P2.2** | README de regeneración + verificación en Swagger UI | P2.1 | — | 20 min |
| **P3.0** | JaCoCo + convenciones de test (sin tests aún) | A-02 | sí | 30 min |
| **P3.1** | Tests usuarios | P3.0 | — | 60 min |
| **P3.2** | Tests eventos | P3.1 | — | 90 min |
| **P3.3** | Tests ventas | P3.2 | — | 120 min |
| **P3.4** | Tests entradas | P3.3 | — | 75 min |
| **P3.5** | Tests gateway | P3.4 | — | 60 min |
| **P3.6** | Cierre de cobertura 90 % (alcance §9.2) | P3.5 | — | 30 min |
| **P4.1** | Concurrencia unitaria (fake repo) | P3.3 | — | 45 min |
| **P4.2** | IT Testcontainers MySQL (`-Pit`) | P4.1, A-03 | sí | 90 min |
| **P4.3** | Informe de concurrencia/performance | P4.2 | — | 30 min |
| **P5.1** | Postman: estructura, environment, auto-login | P1.5 | — | 45 min |
| **P5.2** | Postman: auth + usuarios + eventos | P5.1 | — | 60 min |
| **P5.3** | Postman: ventas + entradas + devoluciones | P5.2 | — | 75 min |
| **P5.4** | Postman: negativos (401/403/409/400) + corrida completa | P5.3 | — | 45 min |
| **P6.1** | Features + inventario de US | — (puede ir en paralelo a P3) | — | 45 min |
| **P6.2** | US E1 con criterios Given/When/Then | P6.1 | — | 120 min |
| **P6.3** | US E2 en alto nivel + revisión de consistencia | P6.2 | — | 45 min |
| **P7.1** | Índice de entrega + trazabilidad + propuesta de backlog | P5.4, P6.3 | A-05 | 30 min |

> Regla R1: un paso por vez. El orden de ejecución es el de la tabla, salvo indicación explícita.

---

## 7. Fase 1 — OpenAPI / Springdoc

### 7.1 Stack

| Ítem | Valor |
|---|---|
| Artefacto | `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0` |
| JSON | `http://localhost:<puerto>/v3/api-docs` |
| YAML | `http://localhost:<puerto>/v3/api-docs.yaml` |
| UI | `http://localhost:<puerto>/swagger-ui/index.html` |
| Gateway | **No** lleva springdoc. Ya whitelistea `/swagger-ui*`, `/v3/api-docs*`, `/webjars/**` en `JwtAuthenticationFilter` (sin cambios) |

### 7.2 Convenciones del contrato

Todas las specs deben cumplir:

1. **Info**: `title` = `TicketYa — <contexto>`, `version` = `1.0.0`, `description` (2–4 líneas),
   `contact` del equipo, `license` (uso académico).
2. **Servers**: local directo (`http://localhost:8081…8084`) y vía gateway
   (`http://localhost:8080/api`, con nota de `StripPrefix=1`). El server de gateway se marca
   con `x-gateway: true`.
3. **Tags**: un tag por controlador/recurso, con descripción.
4. **Seguridad**: `securitySchemes.bearerAuth` (HTTP Bearer JWT) declarado globalmente;
   nota explícita: *la autenticación y el RBAC los aplica el API Gateway; los msvc confían en
   `X-User-*`*. Endpoints públicos → `security: []`.
5. **Errores**: schema reutilizable `ApiError {status, error, message, path, timestamp}` y
   respuestas documentadas según el `GlobalExceptionHandler` real:
   `400` validación/argumento, `401` sin identidad, `403` sin permiso, `404` no encontrado,
   `409` conflicto de negocio, `500` inesperado.
6. **Extensiones** (obligatorias por operación):
   - `x-msvc`: servicio dueño.
   - `x-gateway-path`: path público con `/api`.
   - `x-roles`: roles habilitados (`PUBLIC`, `CLIENTE`, `ORGANIZADOR`, `ADMIN`, `PERSONAL_ACCESO`).
   - `x-idempotency-required`: `true` en `POST /reservas` (key opcional) y `POST /compras` (key obligatoria).
7. **Ejemplos**: request/response reales por operación, usando UUIDs de los seeds
   (`infrastructure/mysql/README.md`) — nunca datos inventados.
8. **Headers**: documentar `Idempotency-Key` y `X-Correlation-Id` donde aplica.
9. **Nombres**: los paths internos del msvc (sin `/api`); el `/api` se agrega en el bundle (§8).

### 7.3 Inventario de endpoints a documentar (62)

**msvc-usuarios — 11**
```
POST /auth/login            POST /auth/register         POST /auth/refresh        POST /auth/logout
GET  /usuarios/me           PATCH /usuarios/me          GET  /usuarios
GET  /usuarios/{id}         PATCH /usuarios/{id}/estado
GET  /usuarios/{id}/roles   PUT  /usuarios/{id}/roles
```

**msvc-eventos — 24**
```
GET  /eventos                              GET  /eventos/{id}
POST /eventos                              PUT  /eventos/{id}
DELETE /eventos/{id}                       POST /eventos/{id}/publicacion
PATCH /eventos/{id}/estado                 PATCH /eventos/{id}/fecha
GET  /eventos/{id}/sectores                PUT  /eventos/{id}/sectores/{sectorId}/precio
GET  /organizadores/me/eventos
GET  /lugares                              GET  /lugares/{id}
POST /lugares                              PUT  /lugares/{id}
POST /lugares/{id}/sectores                PUT  /sectores/{id}
GET  /lugares/{id}/sectores/{sectorId}/asientos
GET  /participantes                        GET  /participantes/{id}
POST /participantes                        PUT  /participantes/{id}
GET  /eventos/{id}/participantes           PUT  /eventos/{id}/participantes
```

**msvc-ventas — 17**
```
GET  /eventos/disponibilidad               GET  /eventos/{id}/disponibilidad
POST /eventos/{id}/cupos                   POST /eventos/{id}/cupos/liberacion
POST /reservas                             GET  /reservas/{id}
POST /reservas/{id}/confirmacion           POST /reservas/{id}/cancelacion
POST /reservas/{id}/renovacion
POST /compras                              GET  /compras
GET  /compras/{id}                         POST /compras/{id}/pagos
GET  /compras/{id}/pagos                   POST /pagos/{id}/reintentos
```

**msvc-entradas — 10**
```
POST /entradas/emision                     GET  /entradas
GET  /entradas/{id}                        GET  /entradas/{id}/qr
POST /entradas/{id}/devoluciones           GET  /devoluciones
GET  /devoluciones/{id}                    POST /devoluciones/{id}/aprobacion
POST /devoluciones/{id}/rechazo            POST /eventos/{idEvento}/reintegro-cancelacion
```

> Los `GET /` de smoke de cada msvc quedan documentados como tag `Smoke` y marcados
> `x-internal: true` (no forman parte del contrato de negocio).

### 7.4 Pasos atómicos

| Paso | Alcance | Criterio de aceptación |
|---|---|---|
| **P1.1** | springdoc en usuarios + `OpenAPI` bean (info/servers/security) + anotaciones O2 | `/v3/api-docs` responde 200 con 12 operaciones en 10 paths (11 de negocio + smoke); Swagger UI carga; `mvn test` sigue verde; ningún endpoint de negocio cambia de comportamiento |
| **P1.2** | eventos | 24 operaciones; schemas de `EventoRequestDTO`/`EventoDetalleResponseDTO` completos; ejemplos con seeds; errores 400/403/404/409 |
| **P1.3** | ventas | 16 operaciones en 14 paths (15 de negocio + smoke); `Idempotency-Key` visible y obligatoria en compras (400 si falta), opcional en reservas; `NivelCupo` documentado con `allowableValues` (constantes, no enum); errores 409 de cupo documentados |
| **P1.4** | entradas | 11 operaciones en 11 paths (10 de negocio + smoke); QR y política de exposición (null si evento CANCELADO o entrada USADA/DEVUELTA/A_REINTEGRAR); 409 de cupo/plazo documentados; devolución 72 h documentada |
| **P1.5** | Documento de gateway | `Backend/docs/api/gateway.md` con tabla de rutas (`order`, `uri`, `StripPrefix`), rutas públicas, matriz RBAC por path/método, headers propagados y 401/403 de ejemplo |

**Validación de compatibilidad (fail-fast):** en P1.1, si springdoc 3.2.0 no arranca con
Boot 4.1.2, se detiene la fase y se propone alternativa (pin de versión, `springdoc` 3.x
posterior o generación estática). No se replica a los otros módulos hasta que P1.1 pase.

> **Operación (P1.1):** el stack completo ya corre bajo el proyecto compose `backend`
> (`Backend/docker-compose.yml` con `include`). Ejecutar `docker compose up` desde el
> directorio de un msvc intenta crear contenedores con los mismos `container_name` y falla
> por conflicto. Para reconstruir un solo servicio usar desde `Backend/`:
> `docker compose build ms-usuarios && docker compose up -d --no-deps --force-recreate ms-usuarios`.
>
> **Lección P1.2 (genéricos):** no fijar `content = @Content(schema = @Schema(implementation = PageResponseDTO.class))`
> en la respuesta de un método que devuelve `PageResponseDTO<T>`: el override con la clase
> cruda borra el genérico y deja `content.items: {}`. Dejar que springdoc infiera del tipo de
> retorno produce `PageResponseDTOEventoResumen` con `items.$ref → EventoResumen`.

---

## 8. Fase 2 — Bundle estático consolidado

### 8.1 Estructura

```text
Backend/docs/openapi/
├── README.md              # versiones, fecha, cómo regenerar, alcance
├── usuarios.yaml          # export crudo de /v3/api-docs.yaml (paths internos)
├── eventos.yaml
├── ventas.yaml
├── entradas.yaml
├── ticketya-api.yaml      # bundle consolidado (paths con /api, servers gateway)
└── bundle.sh              # script reproducible de export + merge
```

### 8.2 Reglas del bundle

- Los YAML por msvc se exportan **sin editar** (paths internos), y se regeneran con `curl`.
- El consolidado:
  - prefija `/api` a cada path (refleja el gateway),
  - `servers`: `https://ticketya.com.ar/api` (prod) + `http://localhost:8080/api` (local),
  - conserva `x-msvc` por operación para trazabilidad,
  - resuelve colisiones: no hay paths duplicados entre servicios (verificado en §7.3),
  - `tags` con prefijo del contexto para evitar ambigüedad.
- Herramienta: `npx @redocly/cli bundle` (sin agregar dependencia al repo) o merge con script.
  Si no hay red/Node, fallback: merge manual documentado y versionado.

### 8.3 Aceptación

- `npx @redocly/cli lint Backend/docs/openapi/*.yaml` sin errores (warnings permitidos y listados).
- El bundle abre en Swagger Editor y en Swagger UI estático sin `$ref` roto.
- Conteo de operaciones del bundle = 62 + smoke.
- `README.md` indica fecha, commit, versión de springdoc y comando exacto de regeneración.

---

## 9. Fase 3 — Tests unitarios JUnit 5 + Mockito

### 9.1 Stack y convenciones

| Regla | Detalle |
|---|---|
| Framework | JUnit 5 (Jupiter) — `org.junit.jupiter.api.*` |
| Mocks | Mockito — `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks` |
| Aserciones | **Solo** `org.junit.jupiter.api.Assertions` (`assertEquals`, `assertThrows`, `assertTrue`, …). **Prohibido AssertJ** |
| Contexto Spring | **No** en unitarios: sin `@SpringBootTest`, sin `@DataJpaTest` |
| Naming | Clase `<ClaseBajoTest>Test`; método `metodo_escenario_resultadoEsperado`; `@DisplayName` en español |
| Agrupación | `@Nested` por método o por regla de negocio |
| Tags | `@Tag("unit")` en unitarios; `@Tag("it")` en integración |
| Determinismo | Sin `Thread.sleep`, sin red, sin reloj real. Para plazos, construir entidades con fechas con margen amplio (≥ 1 h) y stubs de repositorio |
| Datos | Builders/helpers de test propios; UUIDs fijos legibles |
| Excepciones | `assertThrows(Tipo.class, …)` + aserción del mensaje cuando el mensaje es contrato |

> **Restricción real:** los servicios usan `LocalDateTime.now()` interno y no exponen `Clock`.
> No se modifica código para inyectarlo (R3): los casos de vencimiento se arman con fechas en
> el pasado/futuro y stubs de repositorio (`find…Before`, `findByEstado…`).

### 9.2 Política de cobertura (90 %) — ratificada

> **Ratificado 2026-11-08 (D-14).** El gate del 90 % se aplica al alcance de lógica definido abajo.
> No se exige sobre DTOs, entidades ni configuración.

Aplicar 90 % al módulo completo (DTOs, entidades, configuración) obligaría a tests triviales de
getters/setters y no mediría lógica de negocio. Propuesta:

| Ítem | Definición |
|---|---|
| Métrica de gate | `LINE` ≥ **0.90** por módulo |
| Alcance medido | `service/**`, `redis/**`, `queue/**`, `mapper/**`, `util/**`, `filter/**` |
| Exclusiones | `config/**`, `dto/**`, `entity/**`, `exception/**`, `clients/**`, `*Application` |
| Reporte | `target/site/jacoco/index.html` |
| Comando | `mvn verify` (unitarios + gate). `mvn test` corre tests sin gate |
| Meta interna (no bloqueante) | `BRANCH` ≥ 0.80 |
| Excepción | si un paquete del alcance no llega a 90 % por código defensivo no testeable, se documenta en §15 y se aprueba caso por caso |

**Ratificado:** se acepta el alcance/exclusiones de la tabla (D-14). Si un paquete del alcance
no llega a 90 % por código defensivo no testeable, se documenta en §15 y se aprueba caso por caso;
no se agregan tests triviales de getters/setters para inflar la métrica.

### 9.3 Matriz de casos

Convención: `409` = `BusinessConflictException`, `404` = `ResourceNotFoundException`,
`400` = `IllegalArgumentException`, `401` = `UnauthorizedException`, `403` = `ForbiddenException`.
El código HTTP final lo define el `GlobalExceptionHandler` del msvc (se verifica al escribir el test).

#### 9.3.1 msvc-usuarios — `TST-USU-*` (13)

| ID | Clase | Escenario | Resultado esperado |
|---|---|---|---|
| TST-USU-01 | AuthServiceImpl | register válido | usuario con rol CLIENTE, password BCrypt, token emitido |
| TST-USU-02 | AuthServiceImpl | register email existente | `ConflictException` (409) |
| TST-USU-03 | AuthServiceImpl | register sin rol CLIENTE en DB | `ResourceNotFoundException` (404) |
| TST-USU-04 | AuthServiceImpl | login válido | `LoginResponse` con token y datos de sesión |
| TST-USU-05 | AuthServiceImpl | login email inexistente | `UnauthorizedException` (401) |
| TST-USU-06 | AuthServiceImpl | login password incorrecta | `UnauthorizedException` (401) |
| TST-USU-07 | AuthServiceImpl | login usuario inactivo | `UnauthorizedException` (401) |
| TST-USU-08 | AuthServiceImpl | refresh token válido | nuevo token; usuario recargado con roles |
| TST-USU-09 | AuthServiceImpl | refresh token inválido/expirado | `UnauthorizedException` (401) |
| TST-USU-10 | UsuarioServiceImpl | patchMe solo actualiza nombre/apellido/ciudad | email/roles/estado intactos |
| TST-USU-11 | UsuarioServiceImpl | patchMe usuario inexistente | `ResourceNotFoundException` (404) |
| TST-USU-12 | UsuarioServiceImpl | putRoles normaliza `ACCESO`→`PERSONAL_ACCESO` | roles persistidos normalizados |
| TST-USU-13 | UsuarioServiceImpl | putRoles rol inexistente | `ResourceNotFoundException` (404) |
| TST-USU-14 | util.JwtUtil (msvc) | generar/parsear token | claims `userId`, `email`, `rol`, `roles`; expiración coherente |
| TST-USU-15 | util.JwtUtil (msvc) | token con firma alterada / expirado | `validateToken=false` |

#### 9.3.2 msvc-eventos — `TST-EVE-*` (18)

| ID | Clase | Escenario | Resultado esperado |
|---|---|---|---|
| TST-EVE-01 | EventoServiceImpl | crear borrador mínimo (solo nombre) | estado BORRADOR; sin exigir fecha/lugar reales |
| TST-EVE-02 | EventoServiceImpl | crear con `estado=PUBLICADO` | crea, aplica validaciones de publicación y abre cupos (Feign) |
| TST-EVE-03 | EventoServiceImpl | publicar sin fechaInicio | 409 "falta fechaInicio" |
| TST-EVE-04 | EventoServiceImpl | publicar con lugar "Por definir" | 409 "definí el nombre del lugar" |
| TST-EVE-05 | EventoServiceImpl | publicar sin sector con precio | 409 "al menos un tipo de entrada con precio" |
| TST-EVE-06 | EventoServiceImpl | publicar con capacidad < 1 | 400 capacidad ≥ 1 |
| TST-EVE-07 | EventoServiceImpl | publicar OK | estado PUBLICADO; Feign `POST /eventos/{id}/cupos` invocado antes de persistir estado |
| TST-EVE-08 | EventoServiceImpl | publicar con Feign caído | error propagado (fail-hard); estado no queda PUBLICADO |
| TST-EVE-09 | EventoServiceImpl | publicar CANCELADO/FINALIZADO | 409 |
| TST-EVE-10 | EventoServiceImpl | actualizar como dueño | cambios aplicados |
| TST-EVE-11 | EventoServiceImpl | actualizar como ORG no dueño | 403 |
| TST-EVE-12 | EventoServiceImpl | actualizar CANCELADO/FINALIZADO | 409 |
| TST-EVE-13 | EventoServiceImpl | eliminar no BORRADOR | 409 |
| TST-EVE-14 | EventoServiceImpl | patchEstado: ORG suspende propio | SUSPENDIDO |
| TST-EVE-15 | EventoServiceImpl | patchEstado: ORG intenta CANCELADO | 403 |
| TST-EVE-16 | EventoServiceImpl | patchEstado: ADMIN cancela | CANCELADO + Feign reintegro invocado |
| TST-EVE-17 | EventoServiceImpl | patchEstado: BORRADOR → SUSPENDIDO | 409 "publicá el borrador antes" |
| TST-EVE-18 | EventoServiceImpl | patchFecha inválido/estado no editable | 400 / 409 según caso |
| TST-EVE-19 | EventoServiceImpl | búsqueda: default PUBLICADO, `estado=TODOS`, filtros tipo/ciudad/fechas/participante, paginación | resultados y metadatos correctos |
| TST-EVE-20 | EventoServiceImpl | obtener por id inexistente | 404 |
| TST-EVE-21 | EventoServiceImpl | upsert precio sector: sector de otro lugar | 400 |
| TST-EVE-22 | Participantes | asociar como dueño / no dueño | OK / 403 |
| TST-EVE-23 | Participantes | crear sin nombre/tipo | 400 |
| TST-EVE-24 | Lugar/Sector | crear sector con nombre vacío / listar asientos de sector ajeno | 400 / 404 |

#### 9.3.3 msvc-ventas — `TST-VEN-*` (24)

| ID | Clase | Escenario | Resultado esperado |
|---|---|---|---|
| TST-VEN-01 | VentaServiceImpl | disponibilidad con cupos | totales por sector + `nivelCupo` |
| TST-VEN-02 | VentaServiceImpl | disponibilidad sin filas | 404 |
| TST-VEN-03 | VentaServiceImpl | batch: vacío / nulos / duplicados / tope 50 | orden estable, dedupe, máx 50, ítems agotados en 0 |
| TST-VEN-04 | NivelCupo | umbrales 0/19/20/49/50/99/100 | AGOTADO / ULTIMOS_LUGARES / MENOS_DE_50 / MENOS_DE_100 / OK |
| TST-VEN-05 | VentaServiceImpl | abrirCupos válido e idempotente | crea y no duplica; devuelve disponibilidad |
| TST-VEN-06 | VentaServiceImpl | abrirCupos sin sectores / capacidad < 1 / idSector nulo | 400 |
| TST-VEN-07 | VentaServiceImpl | crearReserva OK | PENDIENTE, TTL 10 min, disponible−, reservada+, total correcto |
| TST-VEN-08 | VentaServiceImpl | crearReserva sin stock | 409 "Cupo insuficiente" |
| TST-VEN-09 | VentaServiceImpl | crearReserva evento no PUBLICADO | 409 |
| TST-VEN-10 | VentaServiceImpl | crearReserva excede límite 6/cliente/evento | 409 |
| TST-VEN-11 | VentaServiceImpl | crearReserva sin precio activo de sector | 400 |
| TST-VEN-12 | VentaServiceImpl | crearReserva con `Idempotency-Key` repetida | devuelve la reserva existente (no crea otra) |
| TST-VEN-13 | VentaServiceImpl | confirmar PENDIENTE / estado inválido | CONFIRMADA / 409 |
| TST-VEN-14 | VentaServiceImpl | cancelar reserva PENDIENTE | CANCELADA + libera cupo |
| TST-VEN-15 | VentaServiceImpl | cancelar con compra en curso/aprobada | 409 |
| TST-VEN-16 | VentaServiceImpl | renovar reserva / reserva expirada | extiende TTL / 409 |
| TST-VEN-17 | ReservaExpirationJob | `expirarReservasVencidas` | marca EXPIRADA y libera cupo; devuelve cantidad |
| TST-VEN-18 | VentaServiceImpl | crearCompra sin `Idempotency-Key` | 400 |
| TST-VEN-19 | VentaServiceImpl | crearCompra replay misma key | devuelve la misma compra (no duplica) |
| TST-VEN-20 | VentaServiceImpl | crearCompra con reserva no activa/expirada | 409 |
| TST-VEN-21 | VentaServiceImpl | iniciarPago APROBAR | compra APROBADA, reserva COMPRADA, Feign emisión invocado |
| TST-VEN-22 | VentaServiceImpl | iniciarPago RECHAZAR | compra RECHAZADA, cupo liberado, sin emisión |
| TST-VEN-23 | VentaServiceImpl | pago con resultado inválido / compra no pagable | 400 / 409 |
| TST-VEN-24 | VentaServiceImpl | reintento: solo desde RECHAZADO; sin reserva activa | 409 |
| TST-VEN-25 | VentaServiceImpl | liberarCupoDevolucion: cantidad ≥ 1 / cupo inexistente / vendida insuficiente | OK / 404 / 409 |
| TST-VEN-26 | StockCache | hold de reserva e idempotencia | claves y TTL correctos (Redis mockeado) |
| TST-VEN-27 | VentaServiceImpl | Feign eventos caído al reservar | excepción controlada, sin mutar cupo |

#### 9.3.4 msvc-entradas — `TST-ENT-*` (16)

| ID | Clase | Escenario | Resultado esperado |
|---|---|---|---|
| TST-ENT-01 | EntradaServiceImpl | emisión OK (2 items) | N entradas VENDIDA, QR único, `precio` persistido |
| TST-ENT-02 | EntradaServiceImpl | emisión repetida misma compra | idempotente: no duplica |
| TST-ENT-03 | EntradaServiceImpl | emisión items vacíos / cantidad 0 | 400 |
| TST-ENT-04 | EntradaServiceImpl | listar propias | solo entradas del `X-User-Id` |
| TST-ENT-05 | EntradaServiceImpl | obtener propia / ajena / admin | OK / 403 / OK |
| TST-ENT-06 | EntradaServiceImpl | obtener inexistente | 404 |
| TST-ENT-07 | EntradaServiceImpl | QR de VENDIDA con evento vigente | QR devuelto |
| TST-ENT-08 | EntradaServiceImpl | QR de USADA / DEVUELTA / A_REINTEGRAR / evento cancelado | 409 |
| TST-ENT-09 | EntradaServiceImpl | solicitar devolución dentro de 72 h | devolución PENDIENTE |
| TST-ENT-10 | EntradaServiceImpl | devolución fuera de plazo | 409 |
| TST-ENT-11 | EntradaServiceImpl | devolución de entrada USADA | 409 |
| TST-ENT-12 | EntradaServiceImpl | segunda devolución PENDIENTE | 409 |
| TST-ENT-13 | EntradaServiceImpl | aprobar devolución | entrada DEVUELTA + Feign libera cupo; si Feign falla, no cambia estado |
| TST-ENT-14 | EntradaServiceImpl | rechazar devolución | devolución RECHAZADA, entrada sigue VENDIDA |
| TST-ENT-15 | EntradaServiceImpl | aprobar/rechazar no PENDIENTE | 409 |
| TST-ENT-16 | EntradaServiceImpl | reintegro por cancelación | solo VENDIDA → A_REINTEGRAR; devuelve cantidad |
| TST-ENT-17 | EntradaServiceImpl | devoluciones: cliente propias / admin pendientes | listas correctas |

#### 9.3.5 APIGateway — `TST-GW-*` (12)

| ID | Clase | Escenario | Resultado esperado |
|---|---|---|---|
| TST-GW-01 | JwtAuthenticationFilter | `GET /` y `OPTIONS *` | sin autenticación |
| TST-GW-02 | JwtAuthenticationFilter | públicos: `POST /api/auth/login|register` | pasan sin token |
| TST-GW-03 | JwtAuthenticationFilter | `GET /api/eventos`, `/api/eventos/{id}`, `/api/eventos/{id}/participantes`, `/api/participantes` | públicos |
| TST-GW-04 | JwtAuthenticationFilter | ruta protegida sin `Authorization` | 401 JSON `{error,message}` |
| TST-GW-05 | JwtAuthenticationFilter | token inválido/expirado | 401 |
| TST-GW-06 | JwtAuthenticationFilter | `CLIENTE` → `GET /api/usuarios` | 403 |
| TST-GW-07 | JwtAuthenticationFilter | `ADMIN` → `GET /api/usuarios` | pasa |
| TST-GW-08 | JwtAuthenticationFilter | `CLIENTE` en self-service (`/api/compras`, `/api/entradas`, `/api/devoluciones`, `/api/reservas`) | pasa |
| TST-GW-09 | JwtAuthenticationFilter | `PERSONAL_ACCESO` en `POST /api/eventos/{id}/validaciones` | pasa; en `/api/usuarios` 403 |
| TST-GW-10 | JwtAuthenticationFilter | propagación de headers | `X-User-Id/Email/Role/Roles` presentes y correctos |
| TST-GW-11 | JwtAuthenticationFilter | aprobación/rechazo de devolución por CLIENTE | 403 (admin-only) |
| TST-GW-12 | JwtUtil (gateway) | generar/validar/expirar/ firma inválida | comportamiento esperado |
| TST-GW-13 | SecurityConfig | preflight CORS desde `:5173` y `:3000` | 200 + headers permitidos |

**Total unitarios: ~95 casos.** Se pueden agrupar en un método por regla cuando compartan setup,
sin bajar la cobertura del alcance.

### 9.4 Pasos y aceptación

- **P3.0**: JaCoCo + helpers de test + `README` de convenciones (`Backend/docs/testing/README.md`).
- **P3.1–P3.5**: un módulo por paso. Aceptación por paso:
  1. `mvn test` del módulo verde (todos los casos de su tabla).
  2. `mvn verify` genera reporte y cumple el gate del alcance §9.2.
  3. Sin `AssertJ`, sin `@SpringBootTest`, sin red.
  4. Casos de la tabla implementados con el mismo ID en `@DisplayName`.
- **P3.6**: verificación agregada de los 5 módulos + tabla de cobertura real vs. objetivo.

---

## 10. Fase 4 — Concurrencia

### 10.1 P4.1 — Unitaria (build normal, sin Docker)

**Clase:** `VentaServiceImplConcurrenciaTest` (`@Tag("unit")`).

| ID | Escenario | Resultado esperado |
|---|---|---|
| TST-CONC-01 | 50 hilos reservan 1 entrada sobre **la última unidad** de un sector | exactamente **1** reserva exitosa, 49 `BusinessConflictException`, `disponible=0`, `reservada=1`, nunca negativo |
| TST-CONC-02 | 20 hilos reservan 1 unidad con disponibilidad 5 | exactamente 5 exitosas, 15 conflictos, sin sobreventa |
| TST-CONC-03 | compras concurrentes sobre la misma reserva | una sola compra efectiva (idempotencia + estado) |

**Implementación:** repositorio fake en memoria (`CupoSectorRepository`) que serializa el acceso
con `synchronized`/`ReentrantLock` (equivalente al lock pesimista), `ExecutorService` con
`CountDownLatch` de arranque simultáneo, timeout por test. Se mockean Feign y `StockCache`.
Se documenta la limitación: el fake no prueba el motor de BD; eso lo cubre P4.2.

### 10.2 P4.2 — Integración MySQL con Testcontainers (`-Pit`)

**Clase:** `VentaConcurrenciaIT` (`@Tag("it")`, ejecutada por Failsafe).

- Contenedor `mysql:8.4` (misma versión que el compose del repo) + `withInitScript("sql/ventas-init.sql")`
  (copia versionada del init real; se documenta que debe mantenerse sincronizada con
  `infrastructure/mysql/ventas/init.sql`). Imagen ya disponible en el host.
- Spring context de test con datasource del contenedor; `StockCache` mockeado (sin Redis).
- Escenarios:
  1. 50 transacciones reales sobre el último cupo → 1 éxito, 49 conflictos.
  2. Verificación de que `SELECT … FOR UPDATE` serializa: sin valores negativos ni sobreventa.
  3. Medición informativa: p50/p95 de latencia y throughput del escenario.
- Comando: `mvn verify -Pit` (requiere Docker y red para la imagen).
- Aceptación: 0 sobreventas, 0 valores negativos, informe generado.

### 10.3 P4.3 — Informe

`Backend/docs/testing/concurrencia.md` con: entorno (Docker/JDK/MySQL), escenario, hilos,
resultados, p50/p95, errores esperados, conclusión y limitaciones. Sin meta de performance
bloqueante: el gate es **no sobreventa**.

---

## 11. Fase 5 — Postman

### 11.1 Archivos

```text
Backend/docs/postman/
├── TicketYa-E1.postman_collection.json      # v2.1
├── TicketYa-local.postman_environment.json
└── README.md                                # importar, orden de corrida, credenciales
```

### 11.2 Environment `local`

| Variable | Valor |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `token`, `token_admin`, `token_org`, `token_acceso` | se setean por script |
| `userId`, `eventId`, `lugarId`, `sectorId` | se capturan de respuestas |
| `reservaId`, `compraId`, `entradaId`, `devolucionId` | se capturan de respuestas |
| `idempotencyKey` | `{{$guid}}` por request |

Credenciales verificadas contra los seeds: `demo1234`, `org1234`, `admin1234`, `acceso1234`.

### 11.3 Carpetas y cobertura

| Carpeta | Requests |
|---|---|
| 00 Auth | login (3 roles), register, refresh, logout, negativos (401) |
| 01 Usuarios | me, patch me, listar, get, estado, get/put roles; 403 con CLIENTE |
| 02 Eventos | catálogo con filtros, detalle, crear borrador, editar, publicar, patch estado/fecha, sectores/precio, organizador |
| 03 Lugares y participantes | CRUD lugares, crear sector, asientos, participantes y asociación |
| 04 Ventas | disponibilidad, batch, abrir cupos, reserva, confirmación, cancelación, renovación, compra, pago APROBAR/RECHAZAR, reintento |
| 05 Entradas | emisión (admin/ventas), listar, detalle, QR |
| 06 Devoluciones | solicitar, listar, aprobar, rechazar, reintegro por cancelación |
| 07 Negativos | sin token (401), rol insuficiente (403), sin `Idempotency-Key` (400), recurso inexistente (404), conflicto (409) |
| 08 Gateway | ruteo, públicos, preflight CORS |

### 11.4 Scripts

- **Pre-request (colección)**: si `token` está vacío o vence (decodificar `exp`), hace
  `POST /api/auth/login` con `demo@ticketya.com` y guarda `token`; idem para admin/org/acceso.
- **Tests por request**: status esperado, `Content-Type`, campos obligatorios del schema,
  captura de IDs y `pm.test` con nombre en español.
- Sin dependencias externas (no Newman en esta tanda).

### 11.5 Aceptación

- Importar colección + environment en Postman y correr carpetas 00→08 contra el stack local
  (`docker compose up -d`): **todas verdes**, incluidos los negativos.
- Cada request declara su rol esperado y el status.
- `README.md` explica el orden (publicar evento → abrir cupos → reservar → comprar → pagar →
  entradas → devolución) y cómo resetear datos (reseeds de `infrastructure/`).

---

## 12. Fase 6 — User Stories

### 12.1 Convención (heredada del proyecto)

- Español, Markdown, tablas con `ID | … | Entrega | Estado | Notas` (igual que `backlog.md`).
- Encabezado por feature, historias por rol, criterios en **Dado que / Cuando / Entonces**.
- IDs: `US-F<nn>-<nn>`; features `F-01…`.
- Sin diagramas de secuencia (tanda posterior).
- Fuente de verdad funcional: `specs/consigna.md`, `specs/reglas-de-negocio.md`, `specs/endpoints.md`.

### 12.2 Inventario

| Feature | E1 | E2 (alto nivel) |
|---|---|---|
| F-01 Identidad y sesión | login, registro, refresh, logout, perfil | — |
| F-02 Administración de usuarios | listar, estado, roles | — |
| F-03 Catálogo y búsqueda | filtros, paginación, detalle, participantes | preferencias/recs |
| F-04 Gestión de eventos | lugares, sectores, precios, borrador, publicar, editar, suspender | reportes |
| F-05 Compra | disponibilidad, reserva, compra, pago mock, límite 6 | fila virtual |
| F-06 Entradas y QR | emisión, mis entradas, QR | transferencia |
| F-07 Devoluciones y cancelación | 72 h, aprobación/rechazo, reintegro | notificaciones |
| F-08 Acceso | — | validación QR (PERSONAL_ACCESO) |
| F-09 Plataforma | gateway, RBAC, CORS | observabilidad, resiliencia |

### 12.3 Formato de cada historia

```markdown
### US-F05-03 — Reservar entradas de un evento publicado

| Campo | Valor |
|---|---|
| Rol | CLIENTE (también ORGANIZADOR como comprador) |
| Entrega | E1 |
| Prioridad | Alta |
| Estado | implementada |
| Endpoint | POST /api/reservas |
| Reglas | cupo suficiente, evento PUBLICADO, máx. 6 por cliente/evento, TTL 10 min |

**Criterios de aceptación**
1. **Dado** un evento PUBLICADO con cupo disponible, **cuando** el cliente reserva N entradas,
   **entonces** se crea una reserva PENDIENTE con vencimiento a 10 minutos y el cupo se descuenta.
2. **Dado** un sector sin cupo suficiente, **cuando** se intenta reservar, **entonces** responde
   409 y no se modifica ningún cupo.
3. **Dado** un cliente que ya tiene 6 entradas para el evento, **cuando** intenta reservar otra,
   **entonces** responde 409.
```

### 12.4 Aceptación

- Todas las features E1 con al menos una historia por rol involucrado.
- Cada historia E1 con ≥ 2 criterios verificables y referencia al endpoint real.
- E2 listado con alcance y dependencia, sin criterios detallados.
- Revisión de consistencia contra `reglas-de-negocio.md` y los tests (trazabilidad US ↔ TST).

---

## 13. Fase 7 — Cierre de entrega

1. Verificación integral: `mvn verify` en los 5 módulos + `mvn verify -Pit` en ventas.
2. Chequeo de artefactos: `Backend/docs/openapi/`, `postman/`, `testing/`,
   `api/gateway.md`.
3. Diff propuesto de `Backend/docs/README.md` (A-05) con estados finales.
4. Tabla propuesta de actualización de `Backend/specs/backlog.md` (A-06) para aplicar manualmente.
5. Resumen de cierre: qué quedó hecho, qué quedó fuera, deudas detectadas, próximos pasos.

**Definición de terminado (DoD) de la línea:**

- [ ] OpenAPI vivo en los 4 msvc y bundle consolidado que lintéa sin errores.
- [ ] 100 % de los casos §9.3 implementados y verdes.
- [ ] Cobertura ≥ 90 % en el alcance ratificado (§9.2) en los 5 módulos.
- [ ] Concurrencia unitaria verde y `-Pit` verde (0 sobreventas).
- [ ] Colección Postman corre completa contra el stack local.
- [ ] US E1 con criterios de aceptación + E2 en alto nivel.
- [ ] Índice de entrega actualizado y trazabilidad completa.
- [ ] Commits manuales del responsable, uno por paso.

---

## 14. Trazabilidad con el backlog

| Ítem backlog | Entregable de esta spec | Pasos |
|---|---|---|
| API-03 OpenAPI/Swagger | Fases 1 y 2 | P1.1–P2.2 |
| INF-17 Postman | Fase 5 | P5.1–P5.4 |
| E1-TST-01 unitarias (compra, devoluciones, búsqueda) | Fase 3 | P3.1–P3.6 |
| E1-TST-02 concurrencia | Fase 4 | P4.1–P4.3 |
| E1-REL-01 datos + Postman + README | Fases 5 y 7 | P5.x, P7.1 |
| US-01 features + US | Fase 6 | P6.1–P6.3 |
| US-02 criterios E1 | Fase 6 | P6.2 |
| DOC-MS-GW | P1.5 (`docs/api/gateway.md`) | P1.5 |
| INF-19/ARQ-* | **Fuera de alcance** (tanda posterior) | — |

---

## 15. Riesgos y decisiones abiertas

| ID | Riesgo / decisión | Impacto | Mitigación |
|---|---|---|---|
| R-01 | springdoc 3.2.0 incompatible con Boot 4.1.2 | Bloquea Fase 1 | Validar en P1.1 antes de replicar; plan B: versión posterior o spec estática |
| R-02 | Maven 3.8.7 < 3.9 requerido | Build/tests pueden fallar | A-04 (Docker Maven 3.9) |
| R-03 | 90 % de cobertura sobre módulo completo es inviable | Gate en rojo permanente | Ratificar alcance/exclusiones §9.2 |
| R-04 | Código sin `Clock` inyectado | Tests de TTL dependen de fechas relativas | Fechas con margen amplio + stubs; no se modifica código |
| R-05 | Concurrencia real no es por asiento (solo cupo de sector) | La consigna menciona "mismo asiento" | **Aceptado (D-16)**: se prueba "último cupo de sector"; el asiento numerado queda como deuda E2 documentada. **Mitigado 2026-11-10:** asiento numerado implementado + test de concurrencia por asiento (unitario); falta IT con MySQL real |
| R-06 | IT Testcontainers requiere Docker + red | P4.2 puede no correr en otro entorno | Perfil `-Pit` separado; `mvn test` nunca depende de Docker |
| R-07 | Auto-login Postman depende de seeds intactos | Colección frágil tras reseeds | `README` indica reseed y variables por environment |
| R-08 | Documentar 62 endpoints a mano puede desincronizarse | Spec obsoleta | Bundle regenerable por script + verificación de conteo en P2.2 |
| R-09 | `application.yml` por msvc necesita bloque `springdoc` | Modifica archivo existente | Incluido en el lote de aprobación A-01; alternativa: perfil externo (se evalúa en P1.1) |
| R-10 | `GET /eventos` del front exige login aunque el catálogo es público | Diferencia FE/BE documentada | Se registra en US F-03; no se corrige en esta línea |

**Decisiones ratificadas el 2026-11-08 (D-13 a D-16):**

1. **A-01b**: O2 — anotaciones aditivas en controladores/DTOs (aprobación por lote de archivos).
2. **§9.2**: 90 % LINE sobre paquetes de lógica con las exclusiones de la tabla.
3. **A-04**: M1 — Docker Maven 3.9+ sin cambios en el repo.
4. **R-05**: la concurrencia valida el "último cupo de sector"; asiento numerado = deuda E2 *(superado el 2026-11-10: ver changelog 1.2)*.

**Pendiente real para arrancar:** aprobación de las modificaciones de `pom.xml`
(A-01 springdoc, A-02 JaCoCo, A-03 Testcontainers) y salir de Plan mode para ejecutar P0.1.

---

## 16. Changelog

| Versión | Fecha | Cambio |
|---|---|---|
| 1.0 | 2026-11-08 | Versión inicial con decisiones cerradas, fases, matrices de test y aprobaciones |
| 1.1 | 2026-11-08 | Ratificados D-13 (O2), D-14 (alcance cobertura), D-15 (M1) y D-16 (concurrencia por cupo de sector) |
| 1.2 | 2026-11-10 | D-16/R-05 actualizados: asientos virtuales en sectores numerados implementados (eventos, ventas, entradas, frontend). Cambia el contrato de `POST /reservas` (`asientos`), `GET /eventos/{id}/disponibilidad` (`asientosOcupados`), `POST /entradas/emision` (`asientos`), `POST /eventos/{id}/cupos/liberacion` (`asientos`) y los sectores (`filas`, `asientosPorFila`). El bundle OpenAPI y la colección Postman requieren regeneración/ampliación (SEAT-04) |

---

## 17. Registro de ejecución

| Paso | Fecha | Resultado | Evidencia |
|---|---|---|---|
| P0.1 | 2026-11-08 | **OK** | `maven:3.9-eclipse-temurin-17` → Maven 3.9.16 + JDK 17.0.20. Build base de `msvc-usuarios` con `mvn -B -ntp -DskipTests package` → **BUILD SUCCESS** (21.9 s). Artefacto `msvc-usuarios-0.0.1-SNAPSHOT.jar` (73 MB) generado con owner del usuario host. |
| P0.2 | 2026-11-08 | **OK** | springdoc `3.2.0` resuelve en Maven Central (HTTP 200) y su POM referencia artefactos de Boot 4. Testcontainers gestionado por Boot 4.1.2 → BOM **2.0.5**, artefactos `testcontainers-mysql` / `testcontainers-junit-jupiter`. Imagen `mysql:8.4` ya presente localmente. |
| P1.1 | 2026-11-08 | **OK** | springdoc `3.2.0` + `OpenApiConfig` + anotaciones O2 en `msvc-usuarios`. `/v3/api-docs` → HTTP 200, OpenAPI 3.1.0, **12 operaciones / 10 paths**, 3 servers (directo, gateway local, gateway prod), `bearerAuth`, 8 schemas (`ApiError`, `LoginRequest/Response`, `RegisterRequest`, `RolesUpdate`, `UserSession`, `Usuario`, `UsuarioUpdate`). Swagger UI y `swagger-config` → 200. Login vía gateway (`POST /api/auth/login`, `demo@ticketya.com`) → 200. Build `mvn package -DskipTests` → **BUILD SUCCESS**. |
| P1.4 | 2026-11-08 | **OK** (con reparación de entorno) | springdoc + anotaciones O2 en `msvc-entradas`. `/v3/api-docs` → 200, OpenAPI 3.1.0, **11 operaciones / 11 paths** (10 de negocio + smoke), 6 schemas, 1 pública (smoke) y 10 con `bearerAuth`. Estados: entrada `VENDIDA | USADA | DEVUELTA | A_REINTEGRAR`; devolución `PENDIENTE | APROBADA | RECHAZADA | EN_CURSO`. QR: null si evento CANCELADO (verificado) o entrada no VENDIDA. Devolución: plazo 72 h desde emisión → 409 "Plazo de devolución vencido" (verificado); RBAC: solo ADMIN aprueba/rechaza, org → 403 (verificado). Swagger UI y YAML → 200. Smoke: `GET /api/entradas` (demo, 2 entradas: QR presente en evento activo / null en cancelado). **Entorno:** el volumen `backend_entradas_data` no tenía tablas (`missing table [devolucion]`); se aplicaron `init.sql` + `03-reseed-clean-demo.sql` (idempotentes, sin datos previos) y el servicio quedó `Up`. |
| P1.3 | 2026-11-08 | **OK** (con reparación de entorno) | springdoc + anotaciones O2 en `msvc-ventas`.
| P1.2 | 2026-11-08 | **OK** (con reparación de entorno) | springdoc + anotaciones O2 en `msvc-eventos`. `/v3/api-docs` → 200, OpenAPI 3.1.0, **25 operaciones / 17 paths** (24 de negocio + smoke), 19 schemas, 6 operaciones públicas y 19 con `bearerAuth` (coincide con el gateway). `PageResponseDTO<EventoResponseDTO>` resuelve a `PageResponseDTOEventoResumen` con `content.items.$ref → EventoResumen`. Swagger UI y YAML → 200. Smoke: `GET /api/eventos` (12 visibles de 14) y `GET /api/eventos/{id}` → 200; `GET /api/organizadores/me/eventos` con `org@ticketya.com` → 200 (14 eventos). **Entorno:** el volumen `backend_eventos_data` estaba incompleto (solo `participante`, vacía; `ms-eventos` llevaba horas en `Exited (1)` por `missing table [asiento]`). Se aplicaron `init.sql` + `03-reseed-clean-demo.sql` (idempotentes, sin datos previos que perder) y el servicio quedó `Up`. |

Comando de build verificado (M1):

```bash
docker run --rm -u "$(id -u):$(id -g)" -e HOME=/tmp \
  -v "$HOME/.m2:/var/m2" \
  -v "/home/soledad/Escritorio/ticketYa/ticketYa/Backend":/w \
  -w /w/<msvc> \
  maven:3.9-eclipse-temurin-17 \
  mvn -B -ntp -Dmaven.repo.local=/var/m2/repository <goals>
```

> La imagen imprime un warning `mkdir: cannot create directory '/root'` (script de entrada) y
> continúa: el repo local se fuerza con `-Dmaven.repo.local`, por lo que el warning es inocuo.
