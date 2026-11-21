# Plantilla DOC-MS-00 — documentación de contexto por microservicio

Plantilla común para `<msvc>/docs/README.md`. El objetivo es que cada microservicio tenga un
documento de contexto comparable, sin duplicar las fuentes canónicas del monorepo.

## Reglas de uso

- Copiar esta estructura en `Backend/<msvc>/docs/README.md`.
- Enlazar, no duplicar: el contrato HTTP vive en `<msvc>/docs/endpoints.md` y en
  `docs/openapi/<msvc>.json`; el modelo en `docs/modelo-datos.md`; los mensajes en
  `specs/comunicacion.md`; las reglas en `specs/reglas-de-negocio.md`.
- Si una sección no aplica, escribir **“No aplica”** y el motivo. No dejarla vacía.
- Actualizar la fecha del encabezado cuando cambie el contexto, la API o el modelo.
- Usar `hecho` / `parcial` / `pendiente` solo para deuda, no para describir el contrato.

## Encabezado

| Campo | Valor |
| --- | --- |
| Microservicio | `<msvc>` |
| Bounded context | `<responsabilidad principal>` |
| Puerto local | `<puerto>` |
| Base de datos | `<MySQL/Mongo/Redis> — <nombre>` |
| Mensajería | `<RabbitMQ: colas/routing keys o “No aplica”>` |
| Consumidores | `<gateway, front, otros msvc>` |
| Actualizado | `<AAAA-MM-DD>` |

**Fuentes canónicas**

- API: `<msvc>/docs/endpoints.md` y `docs/openapi/<msvc>.json`
- Modelo: `docs/modelo-datos.md`
- Mensajes: `specs/comunicacion.md`
- Reglas: `specs/reglas-de-negocio.md`
- Arquitectura: `docs/arquitectura.md`
- Gateway: `docs/api/gateway.md`

## 1. Alcance y límites

- **Es responsable de:** `<datos y operaciones que posee>`.
- **No es responsable de:** `<datos/operaciones de otros servicios>`.
- **Consumidores:** `<quién lo llama y para qué>`.
- **Dependencias síncronas:** `<Feign y motivo>`.
- **Dependencias asíncronas:** `<colas y motivo>`.

## 2. Agregados e invariantes

| Agregado / entidad raíz | Entidades relacionadas | Invariantes principales |
| --- | --- | --- |
| `<Agregado>` | `<hijas>` | `<reglas que siempre se cumplen>` |

Máquinas de estado relevantes:

```text
<ESTADO_A> -> <ESTADO_B> -> <ESTADO_C>
```

## 3. API pública

Resumen de recursos (el detalle vive en `endpoints.md`):

| Método | Path | Rol / auth | Uso |
| --- | --- | --- | --- |
| `<GET>` | `</recurso>` | `<público / JWT / ADMIN>` | `<descripción>` |

- **Endpoints internos (Feign / fuera del gateway):** `<...>`.
- **Errores:** payload uniforme y códigos documentados en `endpoints.md`.
- **OpenAPI:** `/v3/api-docs` y `/swagger-ui/index.html`.

## 4. Modelo local

| Tabla / colección | Clave | Índices / restricciones | Notas |
| --- | --- | --- | --- |
| `<nombre>` | `<PK>` | `<UK, índices>` | `<referencias lógicas>` |

- **Referencias lógicas:** `<ids de otros servicios, sin FK física>`.
- **Datos no relacionales:** `<Redis/Mongo, TTL o índices>`.
- **Migraciones/seeds:** `<init.sql / init.js>`.

## 5. Eventos y mensajes

| Dirección | Routing key / cola | Tipo | Idempotencia | Efecto |
| --- | --- | --- | --- | --- |
| `<publica/consume>` | `<routing key / cola>` | `<tipo>` | `<clave>` | `<qué provoca>` |

- **Exchange/DLX:** `<...>`.
- **Reintentos:** `<...>`.
- **Contrato completo:** `specs/comunicacion.md`.

## 6. Reglas y criterios de aceptación

| Regla / escenario | Resultado esperado | Test que lo cubre |
| --- | --- | --- |
| `<dado ... cuando ... entonces ...>` | `<status/estado>` | `<ClaseTest / IT / Postman>` |

- Enlazar las reglas de negocio transversales a `specs/reglas-de-negocio.md`.
- Distinguir validaciones del servicio de las que aplica el gateway.

## 7. Operación

```bash
# Stack completo
cd Backend
docker compose up -d --build

# Solo el módulo (con su DB levantada)
cd <msvc>
mvn -DskipTests spring-boot:run

# Tests
mvn test              # unitarios, sin Docker
mvn -Pit verify       # ITs con Testcontainers (requiere Docker)
```

- **Variables de entorno:** `<...>`.
- **Smoke:** `GET /` y `GET /v3/api-docs`.
- **Seeds:** `<usuarios/eventos de prueba>`.

## 8. Deuda y decisiones

| Tema | Estado | Nota / enlace |
| --- | --- | --- |
| `<deuda>` | `pendiente/parcial` | `<backlog o spec>` |

## Checklist DOC-MS-00

- [ ] Encabezado completo y fecha actualizada.
- [ ] Alcance y límites explícitos.
- [ ] Agregados e invariantes.
- [ ] API pública enlazada a `endpoints.md` y OpenAPI.
- [ ] Modelo local enlazado a `docs/modelo-datos.md`.
- [ ] Eventos/mensajes enlazados a `specs/comunicacion.md`.
- [ ] Criterios de aceptación con tests.
- [ ] Operación: puerto, DB, variables, comandos y smoke.
- [ ] Deuda y decisiones.
