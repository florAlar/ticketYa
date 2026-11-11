# TicketYa — interfaz de servicio.

## Convenciones generales de la API

La API de TicketYa utiliza el protocolo HTTP y el formato JSON para la comunicación entre el frontend y el backend. Todas las solicitudes externas ingresan a través del API Gateway, utilizando el prefijo común https://ticketya.com.ar/api/.

### Métodos HTTP

| Método | Uso |
| --- | --- |
| GET | Consultar recursos o información. |
| POST | Crear recursos o ejecutar operaciones. |
| PUT | Reemplazar completamente un recurso. |
| PATCH | Modificar parcialmente un recurso. |
| DELETE | Eliminar un recurso cuando corresponda. |

### Headers HTTP (encabezado de la solicitud)

| Header | Descripción |
| --- | --- |
| Authorization: Bearer *** | Envía el token de autenticación del usuario. |
| Content-Type: application/json | Indica que el cuerpo de la solicitud está en formato JSON. |
| Idempotency-Key: <UUID> | Evita la duplicación de operaciones ante reintentos. Se utiliza en operaciones críticas, como compras, pagos y reservas. |
| X-Correlation-Id: <ID> | Identifica una solicitud para permitir su seguimiento a través de los distintos microservicios. El Gateway puede generarlo si no está presente. |

### Autenticación y autorización

La autenticación se centraliza en el API Gateway, que recibe y valida el JWT enviado por el cliente. Luego, transmite a los microservicios la identidad y los roles del usuario ya resueltos.

La autorización determina qué operaciones puede realizar cada usuario según sus permisos.

Los roles definidos para la plataforma son:

- CLIENTE: realiza compras, reservas y consulta eventos.
- ORGANIZADOR: administra sus eventos y consulta sus ventas.
- ADMIN: administra las transacciones de la empresa.
- PERSONAL_ACCESO: valida entradas y registra ingresos.

msvc-usuarios administra identidad/roles y emisión de credenciales;
el gateway autentica y enruta.

Cada endpoint establece qué roles están autorizados para ejecutarlo.

Respuestas: formato JSON. /  Errores - Estados HTTP + cuerpo. etc

## Endpoint principal y recursos

Endpoint principal (catálogo público): GET /api/eventos

Ejemplo: GET /api/eventos?tipo=recital&ciudad=Tandil&fechaDesde=2027-01-01&fechaHasta=2027-01-31&participante=123&organizador=456&orden=fecha&page=0&size=20

Query: tipo | organizador | participante | ciudad | fechaDesde | fechaHasta | precioMin | precioMax | orden(fecha|precio|relevancia) | page | size

### Recursos de primer nivel:

- api/auth
- api/usuarios
- api/eventos
- api/lugares
- api/sectores
- api/participantes
- api/organizadores
- api/reservas
- api/compras
- api/pagos
- api/reports
- api/entradas
- api/devoluciones
- api/preferencias
- api/recomendaciones
- api/clients
- api/notificaciones

### Enrutamiento:  gateway → servicio

