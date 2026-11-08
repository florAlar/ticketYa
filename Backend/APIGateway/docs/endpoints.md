# Endpoints — API Gateway

Copia de referencia para este servicio.
**Fuente canónica (no borrar):** `Backend/specs/endpoints.md`

Este archivo no sustituye el global: solo concentra lo que el gateway debe enrutar / validar.

---

## Estado de implementación (gateway)

| ID | Qué | Estado |
| --- | --- | --- |
| API-05 / E1-GW-01 | Enrutado `/api/**` → msvcs + excepciones `/eventos/**` | **hecho** (`application.yml` routes) |
| E1-GW-03 | CORS orígenes Vite (:5173) y FE nginx (:3000) | **hecho** (`SecurityConfig` CorsConfigurationSource) |
| E1-GW-02 | Propagar identidad a msvcs | **hecho** — headers `X-User-Id`, `X-User-Email`, `X-User-Role`, `X-User-Roles` tras JWT OK |
| Auth JWT en gateway | Validación + RBAC por path | **hecho (estructura)** — `JwtUtil` + `JwtAuthenticationFilter` (patrón gatewayejemplo adaptado a MVC). Emisión de tokens = msvc-usuarios (pendiente). Secret: `jwt.secret` / `JWT_SECRET` |

### Auth / roles (filtro)

Público (sin Bearer): `GET /`, `POST /api/auth/login|register`, `GET /api/eventos`, `GET /api/eventos/{id}`, recomendaciones GET, swagger.

Protegido: Bearer JWT válido + rol:

| Rol | Alcance (resumen) |
| --- | --- |
| ADMIN | todo |
| ORGANIZADOR | eventos/lugares/…, reportes, me, GET validaciones; **también** entradas/compras/devoluciones propias (Mis entradas) |
| CLIENTE | reservas/compras/entradas/devoluciones, disponibilidad, me, preferencias |
| PERSONAL_ACCESO | me + POST/GET validaciones; **también** entradas/compras propias |

401/403 JSON: `{"error","message"}` como el ejemplo.

Claims JWT esperados: `sub`/`email`, `userId`, `rol` y/o `roles` (lista/CSV). Alias `ACCESO`→`PERSONAL_ACCESO`.

Smoke local: `GET http://localhost:8080/` → `api gateway ok!`  
Proxy catálogo (con msvc-eventos arriba): `GET http://localhost:8080/api/eventos`  
Protegido sin token: `GET http://localhost:8080/api/usuarios/me` → 401.

Strip: cliente llama `/api/eventos` → msvc recibe `/eventos` (`StripPrefix=1`).

URIs backend (default host / Docker compose):

| Var | Default host | Compose |
| --- | --- | --- |
| TICKETYA_URI_USUARIOS | http://localhost:8081 | http://ms-usuarios:8081 |
| TICKETYA_URI_EVENTOS | http://localhost:8082 | http://ms-eventos:8082 |
| TICKETYA_URI_VENTAS | http://localhost:8083 | http://ms-ventas:8083 |
| TICKETYA_URI_ENTRADAS | http://localhost:8084 | http://ms-entradas:8084 |
| TICKETYA_URI_PREFERENCIAS | http://localhost:8085 | http://ms-preferencias:8085 |
| TICKETYA_URI_NOTIFICACIONES | http://localhost:8086 | http://ms-notificaciones:8086 |

CORS: `ticketya.cors.allowed-origins` / `TICKETYA_CORS_ORIGINS`.

---

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

---

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

### Enrutamiento:  gateway → servicio (implementado)

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

- /eventos/{id}/disponibilidad | /eventos/{id}/cupos (POST) | /eventos/{id}/cupos/liberacion (POST) | /eventos/{id}/fila/** -> msvc-ventas
- /eventos/{idEvento}/validaciones -> msvc-entradas (personal de acceso valida una entrada al momento de ingresar al evento. )
- /eventos/{id}/recomendaciones ->msvc-preferencias

El gateway enruta estas rutas específicas **antes** del catch-all /eventos/** → msvc-eventos (orders 10–13 vs 20).

---

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
  /eventos/{id}/cupos (POST, apertura stock al publicar),
  /eventos/{id}/cupos/liberacion (POST, reabrir stock por devolución),
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
