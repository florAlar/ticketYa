# Endpoints que el front TicketYa usa (gateway :8080)

Contrato **alineado a** [`endpoints-back.md`](./endpoints-back.md) (mandatorio).
El browser solo habla con el API Gateway. Paths con prefijo `/api`.

Auth: `Authorization: Bearer <JWT>` salvo rutas públicas.
Headers opcionales de back: `Idempotency-Key`, `X-Correlation-Id` (el http client ya permite extras).

Mock: `VITE_USE_MOCK=true` → LS local (todo el front, sin backend); **misma forma de llamada** en facades `src/api/*`.
Real (default): `VITE_USE_MOCK=false` → `src/api/http.js` + `VITE_API_BASE_URL`.

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
| GET | `/api/usuarios/me` | JWT | `auth.getMe` → `/perfil` |
| PATCH | `/api/usuarios/me` | JWT | `auth.patchMe` → form Mi perfil |

---

## 1. Usuarios admin — msvc-usuarios

| Método | Path | Auth | Front |
|---|---|---|---|
| GET | `/api/usuarios` | ADMIN | `/gestion/usuarios` |
| GET | `/api/lugares`, `/api/lugares/{id}` | ORG \| ADMIN | selector de lugar y sectores al crear/editar un evento (`EventoLugarSectores`) |
| POST | `/api/lugares` | ADMIN | `/gestion/lugares/nuevo` — gate `FEATURE.lugares` (el organizador no crea lugares) |
| PUT | `/api/lugares/{id}` | ADMIN | `/gestion/lugares/:id` (formulario + sectores; el GET lo usa también el organizador) |
| DELETE | `/api/lugares/{id}` | ADMIN | `/gestion/lugares` — 409 si el lugar tiene eventos |
| POST | `/api/lugares/{id}/sectores` · PUT `/api/sectores/{id}` | ADMIN | `/gestion/lugares/:id` |
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
| PUT | `/api/eventos/{id}` | ORG dueño / ADMIN | `/gestion/eventos/:id/editar` (solo BORRADOR en UI) y body parcial `{esAltaDemanda}` desde el popup previo a publicar |
| DELETE | `/api/eventos/{id}` | ORG | `deleteEvento` (listo) |
| POST | `/api/eventos/{id}/publicacion` | ORG | publicar (UI “Publicar” / “Volver a publicar”), detrás del popup de alta demanda |
| PATCH | `/api/eventos/{id}/estado` | ORG suspende propios; ADMIN cancela | botones Suspender / Cancelar (admin); al salir de PUBLICADO se pausa la fila |
| GET | `/api/organizadores/me/eventos` | ORG | listado gestión org |
| GET | `/api/eventos/{id}/disponibilidad` | público | checkout + detalle (`getDisponibilidadEvento`) |
| GET | `/api/eventos/disponibilidad?ids=` | público | batch catálogo (`getDisponibilidadBatch` → badges home) |

Query catálogo (back): `tipo`, `ciudad`, `estado`, `fechaDesde`, `fechaHasta`, `participante`, `organizador`, `orden`, `page`, `size`.
El front mapea `sort`→`orden`, `limit`→`size` al salir a red.

---

## 3. Ventas — msvc-ventas

| Método | Path | Auth | Front |
|---|---|---|---|
| POST | `/api/compras` | JWT | checkout (`comprarEntrada`; mock 1 paso) |
| GET | `/api/compras` | JWT | `listMisCompras` |
| GET | `/api/compras/{id}` | JWT | `getCompra` |
| POST | `/api/reservas` | JWT | `crearReserva` (la usa `comprarEntrada` en modo gateway; en mock no) |
| GET | `/api/eventos/{id}/disponibilidad` | JWT | checkout: cupo + `asientosOcupados` del sector numerado |

Flujo back completo: reserva → compra (+ Idempotency-Key) → pagos. El simulacro mock sigue en un POST.

**Sectores numerados (asientos virtuales):** el checkout muestra `SeatPicker` (elegir fila → asientos, hasta 6). `comprarEntrada({ idEvento, idSector, asientos })` envía `items: [{ idSector, asientos: [{ fila, numero }] }]`; en sector general sigue `{ idSector, cantidad: 1 }`. Un 409 por asiento tomado quita solo los asientos en conflicto de la selección. Las entradas traen `fila` y `asiento`. Los forms de gestión envían `filas` y `asientosPorFila` (la capacidad se deriva).

**Cola admin:** `GET /api/devoluciones`, aprobación/rechazo y `POST /api/eventos/{id}/reintegro-cancelacion` (`FEATURE.adminColaVentas`).

---

## 3.1 Fila virtual — msvc-ventas (FIL-01…FIL-04)

