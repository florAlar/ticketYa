# TicketYa — Reglas de negocio (resumen colaborativo)



## Roles — qué hace cada uno

Roles definidos en la plataforma:

| Rol | En simple |
| --- | --- |
| **CLIENTE** | Busca eventos, reserva, compra, ve sus entradas/QR, pide devolución, edita su perfil. |
| **ORGANIZADOR** | Crea y configura **sus** eventos, publica, edita borradores, puede suspender los propios, ve listado propio; también puede comprar como cualquier persona (mis entradas). |
| **ADMIN** | Opera la plataforma: usuarios (listar, activar/desactivar, roles), cualquier evento (incl. cancelar), cola de devoluciones, consulta de entradas ajenas si hace falta. |
| **PERSONAL_ACCESO** | Valida el QR en la puerta (E2-ACC-01): el sistema verifica que la entrada exista, sea del evento y no esté usada, y la marca como utilizada. Puede validar **cualquier** evento (no hay asignación de eventos por persona: simplificación). |

Registro público: **solo CLIENTE**.  
Organizador, admin y personal de acceso **no** se auto-registran por la web abierta.

### 2.1 Mapa rápido por acción

| Acción | Quién |
| --- | --- |
| Ver catálogo / detalle de evento | Cualquiera (público) |
| Registrarse / iniciar sesión | Público → queda CLIENTE al registrarse |
| Editar mi perfil (nombre, apellido, ciudad) | Usuario logueado |
| Crear / editar borrador de evento | ORGANIZADOR (dueño) o ADMIN |
| Publicar evento | ORGANIZADOR (dueño) o ADMIN |
| Suspender evento | ORGANIZADOR **solo los propios**; ADMIN cualquiera |
| Cancelar evento | **ADMIN** (dispara reintegro de entradas vendidas) |
| Cambiar fecha del evento | Dueño ORGANIZADOR o ADMIN |
| Alta de lugares / sectores / artistas | ORGANIZADOR o ADMIN (los lugares pasan a ser solo ADMIN en LUG-04) |
| Asociar artistas a un evento | Dueño del evento o ADMIN |
| Reservar / comprar / pagar | CLIENTE (y en la práctica quien el gateway autorice a compras; organizador también puede tener “mis entradas”) |
| Ver mis entradas y QR usable | Dueño de la entrada |
| Ver una entrada de otro | ADMIN |
| Pedir devolución | CLIENTE dueño, con reglas de plazo y estado |
| Aprobar / rechazar devolución | ADMIN |
| Listar usuarios / cambiar estado / asignar roles | ADMIN |
| Validar ingreso con QR en puerta | PERSONAL_ACCESO (y ADMIN) — hecho (E2-ACC-01) |
| Reportes de ventas, ocupación y recaudación del evento | ORGANIZADOR dueño (y ADMIN) — hecho (E2-REP-01) |
| Fila virtual alta demanda | — **pendiente E2** |
| Preferencias y recomendaciones | CLIENTE — **pendiente E2** |
| Notificaciones | — **pendiente E2** |

---

## 3. Cuentas y roles (política de producto)

### en uso hoy

- Alta pública → siempre **CLIENTE**.  
- Cuentas demo de prueba (cliente, org, admin, acceso) viven en datos seed para el TP.  
- ADMIN puede **listar usuarios**, **activar/desactivar** y **asignar roles** por API (el front de admin usuarios todavía no está del todo cableado: list/estado/roles sí existen en backend; otras acciones de UI pueden seguir apagadas).

### Diseño acordado pero **no implementado** (extensión a organizador)

Ser organizador **requiere autorización del administrador de la platafoprma**. No hay “registrarme como organizador” abierto.

1. La persona ya es CLIENTE, o tiene una cuenta activa en la plataforma con los datos personales precargados.  
2. Pide extensión de cuenta (solicitud con motivo).  
3. Un ADMIN aprueba o rechaza.  
4. Si aprueba → se otorga rol ORGANIZADOR.  
5. ADMIN y PERSONAL_ACCESO **no** usan ese flujo (los da otro admin / seed).

En el front hay guía (“¿querés organizar?”) y el botón de solicitud está **apagado** hasta que exista la API.

### Aún no cerrado del todo

- Al aprobar organizador: ¿queda **CLIENTE + ORGANIZADOR** a la vez, o se reemplaza el rol? (el diseño lo deja como decisión de política de roles).  
- ¿Puede haber más de un rol activo a la vez en la vida real del producto? El modelo de datos permite varios roles por usuario; la regla de negocio fina no está escrita como entrega formal.

