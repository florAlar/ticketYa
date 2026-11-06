# TicketYa — Backend

Microservicios Spring Boot + API Gateway. Documentación en `docs/` (consigna, backlog, endpoints, infra).

## Requisitos

- JDK 17+
- Maven 3.9+ (`mvn.cmd` en Windows)
- Docker / Docker Compose (bases y stack)
- Opcional: variables desde la raíz del monorepo — copiar `.env.example` → `.env`

## Puertos (local)

| Servicio | URL smoke |
| --- | --- |
| Gateway | http://localhost:8080/ |
| Usuarios | http://localhost:8081/ |
| Eventos | http://localhost:8082/ |
| Ventas | http://localhost:8083/ |
| Entradas | http://localhost:8084/ |
| Preferencias | http://localhost:8085/ |
| Notificaciones | http://localhost:8086/ |
| Frontend (Docker/Nginx) | http://localhost:3000/ |

DBs publicadas: MySQL 3307–3311, Mongo 27018, Redis 6380. Detalle: `docs/infraestructura.md`.

## Levantar con Docker

Un msvc + su DB:

```bash
cd Backend/msvc-eventos
docker compose up -d --build
```

Stack completo (gateway + msvcs + DBs + frontend):

```bash
cd Backend
docker compose up -d --build
```

Bajar:

```bash
docker compose down
```

## Smoke (apps arriba)

```bash
curl -s http://localhost:8080/
curl -s http://localhost:8082/
curl -s http://localhost:8086/
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3000/
```

Respuesta esperada hoy: texto tipo `api gateway ok!` / `microservicio … ok!` (sin API de negocio aún); FE HTTP 200.

## Correr un msvc en local (sin imagen)

Ejemplo notificaciones:

```bash
cd Backend/msvc-notificaciones
mvn.cmd -DskipTests spring-boot:run
```

Si el msvc necesita DB, levantar antes solo el servicio de datos del compose correspondiente.

## Docs útiles

| Archivo | Contenido |
| --- | --- |
| `docs/consigna.md` | TP |
| `docs/backlog.md` | Tareas E1/E2 |
| `docs/endpoints.md` | Rutas y enrutado |
| `docs/microservicios.md` | Mapa de servicios |
| `docs/comunicacion.md` | Cliente ↔ gateway ↔ msvc |
| `docs/infraestructura.md` | Docker y puertos |
| `docs/diagramas/modelo.md` | ER global |

Front del monorepo: ver README raíz y `Frontend/README.md`.
