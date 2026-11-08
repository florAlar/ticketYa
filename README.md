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

## Frontend + gateway (local)

El browser **no** llama a los msvc directos. Solo al gateway.

### 1. Gateway (y opcionalmente el resto del BE)

```bash
cd Backend
docker compose up -d --build
```

O solo el gateway si ya tenés apps a mano:

```bash
cd Backend/APIGateway
# spring-boot:run o su docker compose
```

Smoke: `curl -s http://localhost:8080/` → `api gateway ok!` (scaffold actual).

Detalle BE: [`Backend/README.md`](Backend/README.md).

### 2. Cliente React

Dev local:

```bash
cd Frontend
cp .env.example .env   # si no existe .env
npm install
npm run dev
```

O con Docker (Nginx :3000, incluido en compose del Backend):

```bash
cd Backend
docker compose up -d --build ms-frontend
```

Env relevante (`.env.example`):

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_USE_MOCK=true
```

- `VITE_USE_MOCK=true` (default): datos mock en el browser; útil sin dominio BE.
- `VITE_USE_MOCK=false`: las facades pegan al gateway (`VITE_API_BASE_URL`). Requiere endpoints reales detrás del gateway.

Más detalle: [`Frontend/README.md`](Frontend/README.md).

## Backend (resumen)

```bash
cd Backend
docker compose up -d --build
```

Puertos apps `8080`–`8086`. Ver `Backend/specs/infraestructura.md` y `Backend/README.md`.

## Notas

- No subir `.env` real (está en `.gitignore`).
- Dominio de negocio y rutas de API aún en construcción; el smoke `GET /` solo confirma que el proceso arranca.
