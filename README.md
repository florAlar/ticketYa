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

Requisitos: Docker con Compose v2. Puertos libres: `3000`, `8080`–`8086`, `3307`–`3311`, `6380`, `27018`.
No hace falta `.env` (los valores por defecto están en los compose); si se quiere cambiar algo, copiar `.env.example` a `.env`.

```bash
cd Backend
docker compose up -d --build
```

El primer build tarda unos minutos. Levanta 15 contenedores: gateway, 6 msvc, frontend (Nginx) y sus bases (MySQL, MongoDB, Redis).

Verificar:

```bash
docker compose ps                                   # todo Up; las bases (healthy)
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3000/        # frontend → 200
curl -s http://localhost:8080/api/eventos                              # gateway → 200 con eventos
```

- Frontend: http://localhost:3000 — Gateway: http://localhost:8080 (el browser habla solo con el gateway).
- Bajar: `docker compose down`. Reiniciar desde cero (borra las bases): `docker compose down -v`.
- Los scripts SQL de `Backend/infrastructure/mysql/*/init.sql` solo corren en el primer arranque de cada volumen.

### Usuarios demo

| Rol | Email | Password |
|---|---|---|
| Cliente | `demo@ticketya.com` | `demo1234` |

Resto de cuentas (organizador, admin, personal de acceso): toast de `/login` o `DEMO_ACCOUNTS` en `Frontend/src/auth/roles.js`.

### Autenticación: Entrega 1 vs Entrega 2

El gateway tiene un flag `TICKETYA_JWT_ENABLED` (default `false`):

| Valor | Modo |
|---|---|
| `false` (Entrega 1) | Identificación por encabezado: sin `Authorization`, el request pasa con los headers `X-User-Id` / `X-User-Role` que envía el cliente. Un `Bearer` presente igual se valida. |
| `true` (Entrega 2) | JWT obligatorio y autorización por roles en el gateway. |

Ejemplo Entrega 1:

```bash
curl -H 'X-User-Id: b0000000-0000-4000-8000-000000000001' -H 'X-User-Role: CLIENTE' http://localhost:8080/api/entradas
```

Activar JWT: `TICKETYA_JWT_ENABLED=true docker compose up -d ms-gateway`.

## Frontend (desarrollo local)

```bash
cd Frontend
cp .env.example .env   # si no existe .env
npm install
npm run dev
```

Variables (`Frontend/.env.example`):

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_USE_MOCK=true
```

- En la imagen Docker, auth, eventos, compras y entradas van contra el backend real (`VITE_USE_MOCK_*=false`); el resto de módulos usa mock mientras `VITE_USE_MOCK=true`.
- `VITE_USE_MOCK=false`: todas las facades pegan al gateway (`VITE_API_BASE_URL`).

Más detalle: [`Frontend/README.md`](Frontend/README.md) y [`Backend/README.md`](Backend/README.md) (gateway y msvc por separado).

## Users Stories

- Tablero: [Acceder User Stories](https://trello.com/b/HhT3lZdK/us-tpe)
- Versión en el repo, con criterios de aceptación: [`Backend/docs/user-stories.md`](Backend/docs/user-stories.md)

## Documentación de diseño

Features y user stories, arquitectura, diagramas de secuencia y modelo de datos: [`Backend/docs/`](Backend/docs/README.md).

## Notas

- No subir `.env` real (está en `.gitignore`).
