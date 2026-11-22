# Contexto — msvc-entradas

| Campo | Valor |
| --- | --- |
| Microservicio | `msvc-entradas` |
| Bounded context | Emisión de entradas, QR, devoluciones y control de acceso |
| Puerto local | `8084` |
| Base de datos | MySQL `entradas` (host `3310`) |
| Mensajería | RabbitMQ: publica `devolucion.aprobada` en `eventos.exchange` |
| Consumidores | `APIGateway`, frontend, `msvc-ventas` por Feign (emisión y resumen) |
| Actualizado | `2026-11-21` |

**Fuentes canónicas**

- API: [`endpoints.md`](endpoints.md) y [`docs/openapi/msvc-entradas.json`](../../docs/openapi/msvc-entradas.json)
- Modelo: [`docs/modelo-datos.md`](../../docs/modelo-datos.md) §4
- Mensajes: [`specs/comunicacion.md`](../../specs/comunicacion.md) §`devolucion.aprobada`
- Reglas: [`specs/reglas-de-negocio.md`](../../specs/reglas-de-negocio.md) §6
- Arquitectura: [`docs/arquitectura.md`](../../docs/arquitectura.md)

## 1. Alcance y límites

- **Es responsable de:** emisión de entradas post-pago, generación/consulta de QR, devoluciones (solicitud/aprobación/rechazo), validación de acceso y reintegro por cancelación de evento.
- **No es responsable de:** compras/pagos (msvc-ventas), configuración de eventos (msvc-eventos), notificaciones (msvc-notificaciones) ni identidad (gateway/usuarios).
- **Consumidores:** gateway para `/api/entradas/**` y `/api/devoluciones/**`; frontend para “Mis entradas”, devoluciones y escaneo; ventas por Feign para emisión y resumen de reportes.
- **Dependencias síncronas:** `msvc-eventos` (datos de evento para DTO/QR y autorización del organizador) y `msvc-ventas` (liberación de cupo al aprobar una devolución, fail-hard).
- **Dependencias asíncronas:** publica `devolucion.aprobada`; no consume colas.

## 2. Agregados e invariantes

| Agregado / entidad raíz | Entidades relacionadas | Invariantes principales |
| --- | --- | --- |
| `Entrada` | `ValidacionAcceso` | `codigo_qr` único; estados `VENDIDA` → `USADA`/`DEVUELTA`/`A_REINTEGRAR`; el QR solo se expone si está `VENDIDA` y el evento no está `CANCELADO`; una entrada por asiento |
| `Devolucion` | `Entrada` | `PENDIENTE` → `APROBADA`/`RECHAZADA`; `EN_CURSO` para reintegro por cancelación; solo `VENDIDA` no usada y **hasta 72 h antes del evento** (fechaInicio − 72 h; 503 si no se puede validar) |
| `ValidacionAcceso` | `Entrada` | Registro de todo intento; validación atómica por QR (`SELECT … FOR UPDATE`); resultados `VALIDA`, `YA_USADA`, `OTRO_EVENTO`, `NO_VALIDA`, `EVENTO_CANCELADO` |

Aprobar una devolución: entrada → `DEVUELTA`, QR invalidado (`INV-*`), liberación de cupo en ventas **fail-hard** y publicación de `devolucion.aprobada` después del commit. Si falla la liberación, hay rollback y no se publica.

La emisión es **idempotente por compra** con el claim `emision_compra`: si ventas reintenta (timeout de Feign) o el job y el post-pago se cruzan, solo una transacción crea las entradas; las demás reciben `503` y reintentan. Un rollback libera el claim.

## 3. API pública

| Método | Path | Rol / auth | Uso |
| --- | --- | --- | --- |
| `POST` | `/entradas/emision` | Interno (Feign ventas; gateway solo ADMIN) | Emitir entradas post-pago; idempotente por compra con claim (`503` si otra emisión está en curso) |
| `GET` | `/entradas`, `/entradas/{id}`, `/entradas/{id}/qr` | JWT (dueño; ADMIN en detalle) | Consulta y QR |
| `POST/GET` | `/entradas/{id}/devoluciones`, `/devoluciones`, `/devoluciones/{id}` | JWT / ADMIN | Solicitud y consulta |
| `POST` | `/devoluciones/{id}/aprobacion`, `/devoluciones/{id}/rechazo` | ADMIN | Resolver devolución |
| `POST` | `/eventos/{id}/reintegro-cancelacion` | Interno | Marcar entradas `A_REINTEGRAR` |
| `POST/GET` | `/eventos/{id}/validaciones` | `PERSONAL_ACCESO`/`ADMIN`; GET también organizador dueño | Control de acceso |
| `GET` | `/eventos/{id}/entradas/resumen` | Interno (Feign ventas) | Resumen para reportes |

