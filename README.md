# TicketYa

Trabajo práctico: venta de entradas para eventos con **API Gateway + 6 microservicios Spring Boot** y una **SPA React**.

El navegador habla **únicamente con el API Gateway** (`:8080`), que valida el JWT, aplica autorización por rol y enruta al microservicio correspondiente. Cada microservicio tiene su propia base de datos.

```text
ticketYa/
├── Backend/
│   ├── APIGateway/           # punto de entrada único (Spring Cloud Gateway MVC)
│   ├── msvc-usuarios/        # identidad, roles y auth (MySQL)
│   ├── msvc-eventos/         # eventos, lugares, sectores y participantes (MySQL + RabbitMQ)
│   ├── msvc-ventas/          # disponibilidad, reservas, compras, pagos y fila virtual (MySQL + Redis)
│   ├── msvc-entradas/        # emisión/QR, devoluciones y control de acceso (MySQL)
│   ├── msvc-preferencias/    # preferencias, historial y recomendaciones (MongoDB)
│   ├── msvc-notificaciones/  # plantillas, email/push e intentos de envío (MySQL + RabbitMQ + MailHog)
│   ├── infrastructure/       # esquemas, seeds y RabbitMQ compartido
│   ├── specs/                # documentación de trabajo del equipo
│   ├── docs/                 # documentación formal de entrega (arquitectura, OpenAPI, Postman, testing)
│   └── docker-compose.yml    # stack completo (include de todos los compose)
├── Frontend/                 # SPA React (Vite) — habla solo con el gateway
├── .env.example              # variables opcionales del backend
└── README.md
```

## Stack

| Capa | Tecnología |
| --- | --- |
| Backend | Java 17, Spring Boot 4, Spring Cloud Gateway MVC, Spring Data JPA, OpenFeign, Spring AMQP |
| Frontend | React 19, Vite 7, React Router 7, CSS propio |
| Datos | MySQL 8.4 (una instancia por servicio), MongoDB 7, Redis 7 |
| Mensajería | RabbitMQ 3.13 (`eventos.exchange`) |
| Emails de prueba | MailHog (SMTP `1025`, bandeja web `8025`) |
| Infra | Docker + Docker Compose v2 |

## Contenido

