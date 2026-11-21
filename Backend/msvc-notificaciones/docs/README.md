# Contexto — msvc-notificaciones

| Campo | Valor |
| --- | --- |
| Microservicio | `msvc-notificaciones` |
| Bounded context | Plantillas, creación de notificaciones desde eventos, entrega e inbox |
| Puerto local | `8086` |
| Base de datos | MySQL `notificaciones` (host `3311`) |
| Mensajería | RabbitMQ: consume `evento.#`, `compra.confirmada` y `devolucion.aprobada` |
| Consumidores | `APIGateway`, frontend (campanita/bandeja), `APIGateway` para admin |
| Actualizado | `2026-11-21` |

**Fuentes canónicas**

- API: [`endpoints.md`](endpoints.md) y [`docs/openapi/msvc-notificaciones.json`](../../docs/openapi/msvc-notificaciones.json)
- Modelo: [`docs/modelo-datos.md`](../../docs/modelo-datos.md) §5
- Mensajes: [`specs/comunicacion.md`](../../specs/comunicacion.md) §colas de notificaciones
- Reglas: [`specs/reglas-de-negocio.md`](../../specs/reglas-de-negocio.md) (preferencias/avisos)
- Entrega: [`docs/testing/notificaciones.md`](../../docs/testing/notificaciones.md)
- Arquitectura: [`docs/arquitectura.md`](../../docs/arquitectura.md)

## 1. Alcance y límites

- **Es responsable de:** plantillas por tipo/canal, creación de avisos a partir de eventos de dominio, respeto de preferencias de canal, agrupación PUSH/EMAIL por mensaje, job de envío con reintentos, registro de intentos y bandeja propia con contador de no leídas.
- **No es responsable de:** definir eventos de negocio (eventos/ventas/entradas), datos de usuarios (msvc-usuarios), preferencias (msvc-preferencias) ni entrega push real (hoy mock, sin Firebase).
- **Consumidores:** gateway para `/api/notificaciones/**`; frontend para la campanita y la bandeja; ADMIN para alta manual y estado de envío.
- **Dependencias síncronas:** `msvc-usuarios` (email del destinatario), `msvc-ventas` (compradores APROBADOS) y `msvc-preferencias` (matching y canales), todas fail-soft para no bloquear la creación.
- **Dependencias asíncronas:** consume 6 routing keys; no publica eventos.

## 2. Agregados e invariantes

| Agregado / entidad raíz | Entidades relacionadas | Invariantes principales |
| --- | --- | --- |
| `Notificacion` | `IntentoEnvio` | Estado `PENDIENTE` → `ENVIADA`/`FALLIDA`; máximo 3 intentos; `clave_idempotencia` única por usuario; `clave_mensaje` agrupa PUSH/EMAIL; `leida`/`fecha_lectura` por mensaje |
| `PlantillaNotificacion` | — | Plantilla activa por tipo/canal; si no hay plantilla para un canal, ese canal no se genera |
| Inbox | `Notificacion` | Una entrada por mensaje; `canales` informa PUSH/EMAIL; `noLeidas` cuenta mensajes, no filas |

Idempotencia: `tipo:canal:eventoId:messageId` para eventos, `CompraConfirmada:EMAIL:compraId` para compras y `DevolucionAprobada:<canal>:<devolucionId>` para devoluciones. Las preferencias de canal se evalúan al crear el aviso.

## 3. API pública

| Método | Path | Rol / auth | Uso |
| --- | --- | --- | --- |
| `GET` | `/notificaciones/me?page=&size=&leida=` | JWT (`X-User-Id`) | Bandeja paginada + `noLeidas` |
| `GET` | `/notificaciones/me/{id}` | JWT (dueño) | Detalle |
| `PATCH` | `/notificaciones/me/{id}/leida` | JWT (dueño) | Marcar el mensaje completo como leído |
| `POST` | `/notificaciones` | ADMIN | Alta manual, queda `PENDIENTE` |
| `GET` | `/notificaciones/{id}/estado` | ADMIN | Estado de envío |

Detalle completo, errores `ApiError` y estados en [`endpoints.md`](endpoints.md). Swagger: `http://localhost:8086/swagger-ui/index.html`.

## 4. Modelo local

| Tabla | Clave | Índices / restricciones | Notas |
| --- | --- | --- | --- |
| `plantilla_notificacion` | `id_plantilla` | UK tipo/canal/activa | Asunto y contenido de la plantilla |
| `notificacion` | `id_notificacion` | UK `(id_usuario, clave_idempotencia)`; `clave_mensaje`, `leida` | Estado, canal, destinatario, fechas |
| `intento_envio` | `id_intento` | `id_notificacion`, número | Resultado y detalle del error |