| Método | Path | Auth | Front |
|---|---|---|---|
| POST | `/api/eventos/{id}/fila/ingreso` | JWT (CLIENTE; ADMIN) | `FilaVirtualPage` → “Entrar a la fila virtual” |
| GET | `/api/eventos/{id}/fila/posicion` | JWT (CLIENTE; ADMIN) | polling cada 5 s en `FilaVirtualPage` |
| POST | `/api/eventos/{id}/fila/validacion` | JWT (CLIENTE; ADMIN) | “Validar turno y comprar” → guarda `tokenFila` y navega al checkout |
| GET | `/api/eventos/{id}/fila/estado` | público | tarjeta de alta demanda en detalle + stats de la fila |
| POST | `/api/eventos/{id}/fila/habilitacion` | ADMIN | override de tanda en `FilaVirtualPage` (y acceso “Fila virtual” en gestión) |

Flujo UI: evento `PUBLICADO` + `esAltaDemanda` → el detalle ofrece la fila y los botones de
sector redirigen a `/eventos/:id/fila`; al validar el turno el SPA guarda `{token, expiraEn}` en
`sessionStorage` y el checkout envía `tokenFila` en `POST /api/reservas`. Los errores
`FILA_TOKEN_REQUERIDO` / `FILA_TOKEN_INVALIDO` descartan el pase y muestran el CTA para volver
a la fila. En mock (`VITE_USE_MOCK=true`) el estado se simula en
`src/api/mock/filaStore.js` con tanda automática y consumo del turno al comprar.

---

## 4. Entradas + validación + devoluciones — msvc-entradas

| Método | Path | Auth | Front |
|---|---|---|---|
| GET | `/api/entradas` | JWT (propias) | `/mis-entradas` |
| GET | `/api/entradas/{id}` | JWT | detalle |
| GET | `/api/entradas/{id}/qr` | JWT | `getEntradaQr` — solo VENDIDA + evento ≠ CANCELADO |
| POST | `/api/eventos/{idEvento}/validaciones` | PERSONAL_ACCESO | `/acceso/scan` |
| GET | `/api/eventos/{idEvento}/validaciones` | ORG\|ADMIN | `listValidacionesEvento` |
| GET | `/api/reportes/eventos/{idEvento}` | ORG (dueño) \| ADMIN | `/gestion/eventos/:id/reportes` (`getReporteEvento`; gate `FEATURE.reportes`) |
| POST | `/api/entradas/{id}/devoluciones` | CLIENTE | soporte / detalle entrada |
| GET | `/api/devoluciones` | CLIENTE filtrado / ADMIN pendientes | mis solicitudes + cola admin |
| GET | `/api/devoluciones/{id}` | JWT | `getDevolucion` |
| POST | `/api/devoluciones/{id}/aprobacion` | ADMIN | `/gestion/ventas` |
| POST | `/api/devoluciones/{id}/rechazo` | ADMIN | `/gestion/ventas` |

---

## 5. Preferencias / notificaciones / recomendaciones (implementado)

| Método | Endpoint | Uso |
|---|---|---|
| GET | `/api/notificaciones/me?page=&size=&leida=` | Campanita (conteo) y bandeja `/notificaciones` |
| PATCH | `/api/notificaciones/me/{id}/leida` | Marcar una notificación como leída |
| GET | `/api/preferencias/me/notificaciones` | Canales `push`/`email` en la bandeja |
| PUT | `/api/preferencias/me/notificaciones` | Guardar canales desde la bandeja |
| GET | `/api/preferencias/me` | Preferencias explícitas en “Mis preferencias” |
| PUT | `/api/preferencias/me` | Guardar tipos, ciudades, artistas y rango de precio |
| PUT | `/api/preferencias/me/artistas` | Atajo para artistas favoritos |
| GET | `/api/recomendaciones/me?limit=8` | “Recomendados para vos” en la home (JWT) |
| GET | `/api/eventos/{id}/recomendaciones?limit=6` | “Eventos similares” en el detalle (público) |
| GET | `/api/clientes/{id}/historial?page=&size=` | “Mi historial” en el perfil (dueño/ADMIN) |

Componentes: `NotificationBell` en el header (badge + últimas 6) y `NotificacionesPage`
(`/notificaciones`, protegida). Polling de la campanita cada 45 s. Gate: `FEATURE.notificaciones`.
Preferencias y recomendaciones: `HomeRecomendados`, `EventosSimilares`, `PreferenciasForm`,
`HistorialCliente` y `RecomendacionesBlock`; gates `FEATURE.preferencias` y
`FEATURE.recomendaciones`.

---

## 6. Mapa UI → path back

