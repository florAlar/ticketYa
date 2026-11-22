# Contexto — msvc-ventas

| Campo | Valor |
| --- | --- |
| Microservicio | `msvc-ventas` |
| Bounded context | Disponibilidad, reservas, compras, pagos, reportes y fila virtual |
| Puerto local | `8083` |
| Base de datos | MySQL `ventas` (host `3309`) + Redis `6380` |
| Mensajería | RabbitMQ: publica `compra.confirmada` en `eventos.exchange` |
| Consumidores | `APIGateway`, frontend, `msvc-eventos`/`msvc-entradas`/`msvc-preferencias` por Feign |
| Actualizado | `2026-11-21` |

**Fuentes canónicas**

- API: [`endpoints.md`](endpoints.md) y [`docs/openapi/msvc-ventas.json`](../../docs/openapi/msvc-ventas.json)
- Modelo: [`docs/modelo-datos.md`](../../docs/modelo-datos.md) §3
- Mensajes: [`specs/comunicacion.md`](../../specs/comunicacion.md) §`compra.confirmada`
- Reglas: [`specs/reglas-de-negocio.md`](../../specs/reglas-de-negocio.md) §5
- Fila virtual: [`specs/fila-virtual.md`](../../specs/fila-virtual.md)
- Arquitectura: [`docs/arquitectura.md`](../../docs/arquitectura.md)

## 1. Alcance y límites

- **Es responsable de:** disponibilidad comercial, cupos por evento/sector, reservas temporales, compras, pagos mock, asientos virtuales, idempotencia, reportes del organizador y estado de la fila virtual.
- **No es responsable de:** configuración/catálogo de eventos (msvc-eventos), emisión/QR/devoluciones (msvc-entradas), notificaciones (msvc-notificaciones) ni identidad (gateway/usuarios).
- **Consumidores:** gateway para `/api/reservas/**`, `/api/compras/**`, `/api/pagos/**`, `/api/reportes/**`, disponibilidad y `/fila/**`; frontend para compra, reportes y fila; eventos/entradas por Feign.
- **Dependencias síncronas:** `msvc-eventos` (precio/disponibilidad/flag de alta demanda), `msvc-entradas` (emisión post-pago y resumen para reportes).
- **Dependencias asíncronas:** publica `compra.confirmada`; no consume colas.

## 2. Agregados e invariantes

| Agregado / entidad raíz | Entidades relacionadas | Invariantes principales |
| --- | --- | --- |
| `CupoSector` | `AsientoReservado` | Cupo por evento/sector; lock pesimista para evitar sobreventa; UK evento/sector; asientos virtuales solo se persisten tomados |
| `Reserva` | `DetalleReserva` | `PENDIENTE` → `CONFIRMADA`; TTL 10 min; cancelación/expiración liberan cupo y asientos |
| `Compra` | `Pago` | `PENDIENTE_PAGO` → `APROBADA`/`RECHAZADA`; `Idempotency-Key` obligatoria; máximo 6 entradas por cliente/evento |
| Fila virtual (Redis) | — | `EN_ESPERA`/`HABILITADO`/`CON_TOKEN`/`TURNO_USADO`; token de 10 min consumido antes del lock y restaurado si hay rollback |

Reglas: el pago aprobado emite entradas vía Feign y publica `compra.confirmada` después del commit; el rechazo/vencimiento libera cupo; en eventos de alta demanda `POST /reservas` exige `tokenFila`; los reportes no se entregan parciales (503 si falla una dependencia).

## 3. API pública

| Método | Path | Rol / auth | Uso |
| --- | --- | --- | --- |
| `GET` | `/eventos/{id}/disponibilidad`, `/eventos/disponibilidad?ids=` | Público por el gateway | Cupos y asientos |
| `POST` | `/eventos/{id}/cupos`, `/cupos/liberacion` | Interno/ADMIN | Apertura/liberación de stock |
| `POST` | `/reservas` | JWT | Crear reserva (opcional `tokenFila`) |
| `GET/POST` | `/reservas/{id}`, `.../confirmacion`, `.../cancelacion`, `.../renovacion` | Dueño | Ciclo de reserva |
| `POST/GET` | `/compras`, `/compras/{id}`, `/compras/{id}/pagos`, `/pagos/{id}/reintentos` | JWT | Compra y pago mock |
| `GET` | `/reportes/eventos/{id}/...` | ORGANIZADOR dueño / ADMIN | Reportes |
| `POST/GET` | `/eventos/{id}/fila/ingreso`, `/posicion`, `/validacion`, `/estado`, `/habilitacion` | JWT; estado público; habilitación ADMIN | Fila virtual |

