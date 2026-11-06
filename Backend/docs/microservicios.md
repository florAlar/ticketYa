# Microservicios

Cada contexto es un proyecto Spring Boot independiente (carpeta + `pom.xml` + `Dockerfile` + `docker-compose.yml`). No se comparten entidades JPA/documentos entre servicios.

| Servicio (repo) | Contenedor app | Puerto app | Persistencia | Responsabilidad |
| --- | --- | ---: | --- | --- |
| `APIGateway` | ms-gateway | 8080 | — | Entrada HTTP; enruta al msvc; no tiene DB |
| `msvc-usuarios` | ms-usuarios | 8081 | MySQL (`db-mysql-usuarios`, host 3307) | Identidad, roles, credenciales / auth |
| `msvc-eventos` | ms-eventos | 8082 | MySQL (`db-mysql-eventos`, host 3308) | Eventos, lugares, sectores, participantes, publicación, búsqueda |
| `msvc-ventas` | ms-ventas | 8083 | MySQL + Redis (`db-mysql-ventas` 3309, `db-redis-ventas` 6380) | Disponibilidad, reservas, compras, pagos, reportes, fila virtual |
| `msvc-entradas` | ms-entradas | 8084 | MySQL (`db-mysql-entradas`, host 3310) | Emisión/QR, devoluciones, validación de acceso |
| `msvc-preferencias` | ms-preferencias | 8085 | MongoDB (`db-mongo-preferencias`, host 27018) | Preferencias, recomendaciones, historial cliente |
| `msvc-notificaciones` | ms-notificaciones | 8086 | MySQL (`db-mysql-notificaciones`, host 3311) | Envíos email/push, plantillas, estado de notificación |

## Estado actual (scaffold)

- Smoke `GET /` en gateway y cada msvc (texto ok).
- Lógica de dominio y rutas de negocio: pendientes (ver `docs/backlog.md`).
- Prefijos públicos y solapes: `docs/endpoints.md`.
- Modelo de dominio global: `docs/diagramas/modelo.md` (preferencias Mongo aún no modelado como documentos — backlog MOD-04).

## Docs por contexto

Cada msvc/gateway tiene `docs/` para specs del bounded context (plantilla y contenido: backlog DOC-MS-*).

## Compose

- Individual: `Backend/<msvc>/docker-compose.yml` (app + su DB).
- Completo: `Backend/docker-compose.yml` (`include` de todos).
- Detalle de puertos y diagramas Docker: `docs/infraestructura.md`.
