# Endpoints que el front TicketYa usa (gateway :8080)

Contrato **alineado a** [`endpoints-back.md`](./endpoints-back.md) (mandatorio).
El browser solo habla con el API Gateway. Paths con prefijo `/api`.

Auth: `Authorization: Bearer <JWT>` salvo rutas públicas.
Headers opcionales de back: `Idempotency-Key`, `X-Correlation-Id` (el http client ya permite extras).

Mock: `VITE_USE_MOCK !== 'false'` → LS local; **misma forma de llamada** en facades `src/api/*`.
Real: `VITE_USE_MOCK=false` → `src/api/http.js` + `VITE_API_BASE_URL`.

Roles JWT (canónicos back):

`CLIENTE` | `ORGANIZADOR` | `ADMIN` | `PERSONAL_ACCESO`

El front acepta alias legado `ACCESO` y lo normaliza a `PERSONAL_ACCESO` (`src/auth/roles.js`).

---

## 0. Auth — msvc-usuarios

| Método | Path | Auth | Front |
|---|---|---|---|
| POST | `/api/auth/login` | público | LoginForm |
| POST | `/api/auth/register` | público (solo CLIENTE) | RegisterForm |
| POST | `/api/auth/refresh` | JWT | `auth.refreshSession` (listo) |
| POST | `/api/auth/logout` | JWT | `auth.logout` (listo) |
| GET | `/api/usuarios/me` | JWT | `auth.getMe` (listo) |
| PATCH | `/api/usuarios/me` | JWT | `auth.patchMe` (listo) |

---

## 1. Usuarios admin — msvc-usuarios

| Método | Path | Auth | Front |
|---|---|---|---|
| GET | `/api/usuarios` | ADMIN | `/gestion/usuarios` |
| GET | `/api/usuarios/{id}` | ADMIN | `getUsuario` |
| PATCH | `/api/usuarios/{id}/estado` | ADMIN | activo on/off |
| GET | `/api/usuarios/{id}/roles` | ADMIN | `getUsuarioRoles` |
| PUT | `/api/usuarios/{id}/roles` | ADMIN | cambio de rol |

**Solo mock (no están en back aún):** alta personal acceso + `eventosAsignados` + delete usuario.
En real esas facades responden 501 hasta que el BE los defina.

---

## 2. Eventos — msvc-eventos (+ solapes gateway)

| Método | Path | Auth | Front |
|---|---|---|---|
| GET | `/api/eventos` | público | Home, `/eventos`; admin gestión (listado amplio) |
| GET | `/api/eventos/{id}` | público | detalle / checkout |
| POST | `/api/eventos` | ORG | `/gestion/eventos/nuevo` |
| PUT | `/api/eventos/{id}` | ORG | `updateEvento` (listo) |
| DELETE | `/api/eventos/{id}` | ORG | `deleteEvento` (listo) |
| POST | `/api/eventos/{id}/publicacion` | ORG | publicar (UI “Publicar”) |
| PATCH | `/api/eventos/{id}/estado` | ADMIN (suspender/cancelar) | botones estado |
| GET | `/api/organizadores/me/eventos` | ORG | listado gestión org |
| GET | `/api/eventos/{id}/disponibilidad` | — | `getDisponibilidadEvento` (msvc-ventas vía gateway) |

Query catálogo (back): `tipo`, `ciudad`, `fechaDesde`, `fechaHasta`, `participante`, `organizador`, `precioMin`, `precioMax`, `orden`, `page`, `size`.
El front mapea `sort`→`orden`, `limit`→`size` al salir a red.

---

## 3. Ventas — msvc-ventas

| Método | Path | Auth | Front |
|---|---|---|---|
| POST | `/api/compras` | JWT | checkout (`comprarEntrada`; mock 1 paso) |
| GET | `/api/compras` | JWT | `listMisCompras` |
| GET | `/api/compras/{id}` | JWT | `getCompra` |
| POST | `/api/reservas` | JWT | `crearReserva` (listo; checkout mock aún no la usa) |

Flujo back completo: reserva → compra (+ Idempotency-Key) → pagos. El simulacro mock sigue en un POST.

**No en back (solo mock UI):** cola compuesta admin de cancelados/reintegros manuales.
Admin pendientes reales: `GET /api/devoluciones`.

---

## 4. Entradas + validación + devoluciones — msvc-entradas

| Método | Path | Auth | Front |
|---|---|---|---|
| GET | `/api/entradas` | JWT (propias) | `/mis-entradas` |
| GET | `/api/entradas/{id}` | JWT | detalle |
| GET | `/api/entradas/{id}/qr` | JWT | `getEntradaQr` |
| POST | `/api/eventos/{idEvento}/validaciones` | PERSONAL_ACCESO\|ADMIN | `/acceso/scan` |
| GET | `/api/eventos/{idEvento}/validaciones` | ORG\|ADMIN | `listValidacionesEvento` |
| POST | `/api/entradas/{id}/devoluciones` | CLIENTE | soporte / detalle entrada |
| GET | `/api/devoluciones` | CLIENTE filtrado / ADMIN pendientes | mis solicitudes + cola admin |
| GET | `/api/devoluciones/{id}` | JWT | `getDevolucion` |
| POST | `/api/devoluciones/{id}/aprobacion` | ADMIN | `/gestion/ventas` |
| POST | `/api/devoluciones/{id}/rechazo` | ADMIN | `/gestion/ventas` |

---

## 5. Preferencias / notificaciones (back sí; UI no)

Ver `endpoints-back.md` §§ preferencias y notificaciones.
Sin facades ni pantallas todavía (home fijo; notifs vía broker).

---

## 6. Mapa UI → path back

| Ruta front | Endpoints |
|---|---|
| `/login` | POST `/api/auth/login`, `/api/auth/register` |
| `/` `/eventos` | GET `/api/eventos` |
| `/eventos/:id` | GET `/api/eventos/{id}` |
| `/eventos/:id/comprar` | POST `/api/compras` (+ futuro reservas/disponibilidad) |
| `/mis-entradas` | GET `/api/entradas` |
| `/mis-entradas/:id` | GET `/api/entradas/{id}` |
| `/soporte/devolucion` | POST `/api/entradas/{id}/devoluciones` |
| `/soporte/transferencia` | **no está en back** — shell UI |
| `/gestion/eventos` | GET `/api/organizadores/me/eventos` o GET `/api/eventos` |
| `/gestion/eventos/nuevo` | POST `/api/eventos` |
| publicar / estado | POST `.../publicacion` · PATCH `.../estado` |
| `/gestion/usuarios` | GET `/api/usuarios` · PATCH `.../estado` · PUT `.../roles` |
| `/gestion/ventas` | GET `/api/devoluciones` · POST `.../aprobacion|rechazo` |
| `/acceso/scan` | POST `/api/eventos/{idEvento}/validaciones` |

---

## 7. Facades `src/api`

| Archivo | Dominio |
|---|---|
| `auth.js` | login, register, refresh, logout, me |
| `eventos.js` | catálogo, detalle, gestión, create, publicar/estado, disponibilidad |
| `usuarios.js` | admin list/get/estado/roles (+ mock acceso) |
| `entradas.js` | list/detalle/qr/validar |
| `ventasCliente.js` | compras, reservas |
| `ventas.js` | cola admin UI + resolución devoluciones |
| `soporteCliente.js` | alta/listado devoluciones cliente |
| `http.js` | fetch gateway |
| `mock/*` | LS solo si `USE_MOCK` |

Fuente de verdad de paths de negocio: **`endpoints-back.md`**. Este archivo es la vista “qué usa el SPA”.
