# Contexto — msvc-preferencias

| Campo | Valor |
| --- | --- |
| Microservicio | `msvc-preferencias` |
| Bounded context | Preferencias del cliente, historial de compras y recomendaciones |
| Puerto local | `8085` |
| Base de datos | MongoDB `preferencias` (host `27018`) |
| Mensajería | RabbitMQ: consume `compra.confirmada` en `preferencias.compras` |
| Consumidores | `APIGateway`, frontend, `msvc-notificaciones` por Feign (matching/preferencias) |
| Actualizado | `2026-11-21` |

**Fuentes canónicas**

- API: [`endpoints.md`](endpoints.md) y [`docs/openapi/msvc-preferencias.json`](../../docs/openapi/msvc-preferencias.json)
- Recomendaciones: [`recomendaciones.md`](recomendaciones.md)
- Modelo: [`docs/modelo-datos.md`](../../docs/modelo-datos.md) §8 y [`specs/diagramas/modelo-mongodb.md`](../../specs/diagramas/modelo-mongodb.md)
- Mensajes: [`specs/comunicacion.md`](../../specs/comunicacion.md) §`preferencias.compras`
- Reglas: [`specs/reglas-de-negocio.md`](../../specs/reglas-de-negocio.md) §7
- Arquitectura: [`docs/arquitectura.md`](../../docs/arquitectura.md)

## 1. Alcance y límites

- **Es responsable de:** preferencias explícitas del cliente (ciudades, artistas, rango de precio, canales/tipos de notificación), historial de compras denormalizado, backfill y recomendaciones propias/similares.
- **No es responsable de:** datos maestros de eventos (msvc-eventos), compras/pagos (msvc-ventas), envío de notificaciones (msvc-notificaciones) ni identidad (gateway/usuarios).
- **Consumidores:** gateway para `/api/preferencias/**`, `/api/recomendaciones/**` y `/api/clientes/**`; frontend (home, detalle, perfil); notificaciones por Feign para matching y canales.
- **Dependencias síncronas:** `msvc-eventos` (snapshot de evento para historial y recomendaciones) y `msvc-ventas` (backfill de compras aprobadas).
- **Dependencias asíncronas:** consume `compra.confirmada`; no publica eventos.

## 2. Agregados e invariantes

| Agregado / documento raíz | Documentos relacionados | Invariantes principales |
| --- | --- | --- |
| `PreferenciasCliente` | `notificaciones` embebido | Un documento por cliente; defaults `push=true`, `email=true`, `tipos=[]` (todos); `tipos=[]` significa “todos” |
| `InteraccionEvento` | — | Snapshot por compra; `idCompra` único (upsert idempotente); un documento por compra confirmada |
| Recomendaciones | `PreferenciasCliente`, `InteraccionEvento` | Híbrido preferencias + co-compra + fallback; excluye eventos comprados y no publicados; pesos configurables |

Las preferencias se evalúan **al crear** la notificación: desactivar un canal después no cancela un envío ya `PENDIENTE`.

## 3. API pública

| Método | Path | Rol / auth | Uso |
| --- | --- | --- | --- |
| `GET/PUT` | `/preferencias/me` | JWT (`X-User-Id`) | Preferencias propias y defaults |
| `PUT` | `/preferencias/me/artistas` | JWT | Actualizar solo artistas |
| `GET/PUT` | `/preferencias/me/notificaciones` | JWT | Canales y tipos |
| `GET` | `/recomendaciones/me?limit=10` | JWT | Recomendaciones propias |
| `GET` | `/eventos/{id}/recomendaciones?limit=6` | Público | Eventos similares |
| `GET` | `/clientes/{id}/historial?page=&size=` | Dueño / ADMIN | Historial paginado |
| `POST` | `/preferencias/clientes/matching` | Interno (`X-Internal-Token`) | Artistas + ciudad → ids de clientes |
| `GET` | `/preferencias/clientes/{id}/notificaciones` | Interno | Canales con defaults |
| `POST` | `/internas/backfill` | Interno | Importar compras aprobadas |

Detalle completo en [`endpoints.md`](endpoints.md). Swagger: `http://localhost:8085/swagger-ui/index.html`.

## 4. Modelo local

| Colección | Clave | Índices / restricciones | Notas |
| --- | --- | --- | --- |
| `preferencias_clientes` | `idCliente` | Índice por `idCliente` | Un documento por cliente; listas de artistas/ciudades y canales |
| `interacciones_eventos` | `_id` | `uk_interacciones_idCompra` (unique, sparse); índices por cliente/evento/tipo | Snapshot de compra (`idCompra`, evento, artistas, fecha, monto) |

Referencias lógicas a usuarios, eventos y ventas, sin FK. Seed/índices: `infrastructure/mongodb/preferencias/init.js`. Backfill: `scripts/backfill-preferencias.sh`.

## 5. Eventos y mensajes

| Dirección | Cola | Routing key | Tipo | Idempotencia | Efecto |
| --- | --- | --- | --- | --- | --- |
| Consume | `preferencias.compras` | `compra.confirmada` | `CompraConfirmada` | upsert por `idCompra` | Agrega el evento al historial del cliente |

DLQ: `preferencias.compras.dlq` (DLX `preferencias.dlx`). Semántica at-least-once. No publica mensajes. Contrato en [`specs/comunicacion.md`](../../specs/comunicacion.md).

## 6. Reglas y criterios de aceptación

| Regla / escenario | Resultado esperado | Test que lo cubre |
| --- | --- | --- |
| Cliente sin documento de preferencias | `GET` devuelve defaults | `PreferenciaServiceImplTest` |
| PUT parcial de canales | Conserva el campo no enviado | `PreferenciaServiceImplTest` |
| Matching por artista/ciudad | Devuelve ids compatibles (case-insensitive) | `PreferenciaServiceImplTest` |
| Reentrega de `compra.confirmada` | No duplica historial (`idCompra`) | `CompraConfirmadaIngestIT` |
| Recomendación sin datos | Fallback a próximos publicados | `RecomendacionServiceImplTest` |
| Recomendación de evento inexistente | `404` | `RecomendacionControllerTest` |
| Índices y repos contra Mongo real | Consultas y scoring correctos | `RecomendacionMongoIT` |

## 7. Operación

```bash
cd Backend
docker compose up -d --build ms-preferencias

cd msvc-preferencias
mvn test              # 39 unitarios
mvn -Pit verify       # +2 IT (Mongo real + Rabbit/Mongo; requiere Docker)
```

- **Variables:** `SPRING_MONGODB_URI` / `SPRING_DATA_MONGODB_URI` (default `mongodb://ticketya:mongo_dev@localhost:27018/preferencias?authSource=admin`), `SPRING_RABBITMQ_*`, `TICKETYA_URI_EVENTOS`, `TICKETYA_URI_VENTAS`, `TICKETYA_RECS_*`.
- **Smoke:** `GET /` → `microservicio preferencias ok!`; OpenAPI en `/v3/api-docs`.
- **Seed:** `init.js` crea colecciones, índices y matching demo (Metallica/Iron Maiden/Buenos Aires).

## 8. Deuda y decisiones

| Tema | Estado | Nota |
| --- | --- | --- |
| Co-compra precomputada | `pendiente` | Se calcula on-the-fly con 2 consultas; suficiente para el alcance |
| Interacciones por vista | `pendiente` | Solo se registra compra; la vista queda como mejora |
| Consistencia eventual | `hecho` | El historial aparece cuando Rabbit entrega el mensaje |
| Aplicación de DOC-MS-00 | `parcial` | README completo; falta revisión final del equipo |