Detalle completo en [`endpoints.md`](endpoints.md). Swagger: `http://localhost:8084/swagger-ui/index.html`.

## 4. Modelo local

| Tabla | Clave | Índices / restricciones | Notas |
| --- | --- | --- | --- |
| `entrada` | `id_entrada` | UK `codigo_qr`; `id_compra`, `id_cliente`, `id_evento`, `id_sector` lógicos | Estado, precio, fila/número, fecha de validación |
| `emision_compra` | `id_compra` | PK `id_compra` | Claim idempotente de emisión: una fila por compra, en la misma transacción que las entradas |
| `devolucion` | `id_devolucion` | `id_entrada` lógico; estado | Motivo, monto, fechas |
| `validacion_acceso` | `id_validacion` | `id_entrada`, `id_evento` lógicos | Resultado, dispositivo, fecha |

Referencias lógicas a ventas, eventos y usuarios, sin FK física. Esquema/seed: `infrastructure/mysql/entradas/init.sql`.

## 5. Eventos y mensajes

| Dirección | Routing key | Tipo | Idempotencia | Efecto |
| --- | --- | --- | --- | --- |
| Publica | `devolucion.aprobada` | `DevolucionAprobada` | `devolucionId` | Notificación PUSH + EMAIL al cliente dueño de la entrada |

Se publica después del commit (`@TransactionalEventListener(AFTER_COMMIT)`). Consumidor: `notificaciones.devoluciones`. Contrato completo en [`specs/comunicacion.md`](../../specs/comunicacion.md). No consume mensajes.

## 6. Reglas y criterios de aceptación

| Regla / escenario | Resultado esperado | Test que lo cubre |
| --- | --- | --- |
| Emisión de una entrada por asiento | Cantidad coincide y devuelve `fila`/`asiento` | `EntradaServiceImplTest` |
| QR de entrada usada/devuelta o evento cancelado | `409`, sin exponer código | `EntradaControllerValidacionTest`, `EntradaServiceImplTest` |
| Devolución fuera de plazo o entrada usada | `400/409` | `EntradaServiceImplTest` |
| Aprobar devolución | `DEVUELTA` + QR invalidado + cupo liberado + evento publicado | `EntradaServiceImplTest`, `DevolucionAprobadaPublisherIT` |
| Emisión concurrente/reintentada de la misma compra | Exactamente las entradas del detalle; el resto `503` y reintenta | `EmisionConcurrenciaIT` |
| Escaneo simultáneo del mismo QR | Solo un `VALIDA`; el resto `YA_USADA` | `ValidacionIngresoConcurrenciaIT` |
| Escaneo de otro evento / QR inexistente | `OTRO_EVENTO` / `404` | `EntradaControllerValidacionTest` |
| Cancelación de evento | Entradas `A_REINTEGRAR` y devolución `EN_CURSO` | `EntradaServiceImplTest` |

## 7. Operación

```bash
cd Backend
docker compose up -d --build ms-entradas

cd msvc-entradas
mvn test              # 86 unitarios
mvn -Pit verify       # +5 IT con MySQL + Rabbit reales (Testcontainers)
```

- **Variables:** `SPRING_DATASOURCE_URL` (default `jdbc:mysql://localhost:3310/entradas`), `SPRING_RABBITMQ_*`, `TICKETYA_URI_EVENTOS`, `TICKETYA_URI_VENTAS`, `TICKETYA_DEVOLUCION_HORAS` (72).
- **Smoke:** `GET /` → `microservicio entradas ok!`; OpenAPI en `/v3/api-docs`.
- **Seeds:** entradas y devolución pendiente demo en `infrastructure/mysql/entradas/init.sql`.

## 8. Deuda y decisiones

| Tema | Estado | Nota |
| --- | --- | --- |
| Asignación de eventos por PERSONAL_ACCESO | `descartado` | X-ACC-01: cualquier personal valida cualquier evento |
| Reintegro por cancelación | `parcial` | No devuelve cupo a ventas; solo marca entradas y crea devolución `EN_CURSO` |
| Pasarela de reintegro real | `pendiente` | El reintegro es mock (solo estado) |
| Aplicación de DOC-MS-00 | `hecho` | Revisión final 2026-11-21: encabezado, alcance, API, modelo, eventos, criterios y operación verificados contra el código. |