---

## 4. Eventos y lugares

### Estados de un evento

`BORRADOR` → `PUBLICADO` → puede pasar a `AGOTADO`, `SUSPENDIDO`, `CANCELADO`, `FINALIZADO`.

### Borrador

- Se puede crear con poco: **nombre** alcanza al inicio.  
- Fecha/lugar/sectores se completan después.  
- Solo se puede **borrar** si sigue en borrador.  
- Edición completa del borrador: dueño o admin.

### Publicar (reglas que sí aplican)

Para pasar a publicado hace falta, entre otras:

- Nombre y **fecha de inicio futura**  
- Lugar real (no un placeholder tipo “Por definir”)  
- Al menos un sector con **precio** y **capacidad ≥ 1**  
- **No hay cupo ilimitado** (la opción de “sin cupo” se ignora)

Al publicar, el sistema **abre el stock de venta** (cupos por sector). Si eso falla, la publicación no queda a medias: es un paso que debe funcionar.

### Catálogo y búsqueda

- El listado público, por defecto, muestra eventos **publicados** (y en la demo operable también se contemplan agotados en catálogo según seed/config de trabajo).  
- Filtros previstos: tipo, ciudad, fechas, organizador, participante (artista), orden, paginación.  
- Admin puede pedir ver **todos** los estados en gestión.

### Participantes

- Son **artistas / bandas / equipos**, no los compradores.  
- Se asocian al evento (principal, telonero, etc.).  
- Sirven para buscar por artista.

### Suspender vs cancelar

- **Suspender:** el organizador puede suspender **sus** eventos; el admin cualquiera.  
- **Cancelar:** admin; las entradas vendidas pasan a reintegro (ver §6).  
- Nota: el listado global de endpoints a veces resume “suspender/cancelar = admin”; la regla **implementada y documentada en eventos** es la de la tabla de roles de arriba. Conviene **alinear el texto global** cuando se arme la doc formal.

---

## 5. Compra (circuito de la primera entrega)

Orden del circuito:

1. El cliente elige evento y **sector** (y cantidad).  
2. Se crea una **reserva temporal** (en la práctica ~10 minutos).  
3. Confirma y crea la **compra** (con clave para no duplicar si reintenta).  
4. **Pago mock**: aprueba o rechaza.  
5. Si aprueba → se **emiten entradas con QR**.  
6. Si rechaza o vence la reserva → se **libera el cupo**.

### Reglas de negocio ya fijadas en el núcleo

- Límite: **como máximo 6 entradas** por cliente y por evento.  
- Anti doble venta: el stock se maneja por **cupo del sector** (no se vende de más aunque haya muchos a la vez). En sectores numerados, además, **un mismo asiento no se puede tomar dos veces**.  
- **Sectores generales vs numerados** (2026-11-10):
  - **General:** se vende por cantidad (cupo); el cliente no elige ubicación. Una entrada por compra desde la UI.
  - **Numerado:** el cliente elige **asientos concretos** (fila + número), hasta 6 por compra. El sector define `filas` × `asientos por fila` y su **capacidad se deriva siempre** de ese producto (nunca se carga aparte). Se admiten filas 1..200 y 1..500 asientos por fila.
  - Los asientos son **virtuales**: no se guardan asientos libres. Solo existe un registro por asiento **tomado** (reservado o vendido), con clave única por evento + sector + fila + número. Las filas se rotulan A..Z, AA, AB, ...
  - El asiento reservado se libera si la reserva se cancela, vence o el pago se rechaza; pasa a vendido al aprobarse el pago; vuelve a libre si se aprueba la devolución de esa entrada. Si el evento se cancela, los asientos vendidos quedan tomados (no se revenden).
  - El precio es **por sector** (no hay precio por asiento). Un sector numerado no se puede publicar sin layout; en borrador puede quedar incompleto.
  - Cada asiento vendido genera **una entrada** con su fila y número.

### Niveles de cupo (para la UI)

Según lugares libres: agotado / últimos / menos de 50 / menos de 100 / ok.  
Si no hay disponible, el checkout no debe dejar comprar.

---

## 6. Entradas, QR, devoluciones y cancelación

### Estados de una entrada

`VENDIDA` | `USADA` | `DEVUELTA` | `A_REINTEGRAR`

