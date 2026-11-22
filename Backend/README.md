# TicketYa — Backend

Backend del TP de venta de entradas: **API Gateway + 6 microservicios Spring Boot** (Java 17) con MySQL, MongoDB, Redis, RabbitMQ y MailHog, orquestados con Docker Compose.

- El navegador habla **solo con el gateway** (`:8080`); el gateway valida JWT, aplica RBAC (Control de Acceso Basado en Roles) y enruta a cada microservicio.
- Cada microservicio es un proyecto Maven independiente, con **su propia base de datos** y su propio `docker-compose.yml`.
- Specs de trabajo del equipo: [`specs/`](specs/). Documentación formal de entrega: [`docs/`](docs/).

---

## Índice

- [Estructura](#estructura)
- [Requisitos](#requisitos)
- [Inicio rápido](#inicio-rápido)
- [Servicios y puertos](#servicios-y-puertos)
- [Variables de entorno](#variables-de-entorno)
- [Scripts útiles - Solo Testing](#scripts-útiles---solo-testing)
- [Autenticación](#autenticación)
- [Preferencias, historial y recomendaciones](#preferencias-historial-y-recomendaciones-e2-pre)
- [Correr un microservicio en local](#correr-un-microservicio-en-local)
- [Tests](#tests)
- [Documentación](#documentación)
- [Troubleshooting](#troubleshooting)

---

## Estructura

```text
Backend/
├── APIGateway/              # Gateway (Spring Cloud Gateway MVC) — sin DB
├── msvc-usuarios/           # Identidad, roles y auth (MySQL)
├── msvc-eventos/            # Eventos, lugares, sectores, participantes (MySQL + RabbitMQ)
├── msvc-ventas/             # Reservas, compras, pagos, reportes, fila virtual (MySQL + Redis)
├── msvc-entradas/           # Emisión/QR, devoluciones, control de acceso (MySQL)
├── msvc-preferencias/       # Preferencias, historial y recomendaciones (MongoDB)
├── msvc-notificaciones/     # Email/push y plantillas (MySQL + RabbitMQ + MailHog)
├── infrastructure/          # Esquemas, seeds y RabbitMQ compartido
├── scripts/                 # Smoke, estado, backfill y seeds
├── specs/                   # Documentación viva del equipo
├── docs/                    # Entrega formal (arquitectura, OpenAPI, Postman, testing)
└── docker-compose.yml       # Stack completo (include de todos los compose)
```

Cada microservicio respeta las mismas capas: `controller`, `service`, `repository`, `entity|document`, `dto`, `mapper`, `clients`, `exception`, `config`.

---

## Requisitos

| Herramienta | Versión / nota |
| --- | --- |
| JDK | 17+ |
| Maven | 3.9+ (`mvn` en Linux/macOS, `mvn.cmd` en Windows) |
| Docker | Docker Engine + Compose v2 (`docker compose`) |
| `curl` / `jq` | Opcionales, para los smoke tests |

Puertos usados por el stack completo: `80` y `3000` (frontend), `8080`–`8086`, `3307`–`3311`, `5672`, `6380`, `8025`, `1025`, `15672`, `27018`. Si `80` está ocupado, el frontend sigue disponible en `:3000` (ajustá el mapeo del compose o levantá solo `ms-frontend`).

> No hace falta crear un `.env`: los compose traen valores por defecto de desarrollo. Solo si querés cambiar algo, copiá `.env.example` (raíz del monorepo) a `.env`.

---

## Inicio rápido

```bash
cd Backend
docker compose up -d --build
```

El primer build tarda unos minutos. Levanta gateway, 6 microservicios, frontend (Nginx), 5 MySQL, MongoDB, Redis, RabbitMQ y MailHog.

Verificar que todo esté arriba:

```bash
sudo docker compose ps
sudo ./scripts/status.sh --http      # tabla de estado + smoke HTTP por servicio
```

Smoke manual:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3000/      # frontend → 200
curl -s http://localhost:8080/api/eventos                            # gateway → 200 con eventos (ruta pública)
```

Bajar el stack:

```bash
docker compose down         # conserva las bases
docker compose down -v      # borra las bases (re-aplica esquemas y seeds)
```

> **Cambios de esquema/seeds:** los `init.sql` / `init.js` de `infrastructure/` solo corren sobre volúmenes nuevos. Si traés cambios que tocan la base, recreá con `docker compose down -v && docker compose up -d --build`.

---

## Servicios y puertos

### Aplicaciones

| Puerto | Servicio | Contenedor | Persistencia | Responsabilidad |
| ---: | --- | --- | --- | --- |
| **8080** | `APIGateway` | `ms-gateway` | — | Único punto de entrada; JWT + RBAC + enrutado |
| **8081** | `msvc-usuarios` | `ms-usuarios` | MySQL `3307` | Identidad, roles, credenciales y auth |
| **8082** | `msvc-eventos` | `ms-eventos` | MySQL `3308` | Eventos, lugares, sectores, participantes, búsqueda |
| **8083** | `msvc-ventas` | `ms-ventas` | MySQL `3309` + Redis `6380` | Disponibilidad, reservas, compras, pagos, reportes, fila virtual |
| **8084** | `msvc-entradas` | `ms-entradas` | MySQL `3310` | Emisión/QR, devoluciones, validación de acceso |
| **8085** | `msvc-preferencias` | `ms-preferencias` | MongoDB `27018` | Preferencias, historial y recomendaciones |
| **8086** | `msvc-notificaciones` | `ms-notificaciones` | MySQL `3311` | Envíos email/push, plantillas y estado |
| **3000** | Frontend (Nginx) | `ms-frontend` | — | UI; también `http://localhost` (`:80`) |
| **5173** | Vite dev server | — | — | Alternativa al frontend Docker (`npm run dev`) |

### Infraestructura

| Puerto(s) | Servicio | Uso |
| --- | --- | --- |
| `5672` / `15672` | RabbitMQ | Eventos de dominio; panel de administración (`ticketya` / `ticketya_dev`) |
| `1025` / `8025` | MailHog | SMTP de prueba y bandeja web (`http://localhost:8025`) |
| `3307`–`3311` | MySQL 8.4 | Una instancia por microservicio con MySQL |
| `27018` | MongoDB 7.0 | `msvc-preferencias` (interno `27017`) |
| `6380` | Redis 7 | `msvc-ventas` (interno `6379`) |

Flujo de red: **Navegador → `:80`/`:3000` (Nginx) → `:8080` (gateway) → `:8081`–`:8086` (msvc)**. Entre contenedores se usan nombres de servicio (`ms-eventos:8082`, etc.).

Detalle de enrutado y matriz de permisos: [`docs/api/gateway.md`](docs/api/gateway.md).

---

## Variables de entorno

Todas son opcionales en desarrollo; los compose aplican estos valores por defecto.

| Variable | Default | Uso |
| --- | --- | --- |
| `MYSQL_PASSWORD` | `ticketya_dev` | Password del usuario `ticketya` en las 5 MySQL |
| `MYSQL_ROOT_PASSWORD` | `root_dev` | Password de `root` en MySQL |
| `MONGO_PASSWORD` | `mongo_dev` | Password de MongoDB |
| `RABBITMQ_PASSWORD` | `ticketya_dev` | Password del usuario `ticketya` en RabbitMQ |
| `JWT_SECRET` | secreto de desarrollo | **Compartido** entre gateway y `msvc-usuarios` |
| `TICKETYA_INTERNAL_TOKEN` | `ticketya_dev_internal` | Sella llamadas internas (fila virtual y backfill) |
| `TICKETYA_FILA_PREFIX` | `fila` | Prefijo de las claves Redis de la fila virtual |
| `TICKETYA_FILA_TTL_HOURS` | `24` | TTL de las claves de la fila |
| `TICKETYA_FILA_TOKEN_TTL_MINUTES` | `10` | TTL del token de compra |
| `TICKETYA_FILA_AUTO_ENABLED` | `true` | Job automático de tandas; `false` para B2/manual |
| `TICKETYA_FILA_AUTO_TAMANIO` | `25` | Posiciones que abre cada tick automático |
| `TICKETYA_FILA_AUTO_INTERVALO_MS` | `10000` | Frecuencia del job de tandas |
| `TICKETYA_NOTIF_ENVIO_INTERVALO_MS` | `30000` | Frecuencia del worker de notificaciones |

> Si rotás `JWT_SECRET`, usá el mismo valor en gateway y usuarios; si no, el gateway rechaza todos los tokens.

---

## Scripts útiles - Solo Testing

Los de `scripts/` se ejecutan desde `Backend/`:

| Script | Qué hace |
| --- | --- |
| `scripts/status.sh` | Estado de contenedores. Flags: `--http` (smoke HTTP), `--watch [seg]` (refresco) |
| `scripts/e2e.sh` | Escenarios E2E F1–F5 contra el stack completo (build, bases `healthy`, gateway + servicios, verify y limpieza). `--keep` deja el entorno |
| `scripts/up-e2e.sh` | Levanta lo mínimo para probar eventos → RabbitMQ → notificaciones (`--down` para bajar) |
| `scripts/test-rabbitmq.sh` | Prueba aislada de `ms-eventos` + `ms-notificaciones` |
| `scripts/backfill-preferencias.sh` | Importa compras aprobadas de ventas al historial (idempotente) |
| `scripts/seed-preferencias.sh` | Aplica índices y seed de co-compra sobre un volumen Mongo existente |

```bash
./scripts/status.sh --http
./scripts/status.sh --watch 5
./scripts/backfill-preferencias.sh
```

### Pruebas de carga (`docs/testing/carga/`)

Requieren el stack arriba, **JMeter 5.x** (probado con 5.6.3; el 2.13 de apt no sirve), `curl` y `jq`. Se ejecutan **desde su propia carpeta**:

| Script | Qué hace |
| --- | --- |
| `generar-usuarios.sh [cantidad] [desde]` | Registra (si no existen) las cuentas `cargaNN@ejemplo.local`, hace login y escribe `usuarios.csv` con `email,token`. Los JWT vencen en 1 h. |
| `jmeter.sh [plan] [usuarios]` | Regenera el CSV y corre el plan indicado; deja `corrida.jtl` y el reporte HTML en `report/`. |

```bash
cd Backend/docs/testing/carga
./jmeter.sh compra-con-fila.jmx 25     # con fila virtual (default)
./jmeter.sh compra-normal.jmx 50       # compra sin fila
```

| Plan | Flujo |
| --- | --- |
| `compra-normal.jmx` | Compra sin fila (evento no de alta demanda) |
| `compra-con-fila.jmx` | Ingreso → poll de posición → validación → reserva → compra → pago → verificación |

Guía completa y parámetros (`-Jusuarios`, `-JidEvento`, `-JadminToken`, etc.): [`docs/testing/carga/README.md`](docs/testing/carga/README.md) y [`docs/testing/carga.md`](docs/testing/carga.md).

---

## Autenticación

El gateway exige **JWT + roles** (sin flag). Fuera de las rutas públicas, un request sin `Authorization: Bearer <token>` recibe **401**.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@ticketya.com","password":"demo1234"}' | jq -r .token)

curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/entradas   # ruta privada → 200
```

- Usuario demo: `demo@ticketya.com` / `demo1234` (CLIENTE). Otras cuentas: `DEMO_ACCOUNTS` en `Frontend/src/auth/roles.js`.
- Roles: `CLIENTE`, `ORGANIZADOR`, `PERSONAL_ACCESO`, `ADMIN`.
- Los headers `X-User-Id` / `X-User-Role` que envíe el cliente **nunca se confían**: el gateway los sobreescribe con los claims del token y los vacía en rutas públicas.
- Rutas públicas: `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/eventos`, `GET /api/eventos/{id}`, `GET /api/participantes`, `GET /api/eventos/{id}/disponibilidad`, `GET /api/eventos/{id}/recomendaciones`, `GET /api/eventos/{id}/fila/estado`.

---

## Preferencias, historial y recomendaciones (E2-PRE)

Con el stack arriba y el `TOKEN` de la sección anterior:

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

- La ingesta es asíncrona: al aprobar una compra, `compra.confirmada` llega a la cola `preferencias.compras` y se guarda en `interacciones_eventos` (idempotente por `idCompra`).
- Compras viejas del seed de MySQL: `./scripts/backfill-preferencias.sh` (idempotente).
- Volumen de Mongo ya creado: `./scripts/seed-preferencias.sh` aplica índices y seed de co-compra.
- Detalle del algoritmo: [`msvc-preferencias/docs/recomendaciones.md`](msvc-preferencias/docs/recomendaciones.md).
- Swagger del microservicio: `http://localhost:8085/swagger-ui/index.html` (spec en [`docs/openapi/msvc-preferencias.json`](docs/openapi/msvc-preferencias.json)).
- En el frontend: Home “Recomendados para vos”, detalle “Eventos similares” y perfil “Mis preferencias” + “Mi historial”.

---

## Correr un microservicio en local

Cada microservicio puede levantarse solo (app + su DB):

```bash
cd Backend/msvc-eventos
docker compose up -d --build
```

O ejecutarlo con Maven, levantando antes solo su infraestructura:

```bash
cd Backend/msvc-eventos
docker compose up -d db-mysql-eventos
mvn -DskipTests spring-boot:run          # mvn.cmd en Windows
```

Ejemplos según dependencias:

| Microservicio | Infra previa |
| --- | --- |
| `msvc-usuarios` | `db-mysql-usuarios` |
| `msvc-eventos` | `db-mysql-eventos` + `rabbitmq` |
| `msvc-ventas` | `db-mysql-ventas` + `db-redis-ventas` + `rabbitmq` |
| `msvc-entradas` | `db-mysql-entradas` + `rabbitmq` |
| `msvc-preferencias` | `db-mongo-preferencias` + `rabbitmq` |
| `msvc-notificaciones` | `db-mysql-notificaciones` + `rabbitmq` + `mailhog` |

---

## Tests

Dentro del módulo correspondiente:

```bash
mvn test                              # unitarios
mvn test -Dtest=ClaseTest             # una clase puntual
mvn -Pit verify                       # ITs de borde con Testcontainers (requiere Docker)
```

El perfil `it` está definido en los **6 microservicios**: `mvn -Pit verify` ejecuta las ITs (`*IT.java`) contra MySQL/Mongo/Redis/Rabbit reales con Testcontainers; `mvn test` corre solo los unitarios sin Docker.

Escenarios E2E (gateway + 6 microservicios + bases reales, sin levantar contextos Spring):

```bash
cd Backend
./scripts/e2e.sh              # F1–F5: limpia volúmenes, levanta el stack, verifica y limpia
./scripts/e2e.sh --keep       # deja el stack levantado para inspeccionar
```

Guía y resultados: [`docs/testing/integracion-msvc.md`](docs/testing/integracion-msvc.md).

> No ejecutes varios `mvn` en paralelo sobre el mismo `~/.m2`.

Informe de concurrencia: [`docs/testing/concurrencia.md`](docs/testing/concurrencia.md).

---

## Documentación

### Documentación formal (`docs/`)

| Archivo | Contenido |
| --- | --- |
| [`docs/README.md`](docs/README.md) | Índice de la entrega |
| [`docs/arquitectura.md`](docs/arquitectura.md) | Arquitectura, módulos y comunicación |
| [`docs/modelo-datos.md`](docs/modelo-datos.md) | Modelo de datos por servicio |
| [`docs/observabilidad.md`](docs/observabilidad.md) | Correlation id y bitácora |
| [`docs/api/gateway.md`](docs/api/gateway.md) | Rutas, rutas públicas y matriz RBAC |
| [`docs/openapi/`](docs/openapi/) | OpenAPI por microservicio + bundle |
| [`docs/postman/`](docs/postman/) | Colección Postman + environment |
| [`docs/testing/`](docs/testing/) | Concurrencia, notificaciones y carga |

### Specs de trabajo (`specs/`)

El flujo completo (propuesta → requisitos → diseño → tareas → implementación, DoD y checklists) está
en [`specs/README.md`](specs/README.md).

| Archivo | Contenido |
| --- | --- |
| [`specs/README.md`](specs/README.md) | Flujo Spec-Driven, convenciones, DoD y checklists |
| [`specs/consigna.md`](specs/consigna.md) | Enunciado del TP |
| [`specs/backlog.md`](specs/backlog.md) | Tareas y estado E1/E2 |
| [`specs/reglas-de-negocio.md`](specs/reglas-de-negocio.md) | Reglas y decisiones pendientes |
| [`specs/endpoints.md`](specs/endpoints.md) | Rutas y enrutado |
| [`specs/microservicios.md`](specs/microservicios.md) | Mapa de servicios |
| [`specs/comunicacion.md`](specs/comunicacion.md) | Cliente ↔ gateway ↔ msvc |
| [`specs/infraestructura.md`](specs/infraestructura.md) | Docker y puertos |
| [`specs/bases-de-datos.md`](specs/bases-de-datos.md) | Esquemas y seeds |
| [`specs/diagramas/modelo.md`](specs/diagramas/modelo.md) | ER global |

### Contrato por microservicio

Cada servicio documenta sus endpoints en `<msvc>/docs/endpoints.md`; el gateway en [`APIGateway/docs/endpoints.md`](APIGateway/docs/endpoints.md).

Frontend: ver [`README.md` raíz](../README.md) y [`Frontend/README.md`](../Frontend/README.md).

---

## Troubleshooting

### El stack tarda en quedar operativo

- **RabbitMQ** puede tardar **2–3 minutos** en crear las colas. Hasta entonces no llegan notificaciones; esperá y reintentá.
- **MongoDB** tarda unos minutos en cargar el seed completo. Si `preferencias` responde raro al principio, esperá o corré `./scripts/seed-preferencias.sh`.

### MailHog

- Bandeja web: `http://localhost:8025`. Verificá que diga **Connected**.
- Es un simulador SMTP: cualquier email registrado en el sistema recibe sus notificaciones ahí. No hace falta configurar nada más.

### El frontend no llega al backend

- El browser debe apuntar al gateway (`VITE_API_BASE_URL=http://localhost:8080`). Si usás Vite (`:5173`), el backend tiene que estar arriba.
- Rutas privadas sin token devuelven **401**: es el comportamiento esperado.

### Infra

- Los scripts de `infrastructure/` solo corren con volúmenes nuevos:
  ```bash
  docker compose down -v && docker compose up -d --build
  ```

### Docker no responde

- Verificá el daemon y permisos sobre `/var/run/docker.sock`:
  ```bash
  sudo systemctl start docker
  sudo usermod -aG docker "$USER"   # luego reabrí la sesión
  ```
- `./scripts/status.sh` devuelve código `2` si Docker no está disponible.

### Puerto ocupado

- El frontend se publica en `:80` y `:3000`. Si `:80` está ocupado, quitá ese mapeo de `Frontend/docker-compose.yml` (el frontend sigue en `:3000`) o levantá solo `ms-frontend`.
- Para cualquier otro puerto del stack, pará el proceso que lo usa o ajustá el mapeo en el `docker-compose.yml` correspondiente.
