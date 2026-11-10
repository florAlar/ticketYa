# Features y User Stories — Entrega 1

Fuente: `specs/consigna.md` (§5.A), `specs/reglas-de-negocio.md` y comportamiento implementado en los microservicios.
Criterios de aceptación en formato *Dado / Cuando / Entonces*. Las rutas son las públicas del gateway (prefijo `/api`).

Roles: **CLIENTE**, **ORGANIZADOR**, **ADMIN**, **PERSONAL_ACCESO** (este último, solo en Entrega 2).

## Índice de features

| ID | Feature | Servicio principal |
|---|---|---|
| F1 | Cuentas y roles | msvc-usuarios |
| F2 | Lugares, sectores y participantes | msvc-eventos |
| F3 | Gestión y publicación de eventos | msvc-eventos (+ ventas) |
| F4 | Catálogo y búsqueda | msvc-eventos (+ ventas) |
| F5 | Reserva temporal de entradas | msvc-ventas |
| F6 | Compra y pago simulado | msvc-ventas |
| F7 | Entradas con QR | msvc-entradas |
| F8 | Devoluciones | msvc-entradas (+ ventas) |
| F9 | Cancelación de evento y reintegro | msvc-eventos → msvc-entradas |
| F10 | No doble venta (requisito transversal) | msvc-ventas |

---

## F1 — Cuentas y roles

**US-01 · Registro** — Como visitante quiero registrarme para poder comprar entradas.
- Dado que el email no existe, cuando envío `POST /auth/register` con datos válidos, entonces se crea una cuenta con rol CLIENTE.
- Dado que el email ya existe, cuando intento registrarme, entonces recibo `409`.
- El registro público nunca otorga ORGANIZADOR, ADMIN ni PERSONAL_ACCESO.

**US-02 · Inicio de sesión** — Como usuario quiero iniciar sesión para operar con mi cuenta.
- Dado un usuario activo, cuando envío `POST /auth/login` con credenciales correctas, entonces recibo un token y mi perfil.
- Dadas credenciales incorrectas, entonces recibo `401`.

**US-03 · Perfil propio** — Como usuario quiero ver y editar mi perfil (nombre, apellido, ciudad).
- Cuando consulto `GET /usuarios/me`, entonces veo solo mis datos.
- Cuando edito con `PATCH /usuarios/me`, entonces los cambios se persisten.

**US-04 · Administración de usuarios** — Como ADMIN quiero listar usuarios, activar/desactivar cuentas y asignar roles.
- Dado un ADMIN, cuando uso `GET /usuarios`, `PATCH /usuarios/{id}/estado` o `PUT /usuarios/{id}/roles`, entonces la operación se realiza.
- Dado un usuario no ADMIN, cuando intenta lo mismo, entonces recibe `403`.

## F2 — Lugares, sectores y participantes

**US-05 · Alta de lugares y sectores** — Como ORGANIZADOR quiero cargar lugares con sectores (numerados o no) y su capacidad.
- Cuando creo un lugar con `POST /lugares` y sectores con `POST /lugares/{id}/sectores`, entonces quedan disponibles para asignarse a eventos.
- Un sector **numerado** se define con `filas` y `asientosPorFila`; su capacidad es el producto. `GET /lugares/{id}/sectores/{sectorId}/asientos` devuelve la grilla.
- Dado un CLIENTE, cuando intenta crear un lugar, entonces recibe `403`.

**US-06 · Participantes** — Como ORGANIZADOR quiero registrar artistas, bandas o equipos y asociarlos a un evento (principal, telonero).
- Cuando asocio participantes con `PUT /eventos/{id}/participantes`, entonces el evento los muestra con su rol y orden.
- Solo el dueño del evento o un ADMIN puede asociar.

## F3 — Gestión y publicación de eventos

**US-07 · Borrador** — Como ORGANIZADOR quiero crear un evento en borrador con pocos datos (alcanza el nombre) y completarlo después.
- Cuando creo `POST /eventos`, entonces el estado es `BORRADOR`.
- Solo un borrador puede borrarse (`DELETE /eventos/{id}`).

**US-08 · Precios por sector** — Como ORGANIZADOR quiero definir el precio de cada sector para mi evento.
- Cuando uso `PUT /eventos/{id}/sectores/{sectorId}/precio`, entonces el precio queda asociado al par evento–sector.

