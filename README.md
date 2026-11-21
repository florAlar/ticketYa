# TicketYa

Monorepo del trabajo práctico: venta de entradas (microservicios + API Gateway + cliente React).

```text
ticketYa/
├── Backend/     # Spring Boot: gateway + msvcs + specs/ + docs/ + compose
├── Frontend/    # SPA React (Vite) — habla solo con el gateway :8080
├── .env.example
└── README.md
```

Specs de trabajo (consigna, backlog, reglas): `Backend/specs/`.  
Documentación formal de entrega: `Backend/docs/`.

## Levantar el stack completo (Docker)

Requisitos: Docker con Compose v2. Puertos libres: `80`, `3000`, `8080`–`8086`, `3307`–`3311`, `6380`,`8025`,`5672`,`15672` `27018`.
No hace falta crear ni copiar ningún `.env` para el backend (los valores por defecto están en los compose); solo si se quiere cambiar algo, copiar la raíz `.env.example` a `.env`. El `.env` del frontend ya está versionado en `Frontend/.env`.

```bash
cd Backend
docker compose up -d --build
```

El primer build tarda unos minutos. Levanta 17 contenedores: gateway, 6 microservicios, frontend (Nginx), 5 MySQL, MongoDB, Redis, RabbitMQ y MailHog.

Verificar:

```bash
docker compose ps                                   # todo Up; las bases (healthy)
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3000/        # frontend → 200
curl -s http://localhost:8080/api/eventos                              # gateway → 200 con eventos
```

- Frontend: http://localhost:3000 (y también http://localhost en el puerto 80, si está libre) — Gateway: http://localhost:8080 (el browser habla solo con el gateway).
- Bajar: `docker compose down`. Reiniciar desde cero (borra las bases): `docker compose down -v`.
- Hay un único `Backend/infrastructure/mysql/*/init.sql` por base y solo corre en el primer arranque de cada volumen. Si cambia el esquema (por ejemplo al traer cambios nuevos del repo), recrear las bases con `docker compose down -v && docker compose up -d --build`.

# Servicios del proyecto

## Servicios publicados

| Puerto | Qué lo usa | Quién lo consume |
|---|---|---|
| **8080** | `APIGateway` (ms-gateway) | El frontend (`VITE_API_BASE_URL`), Nginx del front (`/api/` → `ms-gateway:8080`), curl/Postman. Es el **único punto de entrada** al backend |
| **8081** | `msvc-usuarios` (ms-usuarios) | Gateway (rutas `auth`, `usuarios`);  |
| **8082** | `msvc-eventos` (ms-eventos) | Gateway (`eventos`, `lugares`, `sectores`, `participantes`, `organizadores`) |
| **8083** | `msvc-ventas` (ms-ventas) | Gateway (`reservas`, `compras`, `pagos`, `reportes`, disponibilidad, fila virtual) |
| **8084** | `msvc-entradas` (ms-entradas) | Gateway (`entradas`, `devoluciones`, validaciones QR) |
| **8085** | `msvc-preferencias` (ms-preferencias) | Gateway (`preferencias`, `recomendaciones`, `clientes`) |
| **8086** | `msvc-notificaciones` (ms-notificaciones) | Gateway (`notificaciones`) |
| **3000** | `ms-frontend` (Nginx) | El usuario/browser: http://localhost o  http://localhost:3000 |
| **5173** | Vite dev server (`npm run dev`) | Alternativa al front de Docker, solo en desarrollo local |
| **3307** | MySQL de usuarios | Solo `msvc-usuarios`  |
| **3308** | MySQL de eventos | Solo `msvc-eventos` |
| **3309** | MySQL de ventas | Solo `msvc-ventas` |
| **3310** | MySQL de entradas | Solo `msvc-entradas` |
| **3311** | MySQL de notificaciones | Solo `msvc-notificaciones` |
| **27018** | MongoDB de preferencias (27017 interno) | Solo `msvc-preferencias` |
| **6380** | Redis de ventas (6379 interno) | Solo `msvc-ventas` (fila virtual / locks) |
| **5672** | RabbitMQ (AMQP) | `msvc-eventos`, `msvc-ventas`, `msvc-entradas`, `msvc-preferencias`, `msvc-notificaciones` (publican/consumen en `eventos.exchange`) |
| **15672** | RabbitMQ Management UI | Interfaz web para inspeccionar colas (`ticketya` / `ticketya_dev`) |
| **1025** | MailHog SMTP | `msvc-notificaciones` (envío de emails de prueba) |
| **8025** | MailHog Web UI | Para ver los emails enviados |