Referencias lógicas a usuarios/eventos/compras/devoluciones, sin FK. Esquema/seed: `infrastructure/mysql/notificaciones/init.sql`.

## 5. Eventos y mensajes

| Dirección | Cola | Routing key / tipo | Idempotencia | Efecto |
| --- | --- | --- | --- | --- |
| Consume | `notificaciones.eventos` | `evento.cancelado` (`EventoCancelado`) | `tipo:canal:eventoId:messageId` | PUSH + EMAIL a compradores APROBADOS |
| Consume | `notificaciones.eventos` | `evento.suspendido` (`EventoSuspendido`) | ídem | PUSH + EMAIL a compradores APROBADOS |
| Consume | `notificaciones.eventos` | `evento.modificado` (`EventoModificado`) | ídem | PUSH + EMAIL a compradores APROBADOS |
| Consume | `notificaciones.eventos` | `evento.publicado` (`EventoPublicado`) | ídem | PUSH + EMAIL a favoritos por artista + ciudad |
| Consume | `notificaciones.compras` | `compra.confirmada` (`CompraConfirmada`) | `CompraConfirmada:EMAIL:compraId` | EMAIL al cliente |
| Consume | `notificaciones.devoluciones` | `devolucion.aprobada` (`DevolucionAprobada`) | `DevolucionAprobada:<canal>:<devolucionId>` | PUSH + EMAIL al dueño de la entrada |

DLQs: `notificaciones.eventos.dlq`, `notificaciones.compras.dlq`, `notificaciones.devoluciones.dlq`. Contrato completo en [`specs/comunicacion.md`](../../specs/comunicacion.md). No publica mensajes.

## 6. Reglas y criterios de aceptación

| Regla / escenario | Resultado esperado | Test que lo cubre |
| --- | --- | --- |
| Evento cancelado/suspendido/modificado/publicado | Una entrada por mensaje con `canales` PUSH/EMAIL | `NotificacionServiceImplTest` |
| Reentrega del mismo `messageId` | No duplica la notificación | `NotificacionServiceImplTest` |
| Compra confirmada | Aviso EMAIL al cliente | `NotificacionServiceImplTest` |
| Devolución aprobada | Aviso PUSH + EMAIL con monto | `NotificacionServiceImplTest`, `DevolucionAprobadaContratoTest` |
| Preferencia de canal desactivada | No se crea ese canal | `NotificacionServiceImplTest` |
| Bandeja | Una entrada por mensaje y `noLeidas` por mensaje | `NotificacionControllerTest` |
| Marcar leída | Actualiza todas las filas PUSH/EMAIL | `NotificacionControllerTest` |
| Envío | `PENDIENTE → ENVIADA/FALLIDA` con reintentos | `NotificacionEnvioServiceTest`, `SmtpEmailProviderTest` |

## 7. Operación

```bash
cd Backend
docker compose up -d --build ms-notificaciones

cd msvc-notificaciones
mvn test              # 42 unitarios
mvn -Pit verify       # +6 IT con MySQL + Rabbit reales (Testcontainers)
```

- **Variables:** `SPRING_DATASOURCE_URL` (default `jdbc:mysql://localhost:3311/notificaciones`), `SPRING_RABBITMQ_*`, `SPRING_MAIL_HOST/PORT`, `TICKETYA_NOTIF_EMAIL_PROVIDER` (`mock`/`smtp`), `TICKETYA_NOTIF_ENVIO_INTERVALO_MS` (30 s), `TICKETYA_NOTIF_ENVIO_MAX_INTENTOS` (3).
- **Smoke:** `GET /` y `GET /notificaciones` → `microservicio notificaciones ok!`; OpenAPI en `/v3/api-docs`.
- **MailHog:** `http://localhost:8025` en Docker; sin `smtp` usa `MockEmailProvider`.
- **Seeds:** plantillas en `infrastructure/mysql/notificaciones/init.sql`.

## 8. Deuda y decisiones

| Tema | Estado | Nota |
| --- | --- | --- |
| Push real | `parcial` | `MockPushProvider`; sin Firebase |
| Entrega email | `hecho` | SMTP/MailHog en Docker, mock en tests locales |
| Alta administrativa | `hecho` | Queda `PENDIENTE`; no se agrupa con mensajes de eventos |
| Aplicación de DOC-MS-00 | `parcial` | README completo; falta revisión final del equipo |
