# Contexto — msvc-eventos

| Campo | Valor |
| --- | --- |
| Microservicio | `msvc-eventos` |
| Bounded context | Catálogo de eventos, lugares/sectores, participantes y ciclo de publicación |
| Puerto local | `8082` |
| Base de datos | MySQL `eventos` (host `3308`) |
| Mensajería | RabbitMQ: publica eventos de dominio en `eventos.exchange` |
| Consumidores | `APIGateway`, frontend, `msvc-ventas` y `msvc-entradas` por Feign, `msvc-notificaciones` por broker |
| Actualizado | `2026-11-21` |

**Fuentes canónicas**

- API: [`endpoints.md`](endpoints.md) y [`docs/openapi/msvc-eventos.json`](../../docs/openapi/msvc-eventos.json)
- Modelo: [`docs/modelo-datos.md`](../../docs/modelo-datos.md) §2
- Mensajes: [`specs/comunicacion.md`](../../specs/comunicacion.md) §`evento.#`
- Reglas: [`specs/reglas-de-negocio.md`](../../specs/reglas-de-negocio.md) §4
- Arquitectura: [`docs/arquitectura.md`](../../docs/arquitectura.md)

## 1. Alcance y límites

- **Es responsable de:** catálogo y búsqueda de eventos, alta/edición de borradores, publicación y cambios de estado, lugares y sectores, participantes y asociación a eventos, flag de alta demanda y publicación de eventos de dominio.
- **No es responsable de:** cupos/stock (msvc-ventas), emisión/QR/devoluciones (msvc-entradas), notificaciones (msvc-notificaciones) ni autenticación (gateway/usuarios).
- **Consumidores:** gateway para `/api/eventos/**`, `/api/lugares/**`, `/api/sectores/**`, `/api/participantes/**` y `/api/organizadores/**`; el frontend para catálogo y gestión; ventas/entradas por Feign.
- **Dependencias síncronas:** `msvc-ventas` (apertura de cupos al publicar, fail-hard; sincronización de fila) y `msvc-entradas` (reintegro al cancelar, best-effort).
- **Dependencias asíncronas:** publica en `eventos.exchange`; no consume colas.

## 2. Agregados e invariantes

| Agregado / entidad raíz | Entidades relacionadas | Invariantes principales |
| --- | --- | --- |
| `Evento` | `PrecioSector` | `estado ∈ {BORRADOR, PUBLICADO, AGOTADO, SUSPENDIDO, CANCELADO, FINALIZADO}`; publicar exige nombre, fecha futura, lugar real y ≥1 sector activo con capacidad ≥1; no se edita CANCELADO/FINALIZADO |
| `Lugar` | `Sector` | Sector numerado con `filas` × `asientosPorFila`; capacidad derivada; el evento elige sectores del lugar y define su cupo (`precio_sector.cupo`) |
| `Participante` | `EventoParticipante` | Nombre único (sin distinguir mayúsculas); rol `PRINCIPAL`/`TELONERO` y orden por evento; editar/eliminar solo ADMIN |

Reglas adicionales: si el cupo total de los sectores vendidos supera **5000**, la publicación fuerza `esAltaDemanda=true`; toda salida de `PUBLICADO` pausa la fila virtual y la publicación/republicación la reanuda.

## 3. API pública

| Método | Path | Rol / auth | Uso |
| --- | --- | --- | --- |
| `GET` | `/eventos` | Público | Catálogo paginado y filtros |
| `GET` | `/eventos/{id}` | Público | Detalle con sectores/precios |
| `POST/PUT/DELETE` | `/eventos...` | ORGANIZADOR dueño / ADMIN | Alta, edición y baja de borradores |
| `POST` | `/eventos/{id}/publicacion` | ORGANIZADOR dueño / ADMIN | Publicar |
| `PATCH` | `/eventos/{id}/estado`, `/eventos/{id}/fecha` | ORG (suspender propios) / ADMIN | Ciclo de vida |
| `GET` | `/organizadores/me/eventos` | ORGANIZADOR | Eventos propios |
| `GET/POST/PUT/DELETE` | `/lugares...`, `/sectores...` | Lectura JWT; escritura ADMIN | Lugares y sectores |
| `GET/POST/PUT/DELETE` | `/participantes...` | GET público; alta ORG/ADMIN; edición/baja ADMIN | Participantes |