## Puertos internos (solo dentro de la red Docker)

| Puerto | Servicio |
|---|---|
| `80` | Nginx del frontend (`ms-frontend:80`, mapeado a 3000) |
| `3306` | Las 5 instancias MySQL escuchan acá; cada una se publica en su puerto propio (3307–3311) |
| `27017` | MongoDB interno (publicado en 27018) |
| `6379` | Redis interno (publicado en 6380) |

## Resumen servicios

- **Entrada web:** `80` o `3000` (front Nginx) o `5173` (Vite dev) → **`8080`** (gateway) → **`8081–8086`** (msvc).
- **Datos:** `3307–3311` (MySQL por servicio), `27018` (Mongo), `6380` (Redis).
- **Infra compartida:** `5672`/`15672` (RabbitMQ), `1025`/`8025` (MailHog).


### Usuarios demo

| Rol | Email | Password |
|---|---|---|
| Cliente | `demo@ticketya.com` | `demo1234` |

Resto de cuentas (organizador, admin, personal de acceso): toast de `/login` o `DEMO_ACCOUNTS` en `Frontend/src/auth/roles.js`.

### Autenticación

El gateway exige **JWT + roles** (Entrega 2, sin flag): fuera de las rutas públicas, un request
sin `Authorization: Bearer <token>` se rechaza con **401**. Los headers `X-User-Id` / `X-User-Role`
que envíe el cliente **nunca se trustan**: el gateway los sobreescribe con los claims del token
verificado, y en rutas públicas los vacía.

Login y uso:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@ticketya.com","password":"demo1234"}' | jq -r .token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/entradas
```

Rutas públicas (sin token): `GET /api/eventos`, `GET /api/eventos/{id}`, `GET /api/participantes`,
`GET /api/eventos/{id}/disponibilidad`, `/api/auth/login`, `/api/auth/register`.

> Gateway y `msvc-usuarios` comparten el mismo `JWT_SECRET` (verificable con
> `JwtGatewayIntegrationTest`); si lo rotás, poné el mismo valor en los dos lados.

## Frontend (desarrollo local con `npm run dev`)

Alternativa al frontend de Docker (`:3000`) para desarrollar con recarga en caliente (`:5173`). **Necesita el backend corriendo** (`docker compose up -d --build` en `Backend/`, ver arriba).

```bash
cd Frontend          # npm run dev solo funciona dentro de Frontend/
npm install
npm run dev          # http://localhost:5173
```

`Frontend/.env` ya viene en el repo (sin secretos), no hay que copiar nada. Apunta al gateway real:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_USE_MOCK=false
```

- Es la misma configuración que usa la imagen Docker del frontend.
- **No se mezcla mock y backend.** Con `VITE_USE_MOCK=false` todo va contra el gateway y las funciones que el backend aún no implementa (transferencia, escaneo QR, reportes, preferencias, alta de personal de acceso, etc.) aparecen deshabilitadas. Con `VITE_USE_MOCK=true` todo el front corre en mock local, sin backend (solo demo offline).
- `Frontend/.env.example` es una copia de referencia del mismo contenido.

Más detalle: [`Frontend/README.md`](Frontend/README.md) y [`Backend/README.md`](Backend/README.md) (gateway y msvc por separado).

## Users Stories

- Tablero: [Acceder User Stories](https://trello.com/b/HhT3lZdK/us-tpe)

## Documentación de diseño

Arquitectura, diagramas de secuencia, modelo de datos y demás artefactos: [`Backend/docs/`](Backend/docs/README.md).

## Notas

- No subir `.env` real (está en `.gitignore`).