El detalle de cuerpos, estados y errores está en [`endpoints.md`](endpoints.md). Swagger: `http://localhost:8083/swagger-ui/index.html`.

## 4. Modelo local

| Tabla | Clave | Índices / restricciones | Notas |
| --- | --- | --- | --- |
| `cupo_sector` | `id` | UK `(id_evento, id_sector)` | Cupo, disponible y vendido |
| `reserva` | `id_reserva` | `id_cliente`, TTL | PENDIENTE/CONFIRMADA |
| `detalle_reserva` | `id` | FK lógica a reserva/sector | Cantidad/precio/asientos |
| `compra` | `id_compra` | `id_reserva`, estado | PENDIENTE_PAGO/APROBADA/RECHAZADA; `emision_estado` PENDIENTE/EMITIDA/FALLIDA |
| `pago` | `id_pago` | `id_compra`, estado | Mock aprobado/rechazado |
| `asiento_reservado` | `id` | UK evento/sector/fila/número | RESERVADO/VENDIDO |

Redis: holds de reserva (TTL 10 min), claves de idempotencia (24 h) y claves `fila:*` (TTL 24 h). Esquema/seed: `infrastructure/mysql/ventas/init.sql`.

## 5. Eventos y mensajes

| Dirección | Routing key | Tipo | Idempotencia | Efecto |
| --- | --- | --- | --- | --- |
| Publica | `compra.confirmada` | `CompraConfirmada` | `compraId` | Notificación EMAIL y registro de historial en preferencias |

Se publica después del commit (`@TransactionalEventListener(AFTER_COMMIT)`). Contrato completo en [`specs/comunicacion.md`](../../specs/comunicacion.md). No consume mensajes.

## 6. Reglas y criterios de aceptación

| Regla / escenario | Resultado esperado | Test que lo cubre |
| --- | --- | --- |
| Reserva sin cupo o con asiento tomado | `409` y sin sobreventa | `VentaServiceImplTest`, `CupoSectorConcurrenciaIT`, `AsientoReservadoConcurrenciaIT` |
| Reserva vencida o cancelada | Libera cupo/asientos | `VentaServiceImplTest` |
| Compra sin `Idempotency-Key` o repetida | `400`/misma compra | `VentaServiceImplTest` |
| Pago aprobado | Compra APROBADA + emisión + `compra.confirmada` | `VentaServiceImplTest`, `CompraEventPublisherTest` |
| Pago rechazado | Libera cupo y permite reintento | `VentaServiceImplTest` |
| Fila de alta demanda sin token | `409 FILA_TOKEN_REQUERIDO` y no descuenta cupo | `FilaServiceTest`, `FilaControllerTest`, `FilaRedisStoreIT` |
| Reporte con dependencia caída | `503` sin reporte parcial | `ReporteControllerTest` |

## 7. Operación

```bash
cd Backend
docker compose up -d --build ms-ventas

cd msvc-ventas
mvn test              # 221 unitarios
mvn -Pit verify       # +22 IT (MySQL, Rabbit; requiere Docker)
```

- **Variables:** `SPRING_DATASOURCE_URL` (default `jdbc:mysql://localhost:3309/ventas`), `SPRING_DATA_REDIS_HOST/PORT`, `SPRING_RABBITMQ_*`, `TICKETYA_URI_EVENTOS`, `TICKETYA_URI_ENTRADAS`, `TICKETYA_FILA_*`.
- **Smoke:** `GET /` → `microservicio ventas ok!`; OpenAPI en `/v3/api-docs`.
- **Seeds:** cupos y compras demo en `infrastructure/mysql/ventas/init.sql`.

## 8. Deuda y decisiones

| Tema | Estado | Nota |
| --- | --- | --- |
| Fila virtual | `hecho` | Estado en Redis; sin tablas MySQL nuevas |
| Corrida formal de carga | `hecho` | E2-TST-03: matriz 25/50/100 normal y con fila; ver `docs/testing/carga.md` |
| Consistencia distribuida | `parcial` | Sin 2PC: Feign fail-hard para cupos y best-effort para reintegros |
| Aplicación de DOC-MS-00 | `hecho` | Revisión final 2026-11-21: encabezado, alcance, API, modelo, eventos, criterios y operación verificados contra el código. |