- /auth/** → msvc-usuarios
- /usuarios/** → msvc-usuarios

- /eventos/** → msvc-eventos  (EXCEPCIONES abajo)
- /lugares/** → msvc-eventos
- /participantes/** → msvc-eventos
- /organizadores/** → msvc-eventos
- /sectores/** → msvc-eventos

- /reservas/** → msvc-ventas
- /compras/** → msvc-ventas
- /pagos/** → msvc-ventas
- /reportes/** → msvc-ventas

- /entradas/** → msvc-entradas
- /devoluciones/** → msvc-entradas

- /preferencias/** → msvc-preferencias-recomendaciones
- /recomendaciones/** → msvc-preferencias-recomendaciones
- /clientes/** → msvc-preferencias-recomendaciones

- /notificaciones/** → msvc-notificaciones

### superposicion de servicios en endpoint  /eventos/** (mismo prefijo, distinto servicio): :

- /eventos/{id}/disponibilidad | /eventos/{id}/fila/** -> msvc-ventas (consulta por disponibilidad de evento)
- /eventos/{idEvento}/validaciones -> msvc-entradas (personal de acceso valida una entrada al momento de ingresar al evento. )
- /eventos/{id}/recomendaciones ->msvc-preferencias

El gateway debe enrutar estas rutas específicas antes del catch-all /eventos/** → msvc-eventos.

## usuarios — identidad, roles (auth de request en gateway)

Servicio: msvc-usuarios | Prefijos: /auth, /usuarios

- POST:  /auth/register — Registrar cliente (solo rol CLIENTE público)
- POST : /auth/login — Login; emite token consumido/validado por el gateway
- POST:  /auth/refresh — Renovar token
- POST:  /auth/logout — Cerrar sesión / invalidar refresh
- GET : /usuarios/me — Perfil del usuario autenticado
- PATCH:  /usuarios/me — Actualizar/modificardatos perfil propio
- GET : /usuarios — Listar usuarios (ADMIN)
- GET : /usuarios/{id} — Consultar usuario (ADMIN)
- PATCH: /usuarios/{id}/estado — Activar/desactivar usuario (ADMIN)
- GET : /usuarios/{id}/roles — Consultar roles (ADMIN)
- PUT : /usuarios/{id}/roles — Asignar roles (ADMIN)

## eventos — eventos, lugares, sectores, participantes

Servicio: msvc-eventos | Prefijos: /eventos (catálogo), /lugares, /sectores, /participantes, /organizadores

### recurso Eventos

- GET: /eventos — Buscar eventos con filtros/paginación (endpoint principal) (ver ejemplo principal)
- GET: /eventos/{id} — Detalle de evento
- POST: /eventos — Crear evento en borrador (ORGANIZADOR)
- PUT: /eventos/{id} — Actualizar configuración del evento (ORGANIZADOR)
- DELETE: /eventos/{id} — Eliminar borrador (ORGANIZADOR)
- POST: /eventos/{id}/publicacion — Publicar evento (valida fecha/lugar/sectores/precios/capacidad)
- PATCH: /eventos/{id}/estado — Suspender o cancelar evento (ADMIN); cancelación dispara reintegros/notifs
- PATCH: /eventos/{id}/fecha — Modificar fecha/hora (ORGANIZADOR/ADMIN)
- GET: /organizadores/me/eventos — Listar eventos propios (ORGANIZADOR)
- GET: /eventos/{id}/sectores — Sectores configurados del evento
- PUT: /eventos/{id}/sectores/{sectorId}/precio — Configurar precio de sector en el evento (ORGANIZADOR)
- PUT: /eventos/{id}/participantes — Asociar participantes al evento (ORGANIZADOR)

### recurso Lugares y sectores

- GET: /lugares — Buscar lugares
- GET: /lugares/{id} — Consultar lugar y sectores
- POST: /lugares — Crear lugar (ORGANIZADOR/ADMIN)
- PUT: /lugares/{id} — Modificar lugar (ORGANIZADOR/ADMIN)
- POST: /lugares/{id}/sectores — Crear sector (ORGANIZADOR/ADMIN). Sector numerado: `filas` (1..200) y `asientosPorFila` (1..500) obligatorios; la capacidad se deriva (`filas × asientosPorFila`)
- PUT: /sectores/{id} — Modificar sector (ORGANIZADOR/ADMIN). Mismas reglas de layout
- GET: /lugares/{id}/sectores/{sectorId}/asientos — Asientos **virtuales** del sector numerado, calculados desde su layout (fila + número; sin ids ni filas persistidas; lista vacía en sectores generales). No es disponibilidad comercial

Disponibilidad comercial = msvc-ventas.

### Recurso Participantes

- GET: /participantes — Buscar artistas/bandas principales/teloneros
- GET: /participantes/{id} — Consultar participante
- POST: /participantes — Crear participante (ORGANIZADOR/ADMIN)
- PUT: /participantes/{id} — Actualizar participante
- GET: /eventos/{id}/participantes — Listar artistas del evento

## ventas — disponibilidad, reservas, compras, pagos, reportes, fila

Servicio: msvc-ventas | Prefijos: /reservas, /compras, /pagos, /reportes + /eventos/{id}/disponibilidad y /eventos/{id}/fila/**

### Disponibilidad y reservas

- GET /eventos/disponibilidad?ids= — Batch disponibilidad comercial (catálogo; máx 50; público)
- GET /eventos/{id}/disponibilidad — Consultar disponibilidad comercial (cupos por sector; DTO con disponibleTotal + nivelCupo; en sectores numerados `asientosOcupados[{fila,numero,estado}]`, el resto del layout está libre)
- POST /eventos/{id}/cupos — Abrir/asegurar cupo comercial al publicar (body sectores[{idSector,capacidadTotal}]; idempotente; Feign msvc-eventos)

/EL RECURSO ES EVENTOS PERO LE PEGA A ESTE SERVICIO, HAY QUE RUTEARLO DESDE GATEWAY.

- POST /reservas — Crear reserva temporal (TTL p.ej. 10 min; atómica; cliente desde identidad gateway). Items `{idSector, cantidad}` en sectores generales; `{idSector, asientos:[{fila,numero}]}` en sectores numerados (409 si algún asiento ya está tomado)
- GET /reservas/{id} — Consultar reserva propia
- POST /reservas/{id}/confirmacion — Confirmar intención de compra
- POST /reservas/{id}/cancelacion — Cancelar reserva y liberar cupo/asientos
- POST /reservas/{id}/renovacion — Extender reserva (no automática/ilimitada)

### Compras y pagos

- POST /compras — Crear compra desde reserva válida (Idempotency-Key)
- GET /compras/{id} — Consultar compra propia
- GET /compras — Listar compras propias
- POST /compras/{id}/pagos — Iniciar pago
- GET /compras/{id}/pagos — Consultar pagos de la compra
- POST /pagos/{id}/reintentos — Reintentar pago permitido
- POST /pagos/webhooks/{proveedor} — Webhook pasarela (firma; no confiar solo en browser)

### Reportes

- GET /reportes/eventos/{id}/ventas — Ventas por evento (ORGANIZADOR dueño)
- GET /reportes/eventos/{id}/sectores — Ventas/devoluciones por sector (ORGANIZADOR)
- GET /reportes/eventos/{id}/ocupacion — Porcentaje de ocupación (ORGANIZADOR)
- GET /reportes/eventos/{id}/recaudacion — Recaudación del evento (ORGANIZADOR)

### Fila virtual

- POST /eventos/{id}/fila/ingreso — Ingresar a fila virtual (posición asignada en servidor)
- GET /eventos/{id}/fila/posicion — Consultar posición/estado propio
- POST /eventos/{id}/fila/validacion — Validar token temporal de acceso a compra
- GET /eventos/{id}/fila/estado — Estado general de la venta/fila (público)
- POST /eventos/{id}/fila/habilitacion — Habilitar tanda de clientes (ADMIN/Sistema)

## entradas — emisión, QR, validación, devoluciones

Servicio: msvc-entradas | Prefijos: /entradas, /devoluciones + /eventos/{idEvento}/validaciones

### Entradas y validación

- GET /entradas — Listar entradas propias (CLIENTE)
- GET /entradas/{id} — Consultar entrada propia
- GET /entradas/{id}/qr — Obtener QR de la entrada
- POST /eventos/{idEvento}/validaciones — Validar QR en ingreso (PERSONAL_ACCESO o ADMIN; atómica). Body `{codigoQR, dispositivo?}`. 200 `VALIDA` | 409 `YA_USADA` / `OTRO_EVENTO` / `NO_VALIDA` / `EVENTO_CANCELADO` | 404 | 400 | 403
- GET /eventos/{idEvento}/validaciones — Consultar validaciones del evento (ADMIN; ORGANIZADOR solo el dueño)

### Devoluciones

- POST /entradas/{id}/devoluciones — Solicitar devolución (CLIENTE; p.ej. plazo 72h, no usada)
- GET /devoluciones/{id} — Consultar solicitud propia
- GET /devoluciones — Listar solicitudes pendientes (ADMIN)
- POST /devoluciones/{id}/aprobacion — Aprobar devolución + reintegro (ADMIN)
- POST /devoluciones/{id}/rechazo — Rechazar devolución (ADMIN)

## preferencias — preferencias, historial, recs

Servicio: msvc-preferencias-recomendaciones | Prefijos: /preferencias, /recomendaciones, /clientes + /eventos/{id}/recomendaciones

- GET /preferencias/me — Consultar preferencias propias
- PUT /preferencias/me — Actualizar preferencias (categorías, ciudad, rangoPrecio, etc.)
- GET /preferencias/me/artistas — Consultar artistas favoritos
- PUT /preferencias/me/artistas — Actualizar artistas favoritos
- GET /recomendaciones/me — Eventos recomendados (?limit=)
- GET /eventos/{id}/recomendaciones — Eventos similares (público)

Solape de prefijo: /eventos/{id}/recomendaciones → msvc-preferencias (no eventos).

- GET /clientes/{id}/historial — Historial del cliente (CLIENTE dueño / ADMIN)

Ingesta async típica: CompraConfirmada, EntradaValidada (no REST entre msvcs).

## notificaciones — email/push y admin de envíos

Servicio: msvc-notificaciones | Prefijo: /notificaciones | disparos de negocio vía broker; REST para consulta/admin

- GET /notificaciones/me — Listar notificaciones propias
- GET /notificaciones/me/{id} — Detalle de notificación
- PATCH /notificaciones/me/{id}/leida — Marcar como leída
- GET /notificaciones/me/preferencias — Canales habilitados
- PUT /notificaciones/me/preferencias — Actualizar canales
- POST /notificaciones — Crear notificación administrativa (ADMIN)
- GET /notificaciones/{id}/estado — Estado de envío (ADMIN)

Eventos broker (no REST, ambos docs): CompraConfirmada; EventoCancelado; EventoModificado; NuevoEventoArtistaFavorito; DevolucionAprobada.

## 5. resumen

- msvc-usuarios  → 	/auth,
  /usuarios
- msvc-eventos → /eventos (CRUD/catálogo/publicación/fecha/estado),
  /lugares,
  /sectores,
  /participantes,
  /organizadores
- msvc-ventas → /reservas,
  /compras,
  /pagos,
  /reportes,
  /eventos/{id}/disponibilidad,
  /eventos/{id}/fila/**
- msvc-entradas  → /entradas,
  /devoluciones,
  /eventos/{idEvento}/validaciones
- msvc-preferencias → /preferencias,
  /recomendaciones,
  /clientes,
  /eventos/{id}/recomendaciones
- msvc-notificaciones→/notificaciones

## 7. Checklist de cobertura funcional

- Gestión eventos → /eventos*
- Lugares/sectores/precios → /lugares, /sectores, /eventos/{id}/sectores*
- Publicación/admin → /eventos/{id}/publicacion, /eventos/{id}/estado
- Reservas/compras/pagos + anti doble venta → /reservas, /compras, /pagos
- Fila virtual → /eventos/{id}/fila/**
- Devoluciones/cancelaciones → /devoluciones, /eventos/{id}/estado
- Validación QR → /eventos/{idEvento}/validaciones
- Búsqueda/filtros → GET /eventos
- Preferencias/recs → /preferencias, /recomendaciones
- Notificaciones → /notificaciones + broker
- Reportes organizador → /reportes/eventos/{id}/**
