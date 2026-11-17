# TicketYa — cliente React (simulacro)

SPA del front cliente TicketYa. El browser habla **solo** con el API Gateway (`http://localhost:8080`).

Contrato UI ↔ backend: [`docs/endpoints-front.md`](docs/endpoints-front.md) (mantener al cruzar con el monorepo `ticketYa`).

## Stack

- React 19 + Vite + React Router (`BrowserRouter`)
- Auth/roles en Context; datos vía facades `src/api/*` con paths de `docs/endpoints-back.md`.
- Roles JWT: `CLIENTE` | `ORGANIZADOR` | `ADMIN` | `PERSONAL_ACCESO`
- Un solo interruptor de mock: `VITE_USE_MOCK` (ver `.env`); no se mezcla mock y backend

## Scripts

```bash
npm install
npm run dev
npm run build
npm run lint
```

## Env

`Frontend/.env` está versionado (sin secretos): no hay que copiar nada. Requiere el backend corriendo (`cd Backend && docker compose up -d --build`).

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_USE_MOCK=false
```

- `VITE_USE_MOCK=false` (default): **todo** contra el gateway real. Las funciones que el backend aún no implementa (transferencia, escaneo QR, reportes, preferencias, alta/baja de personal de acceso, solicitud de organizador, reintegro por entrada) aparecen deshabilitadas (`src/config/featureGates.js`).
- `VITE_USE_MOCK=true`: **todo** en mock local (localStorage), sin backend; solo para demo offline.

## Docker (imagen Nginx)

Multi-stage en `Frontend/Dockerfile`. Incluido en el compose del Backend como `ms-frontend` (:3000).

```bash
cd Backend
docker compose up -d --build ms-frontend
# o stack completo:
docker compose up -d --build
```

- UI: http://localhost:3000
- Nginx proxy `/api/` → `ms-gateway:8080` (rutas de negocio del gateway = API-05, aún pendientes)

## Estructura `src/`

```
src/
  api/           # facades + mock/* (LS) — misma forma que el gateway
  auth/          # roles CLIENTE | ORGANIZADOR | ADMIN | ACCESO + helpers de demo
  components/    # layout (Header/Footer), home (Hero, EventList/cards), auth, ui
  config/        # site.js (brand/footer), theme.js (tokens light/dark), featureGates
  context/       # Auth, Site, Theme
  domain/        # shapes + reglas (evento/ dividido en constants, sectores, mappers, disponibilidad)
  hooks/         # useAuth, useSite, useTheme, useAsync
  lib/           # formateadores compartidos (fechas, moneda)
  pages/         # pantallas por ruta + hooks/componentes locales de cada flujo
  providers/     # Site → Theme → Auth
  routes/        # AppRoutes + ProtectedRoute (única fuente de rutas)
  styles/        # global.css (índice) → base, layout, components, header, footer
```

Rutas vivas: solo `src/routes/AppRoutes.jsx` (Home, eventos, checkout, mis-entradas, soporte, gestión, acceso, login, 404).

## Capas de datos y estados

- `src/api/http.js` centraliza `fetch`: timeout, `AbortSignal`, `ApiError` uniforme y evento global `ticketya:auth-expired` ante 401. `AuthProvider` escucha ese evento y limpia la sesión.
- `src/hooks/useAsync.js` resuelve carga/error/cancelación/recarga y evita condiciones de carrera cuando cambian los parámetros de la ruta. Las pantallas ya no repiten `useEffect + setLoading`.
- `src/providers/AuthProvider.jsx` rehidrata la sesión con `GET /usuarios/me` antes de habilitar rutas protegidas.
- `src/lib/format.js` es la única fuente para fecha/hora y moneda (`formatFecha`, `formatMoney`).
- `src/components/ui/AsyncState.jsx` y `ErrorBoundary.jsx` estandarizan estados de carga, error, vacío y la red de seguridad de render.
- Las rutas usan `React.lazy` + `Suspense`, y las imágenes de `src/assets/img` se sirven en WebP para reducir el peso inicial.


## Capas que se conservan a propósito

- `EventCardCompact` + key `compact` en el registry → home por preferencias (msvc futuro).
- Shells `/soporte/transferencia` y `/acceso/scan` → UI lista; API real al cablear gateway.
- `docs/endpoints-front.md` → contrato del cruce back (auth, eventos, ventas, entradas, preferencias, …).

## Cuentas demo (mock)

Ver `DEMO_ACCOUNTS` / toast en `/login` (solo con mock).