| Ruta front | Endpoints |
|---|---|
| `/login` | POST `/api/auth/login`, `/api/auth/register` |
| `/perfil` | GET/PATCH `/api/usuarios/me` · GET/PUT `/api/preferencias/me` · GET `/api/clientes/{id}/historial` |
| `/` | GET `/api/eventos` (secciones home; «Destacados» pide `?destacado=true&size=200&fechaDesde=<hoy>` y descarta en el cliente los que ya pasaron) · GET `/api/recomendaciones/me` (logueado) |
| `/eventos` | JWT · sidebar filtros · GET `/api/eventos` (+ batch disponibilidad) |
| `/eventos/:id` | GET `/api/eventos/{id}` · GET `/api/eventos/{id}/recomendaciones` · GET `/api/eventos/{id}/fila/estado` (alta demanda) |
| `/eventos/:id/fila` | POST `.../fila/ingreso` · GET `.../fila/posicion` (polling) · POST `.../fila/validacion` · GET `.../fila/estado` · POST `.../fila/habilitacion` (ADMIN) |
| `/eventos/:id/comprar` | GET `/api/eventos/{id}/disponibilidad` → POST `/api/reservas` (con `tokenFila` si el evento es de alta demanda) → POST `/api/compras` → POST `/api/compras/{id}/pagos` (sector numerado: selector de asientos) |
| `/mis-entradas` | GET `/api/entradas` |
| `/mis-entradas/:id` | GET `/api/entradas/{id}` |
| `/notificaciones` | GET `/api/notificaciones/me` · PATCH `.../{id}/leida` · GET/PUT `/api/preferencias/me/notificaciones` |
| `/soporte/devolucion` | POST `/api/entradas/{id}/devoluciones` |
| `/soporte/transferencia` | **no está en back** — shell UI |
| `/gestion` | GET `/api/eventos` o `/api/organizadores/me/eventos` · GET `/api/eventos/disponibilidad` (resumen; ADMIN suma usuarios y cola de devoluciones) |
| `/gestion/eventos` | GET `/api/organizadores/me/eventos` o GET `/api/eventos` |
| `/gestion/eventos/nuevo` | POST `/api/eventos` (si se crea publicando, antes pasa por el popup de alta demanda) |
| `/gestion/eventos/:id/editar` | GET/PUT `/api/eventos/{id}` |
| `/gestion/eventos/:id/reportes` | GET `/api/reportes/eventos/{id}` |
| `/gestion/entradas/:id` | GET `/api/entradas/{id}` (ADMIN) |
| publicar / estado | GET `/api/eventos/{id}` (para el cupo total del popup) · PUT `/api/eventos/{id}` con `{esAltaDemanda}` si el flag cambia · POST `.../publicacion` · PATCH `.../estado` |
| `/gestion/usuarios` | GET `/api/usuarios` · PATCH `.../estado` · PUT `.../roles` |
| `/gestion/participantes` | GET/POST `/api/participantes` · PUT/DELETE `/api/participantes/{id}` (ADMIN; 409 si el nombre existe / si participa en eventos) — gate `FEATURE.participantes` |
| `/gestion/lugares`, `/gestion/lugares/nuevo`, `/gestion/lugares/:id` | GET/POST `/api/lugares` · GET/PUT/DELETE `/api/lugares/{id}` · POST `.../sectores` · PUT `/api/sectores/{id}` |
| `/gestion/ventas` | GET `/api/devoluciones` · POST `.../aprobacion|rechazo` |
| `/acceso/scan` | POST `/api/eventos/{idEvento}/validaciones` |

---

## 6.1 Popup previo a publicar (regla de negocio)

Antes de **toda** transición a `PUBLICADO` (crear y publicar, publicar borrador o volver a publicar un suspendido), el SPA muestra `ConfirmarAltaDemandaModal`:

- Cupo total de los sectores vendidos **≤ 5000**: pregunta **“¿Es este un evento de alta demanda?”** con botones **Sí** / **No** (y **Cancelar**, que deja el evento sin publicar).
- Cupo total **> 5000**: aviso **“Tu evento es un evento de alta demanda”** con un único botón **Continuar**; el flag se fuerza en `true`.

El total se calcula con `cupoTotalSectores` (en `domain/evento/sectores.js`) sobre las filas del form de alta o sobre `detalle.sectores` (donde `capacidad` ya es el cupo efectivo). La respuesta del popup se persiste con `patchEventoAltaDemanda` (`PUT /api/eventos/{id}` parcial) antes de publicar en el listado de gestión; en el alta viaja en el body de `POST /api/eventos`. El backend además fuerza el flag cuando el cupo supera 5000, aunque la petición venga directa de la API.

## 7. Facades `src/api`

| Archivo | Dominio |
|---|---|
| `auth.js` | login, register, refresh, logout, me |
| `eventos.js` | catálogo, detalle, gestión, create, publicar/estado, `patchEventoAltaDemanda`, disponibilidad |
| `usuarios.js` | admin list/get/estado/roles (+ mock acceso) |
| `entradas.js` | list/detalle/qr/validar |
| `ventasCliente.js` | compras, reservas (+ `tokenFila` de alta demanda) |
| `filaVirtual.js` | ingreso, posición, estado, validación y habilitación de la fila virtual |
| `ventas.js` | cola admin UI + resolución devoluciones |
| `soporteCliente.js` | alta/listado devoluciones cliente |
| `preferencias.js` | preferencias, artistas, recomendaciones propias/similares e historial |
| `http.js` | fetch gateway |
| `mock/*` | LS solo si `USE_MOCK` |

Fuente de verdad de paths de negocio: **`endpoints-back.md`**. Este archivo es la vista “qué usa el SPA”.