**US-09 · Publicación** — Como ORGANIZADOR quiero publicar mi evento para abrir la venta.
- Dado un borrador con nombre, fecha de inicio futura, lugar real y al menos un sector con precio y capacidad ≥ 1, cuando uso `POST /eventos/{id}/publicacion`, entonces pasa a `PUBLICADO` y se abren los cupos de venta por sector.
- Dado un borrador incompleto, entonces recibo un error de validación y el evento no cambia de estado.
- Si no se pueden abrir los cupos en ventas, la publicación no queda a medias.

**US-10 · Suspensión y cambio de fecha** — Como ORGANIZADOR quiero suspender mis eventos y reprogramarlos.
- El ORGANIZADOR puede suspender solo sus eventos; el ADMIN cualquiera (`PATCH /eventos/{id}/estado`).
- El dueño o el ADMIN puede cambiar la fecha (`PATCH /eventos/{id}/fecha`).

## F4 — Catálogo y búsqueda

**US-11 · Búsqueda de eventos** — Como CLIENTE (o visitante) quiero buscar eventos por tipo, ciudad, fechas, organizador y participante, con orden y paginación.
- Cuando consulto `GET /eventos` con filtros, entonces recibo una página de resultados que cumple todos los filtros.
- Por defecto solo se listan eventos publicados; el ADMIN puede pedir todos los estados.
- La consulta es pública (no requiere sesión).

**US-12 · Disponibilidad** — Como CLIENTE quiero ver cuántos lugares quedan para decidir.
- Cuando consulto `GET /eventos/{id}/disponibilidad`, entonces veo el total y el nivel por sector: `AGOTADO` (0), `ULTIMOS_LUGARES` (<20), `MENOS_DE_50`, `MENOS_DE_100` u `OK`.
- Dado un sector agotado, entonces el checkout no permite comprar.

## F5 — Reserva temporal

**US-13 · Reservar** — Como CLIENTE quiero reservar entradas de un sector por un tiempo limitado para poder pagar sin perderlas.
- Dado cupo suficiente, cuando uso `POST /reservas` con `idEvento` e `items[{idSector, cantidad}]`, entonces se crea una reserva `PENDIENTE` con vencimiento a los **10 minutos** y el cupo pasa de disponible a reservado.
- Dado cupo insuficiente, entonces recibo `409` y no cambia el cupo.
- Si repito la misma petición con el mismo `Idempotency-Key` (opcional en reservas), entonces obtengo la misma reserva (sin reservar dos veces).
- Dado que con esta reserva superaría las **6 entradas por cliente y evento**, entonces recibo `409` ("Límite de entradas por cliente/evento"); el cálculo suma entradas ya compradas, compras pendientes y reservas activas.

**US-13b · Reservar asientos concretos (sector numerado)** — Como CLIENTE quiero elegir mis asientos en un sector numerado.
- Dado un sector numerado, cuando reservo con `items[{idSector, asientos:[{fila, numero}]}]`, entonces esos asientos quedan `RESERVADO` para mí y nadie más puede tomarlos.
- Dado un asiento ya `RESERVADO` o `VENDIDO`, entonces recibo `409` y no se reserva ninguno de los pedidos.
- Dado un asiento que no existe en el layout, repetido en el pedido, o asientos en un sector no numerado, entonces recibo `400`.
- `GET /eventos/{id}/disponibilidad` informa los asientos ocupados para poder mostrarlos como no disponibles.
- Al pagar, los asientos pasan a `VENDIDO`; si el pago se rechaza, la reserva vence o se cancela, vuelven a estar libres.

**US-14 · Confirmar, renovar y cancelar** — Como CLIENTE quiero gestionar mi reserva.
- `POST /reservas/{id}/confirmacion`: `PENDIENTE` → `CONFIRMADA`.
- `POST /reservas/{id}/renovacion`: extiende el vencimiento.
- `POST /reservas/{id}/cancelacion`: libera el cupo reservado.
- Solo el dueño de la reserva puede operar sobre ella.

**US-15 · Vencimiento** — Como sistema quiero liberar las reservas que vencen para no bloquear stock.
- Dada una reserva activa cuyo vencimiento pasó, cuando corre el job de expiración (cada 30 s) o se consulta la reserva, entonces pasa a `EXPIRADA`, el cupo vuelve a disponible y toda compra pendiente asociada queda `RECHAZADA`.

## F6 — Compra y pago simulado

**US-16 · Comprar** — Como CLIENTE quiero convertir mi reserva en una compra.
- Cuando uso `POST /compras` con `{idReserva}` y header `Idempotency-Key`, entonces se crea una compra `PENDIENTE_PAGO`.
- Sin `Idempotency-Key`, recibo `400`; repetir con la misma clave devuelve la misma compra.
- Dada una reserva vencida o que ya tiene compra, entonces recibo `409`.

