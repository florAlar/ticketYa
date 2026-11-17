# TicketYa — Backend

Microservicios Spring Boot + API Gateway.
Specs de trabajo del equipo: `specs/` (consigna, backlog, endpoints, infra, reglas de negocio).
Documentación formal de entrega: `docs/`.

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

DBs publicadas: MySQL 3307–3311, Mongo 27018, Redis 6380. Detalle: `specs/infraestructura.md`.

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
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3000/      # frontend → 200
curl -s http://localhost:8080/api/eventos                            # gateway → 200 con eventos (ruta pública)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@ticketya.com","password":"demo1234"}' | jq -r .token)
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/entradas   # ruta privada → 200
```

Gateway sin `Bearer` en rutas privadas → 401. El JWT es obligatorio (no hay flag); ver README raíz.

## Preferencias, historial y recomendaciones (E2-PRE)

Con el stack arriba y el `TOKEN` del smoke:

```bash
# Preferencias propias (crea defaults si no existe)
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/preferencias/me

# Actualizar artistas favoritos
curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"artistas":["Metallica","Iron Maiden"]}' \
  http://localhost:8080/api/preferencias/me/artistas

# Historial (dueño o ADMIN)
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/clientes/b0000000-0000-4000-8000-000000000001/historial

# Recomendaciones propias (JWT) y similares a un evento (público)
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/recomendaciones/me?limit=10
curl -s "http://localhost:8080/api/eventos/d0000000-0000-4000-8000-000000000001/recomendaciones?limit=6"
```

- La ingesta es asíncrona: al aprobar una compra, `compra.confirmada` llega a la cola
  `preferencias.compras` y se guarda en `interacciones_eventos` (idempotente por `idCompra`).
- Compras viejas del seed de MySQL: `./scripts/backfill-preferencias.sh` (idempotente).
  El script envía `X-Internal-Token` (`TICKETYA_INTERNAL_TOKEN`, default `ticketya_dev_internal`).
- Volumen de Mongo ya creado: `./scripts/seed-preferencias.sh` aplica índices y seed de co-compra.
- Detalle del algoritmo: `msvc-preferencias/docs/recomendaciones.md`.
- Swagger del microservicio: `http://localhost:8085/swagger-ui/index.html`
  (spec JSON en `Backend/docs/openapi/msvc-preferencias.json`).
- Frontend: Home “Recomendados para vos”, detalle “Eventos similares” y perfil
  “Mis preferencias” + “Mi historial”.

## Correr un msvc en local (sin imagen)

Ejemplo notificaciones:

```bash
cd Backend/msvc-notificaciones
mvn.cmd -DskipTests spring-boot:run
```

Si el msvc necesita DB, levantar antes solo el servicio de datos del compose correspondiente.

## Specs y docs

| Archivo | Contenido |
| --- | --- |
| `specs/reglas-de-negocio.md` | Resumen humano + decisiones pendientes |
| `specs/consigna.md` | TP |
| `specs/backlog.md` | Tareas E1/E2 |
| `specs/endpoints.md` | Rutas y enrutado |
| `specs/microservicios.md` | Mapa de servicios |
| `specs/comunicacion.md` | Cliente ↔ gateway ↔ msvc |
| `specs/infraestructura.md` | Docker y puertos |
| `specs/diagramas/modelo.md` | ER global |
| `docs/` | Entrega formal (índice en `docs/README.md`) |

Front del monorepo: ver README raíz y `Frontend/README.md`.

## IMPORTANTE
Las colas en RabbitMQ suelen tardar en crearse aprox (2-3 min) POR LO TANTO esperar para recibir las notificaciones.
Ídem para el sistema de preferencias, suele tardar unos minutos hasta que MongoDB cargue por completo.
Al entrar a MailHog procurar que este "Connected". No hace falta tocar nada más. Es un simulador SMTP, cualquier mail usado/registrado en el sistema las notificaciones por email llegará ahí mismo.