### QR (política de exposición)

- El código usable se muestra solo si la entrada está **VENDIDA** y el evento **no está cancelado**.  
- Si se aprueba una devolución o hay reintegro por cancelación, el QR se **invalida** (deja de servir un pantallazo viejo).  
- Marcar la entrada como **usada en la puerta** = segunda entrega (aún no).

### Devolución pedida por el cliente

- Solo entrada vendida y no usada.  
- Plazo: **72 horas** contadas desde la **emisión** (no desde la fecha del show, según lo documentado en entradas).  
- Admin **aprueba o rechaza**.  
- Si aprueba: entrada devuelta, QR invalidado, **se libera 1 lugar** del sector en ventas, reintegro mock.

### Cancelación del evento

- Entradas vendidas → a reintegrar; QR invalidado.  
- Queda un trámite visible para el cliente (no es la misma cola de “devolución voluntaria” del admin).  
- En el diseño actual, ese paso hacia entradas es **best-effort**: el evento puede quedar cancelado aunque falle el aviso a entradas (deuda de consistencia reconocida; no es un olvido).

### Emisión tras el pago (problema y solución en simple)

**El problema**  
El pago se confirma en **ventas**. Los QR se crean en **entradas** (otro servicio).  
Si justo en ese momento entradas no responde, antes podía pasar: “pago OK” pero el cliente **sin entradas**.  
Eso no se “arregla mágicamente” solo porque usamos Feign: hay que **reintentar y recordar** qué compras todavía no tienen QR.

**Lo que hace el sistema ahora (alcanza para el TP)**  
1. El pago aprobado deja la compra en **APROBADA** y marca la emisión como **PENDIENTE**.  
2. En cuanto termina de guardarse el pago, ventas **intenta varias veces** crear los QR en entradas.  
3. Si igual no pudo, la compra queda **PENDIENTE de emisión**; un **trabajo automático** (cada ~30 s) vuelve a intentar.  
4. La emisión en entradas es **idempotente por compra**: reintentar no duplica entradas.  
5. Si después de muchos intentos no se pudo: estado **FALLIDA** (queda visible para soporte; no se inventa un reembolso automático).  
6. La respuesta de compra expone `emisionEstado` (`NO_REQUIERE` | `PENDIENTE` | `EMITIDA` | `FALLIDA`) para que el front pueda decir “generando entradas…” si hace falta.

**Qué no hacemos todavía (opcional, más adelante)**  
- Tabla **outbox** (cola formal de mensajes en la misma transacción del pago): más “de libro”, no necesaria si el estado PENDIENTE + job alcanza.  
- Compensar el pago (anular compra y devolver cupo) si nunca salió el QR: política de producto aún no cerrada; el TP se cubre con reintento + pendiente.

**Sigue best-effort (otro flujo)**  
Cancelar evento → avisar reintegro a entradas: todavía best-effort (distinto de la emisión post-pago).

---

## 7. Qué hace cada “área” del sistema

| Área | Se ocupa de |
| --- | --- |
| Usuarios | Cuentas, login, roles, perfil |
| Eventos | Eventos, lugares, sectores, artistas, publicar, estados |
| Ventas | Cupos, reservas, compras, pago mock, (más adelante fila y reportes) |
| Entradas | QR, devoluciones, (más adelante validación en puerta) |
| Preferencias | Gustos, recomendaciones, historial (E2, base NoSQL) |
| Notificaciones | Avisos email/push (E2) |
| Gateway | Única puerta de entrada del front; valida sesión y manda al área correcta |

El front **solo habla con el gateway**, no con cada servicio por su cuenta.

---

## 8. Primera entrega vs segunda (para no mezclar)

**Ya cubierto en el núcleo (funcional de E1, según backlog de trabajo):**  
publicar y buscar, reservar y comprar con pago mock, QR, cupos/anti sobreventa, devoluciones admin, cancelación con reintegro, identidad con roles (adelantamos JWT respecto de la consigna E1 “sin auth formal”).

**Explicitamente de la segunda entrega (no bloquear el cierre funcional E1):**  
fila virtual, scan de acceso, preferencias/recs, notificaciones, reportes organizador, compose completo de apps, pruebas de carga, etc.

**Entregables formales de la consigna E1 que aún marcan pendiente en backlog (no son “otra feature de compra”):**  
historias de usuario, diagramas de arquitectura/secuencia, endpoints documentados finos / OpenAPI, Postman, tests unitarios y test de concurrencia. Van a `Backend/docs/` cuando se armen para entregar.