El detalle completo está en [`endpoints.md`](endpoints.md). Swagger: `http://localhost:8082/swagger-ui/index.html`.

## 4. Modelo local

| Tabla | Clave | Índices / restricciones | Notas |
| --- | --- | --- | --- |
| `lugar` | `id` | — | Dirección, ciudad, coordenadas opcionales |
| `sector` | `id` | FK lógica a `lugar` | Numerado (`filas`, `asientos_por_fila`) o general |
| `evento` | `id` | `id_lugar`, `id_organizador` lógicos | Estado, fechas, `es_alta_demanda`, `imagen_url` |
| `precio_sector` | `id` | UK evento/sector | Precio activo y cupo del evento en el sector |
| `participante` | `id` | UK `nombre` | Artista, banda, equipo, etc. |
| `evento_participante` | `(id_evento, id_participante)` | PK compuesta | Rol y orden |

Referencias lógicas a usuarios, ventas, entradas y preferencias sin FK física. Esquema/seed: `infrastructure/mysql/eventos/init.sql`.

## 5. Eventos y mensajes

| Dirección | Routing key | Tipo | Idempotencia | Efecto |
| --- | --- | --- | --- | --- |
| Publica | `evento.cancelado` | `EventoCancelado` | `messageId` | Aviso a compradores APROBADOS |
| Publica | `evento.suspendido` | `EventoSuspendido` | `messageId` | Aviso a compradores APROBADOS |
| Publica | `evento.modificado` | `EventoModificado` | `messageId` | Aviso a compradores APROBADOS |
| Publica | `evento.publicado` | `EventoPublicado` | `messageId` | Aviso a favoritos por artista + ciudad |

Exchange `eventos.exchange` (topic). Los consumidores y el contrato de payload están en [`specs/comunicacion.md`](../../specs/comunicacion.md). No consume mensajes.

## 6. Reglas y criterios de aceptación

| Regla / escenario | Resultado esperado | Test que lo cubre |
| --- | --- | --- |
| Publicar sin lugar real, fecha futura o sector activo | `400` | `EventoServiceImplTest` |
| Cupo total > 5000 al publicar | `esAltaDemanda=true` forzado | `EventoServiceImplTest` |
| Cancelar un evento con entradas | `CANCELADO` + reintegro best-effort | `EventoServiceImplTest` |
| Editar/eliminar un evento CANCELADO/FINALIZADO | `409/400` | `EventoServiceImplTest` |
| Participante duplicado o tipo inválido | `409/400` | `ParticipanteControllerTest`, `EventoServiceImplTest` |
| Escritura de lugar/sector por no-ADMIN | `403` en gateway y controller | `LugarControllerTest`, `AutorizacionPorRolMatrixTest` |
| Publicar abre cupos en ventas | Feign `POST /eventos/{id}/cupos` fail-hard | `EventoServiceImplTest` |
| Transición de estado publica el evento correcto | routing key/tipo esperado | `EventChangePublisherTest`, `EventMessagingSerializationTest` |

## 7. Operación

```bash
cd Backend
docker compose up -d --build ms-eventos

cd msvc-eventos
mvn test              # 203 unitarios
mvn -Pit verify       # +1 IT de contexto con MySQL real (Testcontainers)
```

- **Variables:** `SPRING_DATASOURCE_URL` (default `jdbc:mysql://localhost:3308/eventos`), `SPRING_RABBITMQ_*`, `TICKETYA_URI_VENTAS`, `TICKETYA_URI_ENTRADAS`, `TICKETYA_INTERNAL_TOKEN`, `TICKETYA_GEOCODING_*`.
- **Smoke:** `GET /` → `microservicio eventos ok!`; OpenAPI en `/v3/api-docs`.
- **Seeds:** eventos y lugares en `infrastructure/mysql/eventos/init.sql` (incluye eventos demo y portadas).

## 8. Deuda y decisiones

| Tema | Estado | Nota |
| --- | --- | --- |
| Reintegro al cancelar | `parcial` | Feign a entradas best-effort; si entradas cae, el evento queda CANCELADO igual |
| `destacado` | `pendiente` | Sin columna en el esquema: siempre `false` |
| Alta demanda | `hecho` | Umbral 5000 y sincronización de fila con ventas |
| Aplicación de DOC-MS-00 | `hecho` | Revisión final 2026-11-21: encabezado, alcance, API, modelo, eventos, criterios y operación verificados contra el código. |