- [Funcionalidad principal](#funcionalidad-principal)
- [Inicio rápido (Docker)](#inicio-rápido-docker)
- [Servicios y puertos](#servicios-y-puertos)
- [Usuarios demo](#usuarios-demo)
- [Autenticación](#autenticación)
- [Frontend en desarrollo local](#frontend-en-desarrollo-local)
- [Tests](#tests)
- [Documentación](#documentación)
- [Troubleshooting](#troubleshooting)

## Funcionalidad principal

- Catálogo público de eventos con búsqueda, filtros, detalle, lugares y sectores.
- Compra de entradas: disponibilidad, reserva temporal, pago simulado, asientos numerados y **fila virtual** para eventos de alta demanda.
- Entradas con QR, validación de acceso en puerta y devoluciones.
- Gestión de eventos (alta/edición, publicación y cambios de estado), lugares, sectores y reportes para organizadores.
- Preferencias, historial y recomendaciones; notificaciones por email/push con bandeja propia.
- Autenticación con JWT y roles `CLIENTE`, `ORGANIZADOR`, `PERSONAL_ACCESO` y `ADMIN`.

## Inicio rápido (Docker)

Requisitos: Docker con Compose v2 y estos puertos libres:

`80`, `3000`, `8080`–`8086`, `3307`–`3311`, `5672`, `6380`, `1025`, `8025`, `15672`, `27018`.

No hace falta crear un `.env`: los compose traen valores de desarrollo por defecto. Solo si querés cambiar algo, copiá `.env.example` a `.env` (raíz o `Backend/`).

```bash
cd Backend
docker compose up -d --build
```

El primer build tarda unos minutos. Levanta **17 contenedores**: gateway, 6 microservicios, frontend (Nginx), 5 MySQL, MongoDB, Redis, RabbitMQ y MailHog.

Verificar:

```bash
docker compose ps                                                       # todo Up; las bases healthy
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3000/         # frontend → 200
curl -s http://localhost:8080/api/eventos                               # gateway → 200 con eventos
```

- Frontend: <http://localhost> (y también <http://localhost:3000>).
- Gateway: <http://localhost:8080>.
- Estado del stack con smoke HTTP: `sudo ./scripts/status.sh --http` (desde `Backend/`).
- Bajar: `docker compose down`. Reiniciar desde cero y borrar las bases: `docker compose down -v`.

> Los `init.sql` / `init.js` de `Backend/infrastructure/` solo corren sobre volúmenes nuevos. Si cambia el esquema, recreá las bases con `docker compose down -v && docker compose up -d --build`.

## Servicios y puertos

### Aplicaciones

| Puerto | Servicio | Contenedor | Persistencia | Rutas del gateway |
| ---: | --- | --- | --- | --- |
| **8080** | `APIGateway` | `ms-gateway` | — | Único punto de entrada al backend |
| **8081** | `msvc-usuarios` | `ms-usuarios` | MySQL `3307` | `auth`, `usuarios` |
| **8082** | `msvc-eventos` | `ms-eventos` | MySQL `3308` | `eventos`, `lugares`, `sectores`, `participantes`, `organizadores` |
| **8083** | `msvc-ventas` | `ms-ventas` | MySQL `3309` + Redis `6380` | `reservas`, `compras`, `pagos`, `reportes`, disponibilidad y fila virtual |
| **8084** | `msvc-entradas` | `ms-entradas` | MySQL `3310` | `entradas`, `devoluciones`, validaciones QR |
| **8085** | `msvc-preferencias` | `ms-preferencias` | MongoDB `27018` | `preferencias`, `recomendaciones`, `clientes` |
| **8086** | `msvc-notificaciones` | `ms-notificaciones` | MySQL `3311` | `notificaciones` |
| **3000** | Frontend (Nginx) | `ms-frontend` | — | El usuario/browser; también en `:80` |
| **5173** | Vite dev server | — | — | Alternativa local (`npm run dev`) |

### Infraestructura

| Puerto | Servicio | Uso |
| ---: | --- | --- |
| `5672` / `15672` | RabbitMQ | Eventos de dominio y panel web (`ticketya` / `ticketya_dev`) |
| `1025` / `8025` | MailHog | SMTP de prueba y bandeja web (<http://localhost:8025>) |
| `3307`–`3311` | MySQL 8.4 | Una instancia por microservicio con MySQL |
| `27018` | MongoDB 7.0 | `msvc-preferencias` (interno `27017`) |
| `6380` | Redis 7 | `msvc-ventas` (interno `6379`) |

Flujo de red: **Navegador → `:80`/`:3000` (Nginx) → `:8080` (gateway) → `:8081`–`:8086` (msvc)**. Entre contenedores se usan los nombres de servicio (`ms-eventos:8082`, etc.).

Detalle de rutas y permisos: [`Backend/docs/api/gateway.md`](Backend/docs/api/gateway.md).

### Variables de entorno

Todas son opcionales en desarrollo (los compose aplican valores por defecto). Las más usadas:

| Variable | Default | Uso |
| --- | --- | --- |
| `MYSQL_PASSWORD` | `ticketya_dev` | Password del usuario `ticketya` en las 5 MySQL |
| `MYSQL_ROOT_PASSWORD` | `root_dev` | Password de `root` en MySQL |
| `MONGO_PASSWORD` | `mongo_dev` | Password de MongoDB |
| `RABBITMQ_PASSWORD` | `ticketya_dev` | Password del usuario `ticketya` en RabbitMQ |
| `JWT_SECRET` | secreto de desarrollo | **Compartido** entre gateway y `msvc-usuarios` |
| `TICKETYA_INTERNAL_TOKEN` | `ticketya_dev_internal` | Sella llamadas internas (fila virtual, backfill) |
| `TICKETYA_FILA_*` | ver `.env.example` | Prefijo, TTL y job de tandas de la fila virtual |

> Si rotás `JWT_SECRET`, usá el mismo valor en gateway y usuarios; si no, el gateway rechaza todos los tokens.

## Usuarios demo

| Rol | Email | Password |
| --- | --- | --- |
| Cliente | `demo@ticketya.com` | `demo1234` |
| Organizador | `org@ticketya.com` | `org1234` |
| Admin | `admin@ticketya.com` | `admin1234` |
| Personal de acceso | `acceso@ticketya.com` | `acceso1234` |

Las cuentas salen del seed de `Backend/infrastructure/mysql/usuarios/init.sql`. En el frontend también están en `DEMO_ACCOUNTS` (`Frontend/src/auth/roles.js`), que se usa en el toast de `/login` cuando `VITE_USE_MOCK=true`.

## Autenticación

El gateway exige **JWT + roles**. Fuera de las rutas públicas, un request sin `Authorization: Bearer <token>` recibe **401**. Los headers `X-User-Id` / `X-User-Role` que envíe el cliente **nunca se confían**: el gateway los sobrescribe con los claims del token y los vacía en rutas públicas.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@ticketya.com","password":"demo1234"}' | jq -r .token)

curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/entradas
```

Rutas públicas (sin token):

- `POST /api/auth/login` y `POST /api/auth/register`
- `GET /api/eventos`, `GET /api/eventos/{id}` y `GET /api/eventos/{id}/participantes`
- `GET /api/participantes` y `GET /api/participantes/{id}`
- `GET /api/eventos/disponibilidad` y `GET /api/eventos/{id}/disponibilidad`
- `GET /api/eventos/{id}/recomendaciones`
- `GET /api/eventos/{id}/fila/estado`

Swagger de cada microservicio: `http://localhost:8081/swagger-ui/index.html` (repetir con `8082`–`8086`). La documentación OpenAPI también está en [`Backend/docs/openapi/`](Backend/docs/openapi/).

## Frontend en desarrollo local

Alternativa al frontend de Docker (`:3000`) con recarga en caliente. **Necesita el backend corriendo.**

```bash
cd Frontend
npm install
npm run dev          # http://localhost:5173
```

`Frontend/.env` ya está versionado (sin secretos) y apunta al gateway real:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_USE_MOCK=false
```

- `VITE_USE_MOCK=false` (default): **todo** contra el backend. Los flujos sin API se ocultan o deshabilitan desde `Frontend/src/config/featureGates.js`. No se mezcla mock y backend.
- `VITE_USE_MOCK=true`: **todo** en mock local (localStorage), sin backend; solo para demo offline.

Más detalle: [`Frontend/README.md`](Frontend/README.md).

## Tests

Cada módulo se compila y prueba de forma independiente. Dentro del módulo:

```bash
mvn test                              # unitarios
mvn test -Dtest=ClaseTest             # una clase puntual
mvn -Pit verify                       # integración con Testcontainers (requiere Docker)
```

El perfil `it` está definido en los **6 microservicios**: `mvn -Pit verify` ejecuta las ITs (`*IT.java`) contra MySQL/Mongo/Redis/Rabbit reales con Testcontainers, y `mvn test` corre solo los unitarios sin Docker. **No corras varios Maven en paralelo** sobre el mismo `~/.m2`.

Escenarios E2E (F1–F5) contra el stack completo, hablando solo con el gateway:

```bash
cd Backend
./scripts/e2e.sh              # limpia volúmenes, levanta, corre -Pe2e verify y limpia
./scripts/e2e.sh --keep       # deja el stack levantado para inspeccionar
```

Frontend:

```bash
cd Frontend
npm run lint
npm run build
```

Guías: [`Backend/docs/testing/`](Backend/docs/testing/) (concurrencia, notificaciones, integración entre microservicios y pruebas de carga con JMeter).

## Documentación

| Qué | Dónde |
| --- | --- |
| Consigna, backlog, reglas de negocio y decisiones del equipo | [`Backend/specs/`](Backend/specs/) |
| Flujo Spec-Driven (propuesta → requisitos → diseño → tareas → código) | [`Backend/specs/README.md`](Backend/specs/README.md) |
| Entrega formal: arquitectura, modelo de datos, observabilidad, OpenAPI, Postman y testing | [`Backend/docs/`](Backend/docs/README.md) |
| Rutas, rutas públicas y matriz RBAC del gateway | [`Backend/docs/api/gateway.md`](Backend/docs/api/gateway.md) |
| Contrato de cada microservicio | `Backend/<msvc>/docs/endpoints.md` |
| Detalle de ejecución del backend | [`Backend/README.md`](Backend/README.md) |
| Detalle del cliente React | [`Frontend/README.md`](Frontend/README.md) |
| User stories | [Tablero Trello](https://trello.com/b/HhT3lZdK/us-tpe) |

## Troubleshooting

- **Cambios de esquema o seeds:** los scripts solo corren con volúmenes nuevos. Recreá con `docker compose down -v && docker compose up -d --build`.
- **RabbitMQ tarda en crear las colas:** puede necesitar 2–3 minutos; hasta entonces no llegan notificaciones.
- **Puerto 80 ocupado:** el frontend sigue disponible en `:3000`. Podés quitar el mapeo de `Frontend/docker-compose.yml` o levantar solo `ms-frontend`.
- **401 en rutas privadas:** es el comportamiento esperado si falta el token o venció.
- **Ver logs de un servicio:** `docker compose logs -f ms-gateway` (o el contenedor que necesites).
- **MailHog:** la bandeja web debe decir *Connected*; todos los emails de prueba llegan ahí.