---

## 9. Decisiones pendientes 

Marcar acá lo que **hay que acordar o alinear**. No son tareas de código por sí solas: son charlas de producto/arquitectura.

### Roles y cuentas

1. Al aprobar organizador: ¿**suma** rol ORGANIZADOR al CLIENTE o **reemplaza**?  
2. ¿ADMIN puede también ser comprador con la misma cuenta en producción, o solo en demo?  
3. ¿PERSONAL_ACCESO puede tener entradas propias (hoy el gateway le deja compras/entradas) o solo puerta?  
4. ¿Cuándo cableamos del todo el panel admin de usuarios en el front (listado/estado/roles sí; alta acceso / asignar eventos / borrar siguen fuera o a medias)?  
5. Extensión CLIENTE→ORG: ¿la implementamos en este TP o queda diseño + CTA apagado?

### Eventos y stock

6. ~~Venta por butaca numerada vs solo cupo por sector.~~ **Resuelto (2026-11-10):** modelo híbrido — sector numerado por asiento concreto (asientos virtuales), sector general por cupo. Ver «Reglas de negocio ya fijadas en el núcleo».  
7. ¿Quién puede **cancelar**: solo admin (como ahora) o también el organizador dueño?  
8. Estado **AGOTADO**: ¿lo pone solo el stock en cero de forma automática / derivada, o es un estado que alguien setea a mano? (hoy conviven estado de evento y nivel de cupo en ventas.)  
9. **FINALIZADO**: ¿regla automática por fecha o acción manual?

### Compra y consistencia

10. ~~Si el pago aprueba pero falla la emisión del QR~~ → **cerrado para el TP**: reintentos + `emisionEstado` PENDIENTE/EMITIDA/FALLIDA + job (sin outbox). Opcional futuro: outbox o compensación de pago.  
11. Si se cancela el evento y falla el reintegro masivo: ¿se reintenta, queda cola, aviso manual?  
12. Renovación de reserva: ¿cuántas veces / tope de tiempo total?

### Devoluciones

13. Las 72 h: confirmar con el grupo si queda **desde emisión** (como entradas hoy) o **hasta 72 h antes del evento** (texto de consigna). Hay que **elegir una** y unificar consigna / implementación / doc formal.  
14. ¿El organizador ve devoluciones de sus eventos o solo admin?  
15. Transferencia de entrada a otro usuario: hoy no hay API; ¿E2, extra, o fuera de alcance?

### Segunda entrega  para no olvidar el orden)

16. Fila virtual: reglas de tandas y quién habilita.  
17. Preferencias: qué guarda el cliente y qué tan “inteligente” es la recomendación (puede ser simple).  
18. Notificaciones: canales y en qué momentos exactos avisan.  
19. Reportes: métricas mínimas del organizador.

### Documentación y entrega

20. Qué entra sí o sí en `Backend/docs/` para la primera entrega formal (US, diagramas, Postman, tests).  
21. Alinear textos viejos (endpoints globales vs reglas reales de suspender/org, “scaffold pendiente” en microservicios.md, etc.) sin reescribir historia a mano alzada.

---

## 10. Cómo usar este archivo

- Si el grupo **cierra** una decisión de la §9: moverla a la sección que corresponda (§2–§6) en una frase clara y tachar el ítem pendiente.  
- Si algo se implementa distinto a lo escrito acá: **actualizar este resumen** en la misma sesión (fuente de verdad de negocio del equipo).  
- Detalle de rutas, JSON y estados de implementación: seguir en `Backend/specs/` y en `*/docs/endpoints.md` de cada servicio.  
- Texto para el profesor / entrega: copiar y pulir hacia `Backend/docs/`.

---

## 11. Origen (trazabilidad breve)

Resumen armado a partir de:

- `Backend/specs/consigna.md` — alcance E1/E2 y roles de la consigna  
- `Backend/specs/backlog.md` — qué está hecho, diseño X-ORG, residual FE  
- `Backend/specs/endpoints.md` — roles y mapa de acciones por recurso  
- `Backend/APIGateway/docs/endpoints.md` — quién entra a qué  
- `Backend/msvc-eventos|ventas|entradas|usuarios/docs/endpoints.md` — reglas ya implementadas (publicar, cupo, 72 h, QR, estados)