**US-17 · Pagar (pasarela mock)** — Como CLIENTE quiero pagar mi compra.
- Con `POST /compras/{id}/pagos` y `resultado=APROBAR`, entonces el pago es `APROBADO`, la compra `APROBADA`, la reserva `COMPRADA`, el cupo pasa de reservado a vendido y se emiten las entradas.
- Con `resultado=RECHAZAR`, entonces el pago es `RECHAZADO`, la compra `RECHAZADA`, la reserva `CANCELADA` y el cupo se libera.
- La respuesta de la compra incluye `emisionEstado` (`NO_REQUIERE`, `PENDIENTE`, `EMITIDA`, `FALLIDA`).

**US-18 · Emisión resiliente** — Como CLIENTE quiero recibir mis entradas aunque msvc-entradas falle un momento.
- Dado un pago aprobado, si la emisión falla, entonces la compra queda con `emisionEstado=PENDIENTE` y se reintenta automáticamente cada 30 s (máx. 20 intentos, luego `FALLIDA`).
- La emisión es idempotente por compra: un reintento no duplica entradas.
- En un sector numerado se emite **una entrada por asiento**, con su `fila` y número.

## F7 — Entradas con QR

**US-19 · Mis entradas** — Como CLIENTE quiero ver mis entradas y su QR.
- Cuando consulto `GET /entradas`, entonces veo solo las mías; `GET /entradas/{id}/qr` devuelve el QR.
- El QR solo se muestra si la entrada está `VENDIDA` y el evento no está cancelado; en otro caso es `null`.
- Cada entrada tiene un código QR único.
- Un ADMIN puede consultar entradas ajenas.

## F8 — Devoluciones

**US-20 · Solicitar devolución** — Como CLIENTE quiero pedir la devolución de una entrada.
- Dada una entrada `VENDIDA` y no usada, dentro del plazo de **72 h desde su emisión**, cuando uso `POST /entradas/{id}/devoluciones`, entonces se crea una devolución `PENDIENTE`.
- Fuera de plazo, entonces recibo `409` ("Plazo de devolución vencido").
- Nota de diseño: la consigna habla de "72 h antes del evento"; la implementación actual cuenta desde la emisión (decisión pendiente en `specs/reglas-de-negocio.md` §9.13).

**US-21 · Resolver devolución** — Como ADMIN quiero aprobar o rechazar devoluciones.
- `POST /devoluciones/{id}/aprobacion`: la entrada pasa a `DEVUELTA`, el QR se invalida y se libera 1 lugar del sector en ventas (si la entrada era de un asiento, ese asiento vuelve a estar libre).
- `POST /devoluciones/{id}/rechazo`: la entrada sigue `VENDIDA`.
- Un usuario no ADMIN recibe `403`.

## F9 — Cancelación de evento

**US-22 · Cancelar evento** — Como ADMIN quiero cancelar un evento y reintegrar a los compradores.
- Cuando el ADMIN cancela (`PATCH /eventos/{id}/estado`), entonces el evento pasa a `CANCELADO` y las entradas vendidas pasan a `A_REINTEGRAR` con el QR invalidado.
- Limitación conocida: el aviso a msvc-entradas es *best-effort* (documentado en `specs/reglas-de-negocio.md` §6).

## F10 — No doble venta (requisito transversal)

**US-23 · Sin sobreventa** — Como organizador quiero que nunca se vendan más entradas que la capacidad ni el mismo asiento dos veces.
- Dadas N reservas simultáneas sobre el último lugar disponible de un sector, entonces exactamente una tiene éxito y el resto recibe `409`.
- Dadas N reservas simultáneas del **mismo asiento** de un sector numerado, entonces exactamente una tiene éxito y el resto recibe `409`.
- Mecanismo: bloqueo pesimista (`SELECT … FOR UPDATE`) sobre `cupo_sector` y claves únicas `(id_evento, id_sector)` en `cupo_sector` y `(id_evento, id_sector, fila, numero)` en `asiento_reservado`.
- Evidencia: `docs/testing/concurrencia.md` y `ReservaAsientoConcurrenciaTest`.

---

## Fuera de alcance de esta entrega (Entrega 2)

Fila virtual, control de acceso por QR (PERSONAL_ACCESO), preferencias y recomendaciones, notificaciones, reportes del organizador, JWT con autorización por roles, resiliencia/caché/trazabilidad y pruebas de carga.
