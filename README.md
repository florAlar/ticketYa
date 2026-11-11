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
No hace falta crear ni copiar ningún `.env` para el backend (los valores por defecto están en los compose); solo si se quiere cambiar algo, copiar la raíz `.env.example` a `.env`. El `.env` del frontend ya está versionado en `Frontend/.env`.

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
- Hay un único `Backend/infrastructure/mysql/*/init.sql` por base y solo corre en el primer arranque de cada volumen. Si cambia el esquema (por ejemplo al traer cambios nuevos del repo), recrear las bases con `docker compose down -v && docker compose up -d --build`.

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

> **Limitación conocida de la Entrega 1:** con `TICKETYA_JWT_ENABLED=false`, un request **sin** `Authorization` se acepta con los headers `X-User-Id` / `X-User-Role` que mande el cliente, sin verificarlos: cualquiera puede declararse ADMIN. Es una decisión de la entrega (identificación por encabezado), no una garantía de seguridad. Con un `Bearer` presente el token siempre se valida (firma, expiración, roles) y pisa los headers del cliente. Con `TICKETYA_JWT_ENABLED=true` (Entrega 2) el JWT es obligatorio.
>
> El login real (`POST /api/auth/login`) emite un JWT en ambos modos, así que el front funciona sin mock. Que gateway y `msvc-usuarios` compartan el mismo `JWT_SECRET` lo verifica `JwtGatewayIntegrationTest`.

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
