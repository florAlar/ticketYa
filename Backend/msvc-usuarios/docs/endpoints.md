# Endpoints — msvc-usuarios

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/docs/endpoints.md`

Paths públicos vía gateway: prefijo `/api` (ej. `/api/auth/login`). Abajo paths **internos** del msvc (sin `/api`; el GW hace StripPrefix=1).

---

## Estado implementación (E1-USU-01)

| Endpoint | Estado | Notas |
| --- | --- | --- |
| POST /auth/login | **hecho** | BCrypt + JWT; respuesta `{ token, type, user }` (FE AuthProvider) |
| POST /auth/register | **hecho** | Solo rol CLIENTE; 409 si email existe |
| POST /auth/refresh | **hecho** | Re-emite JWT si Bearer válido |
| POST /auth/logout | **hecho** | 204 no-op (stateless) |
| GET /usuarios/me | **hecho** | Requiere header `X-User-Id` (gateway) |
| PATCH /usuarios/me | **hecho** | nombre/apellido/ciudad |
| GET /usuarios | **hecho** | listado (RBAC en gateway = ADMIN) |
| GET /usuarios/{id} | **hecho** | |
| PATCH /usuarios/{id}/estado | **hecho** | body `{ "activo": true\|false }` |
| GET/PUT /usuarios/{id}/roles | **hecho** | PUT body `{ "roles": ["CLIENTE", ...] }` |

JWT: `jwt.secret` / `JWT_SECRET` **igual que API Gateway**. Claims: `userId`, `email`, `nombre`, `rol`, `roles`.

Seeds demo (init.sql): demo@ / org@ / admin@ / acceso@ — pass en README mysql (`demo1234` etc.).



Smoke: `GET /` → `microservicio usuarios ok!`

---

## usuarios — identidad, roles (auth de request en gateway)

Servicio: msvc-usuarios | Prefijos: /auth, /usuarios

- POST:  /auth/register — Registrar cliente (solo rol CLIENTE público) — **implementado**
- POST : /auth/login — Login; emite token consumido/validado por el gateway — **implementado**
- POST:  /auth/refresh — Renovar token — **implementado**
- POST:  /auth/logout — Cerrar sesión / invalidar refresh — **implementado** (no-op server)
- GET : /usuarios/me — Perfil del usuario autenticado — **implementado**
- PATCH:  /usuarios/me — Actualizar/modificardatos perfil propio — **implementado**
- GET : /usuarios — Listar usuarios (ADMIN) — **implementado**
- GET : /usuarios/{id} — Consultar usuario (ADMIN) — **implementado**
- PATCH: /usuarios/{id}/estado — Activar/desactivar usuario (ADMIN) — **implementado**
- GET : /usuarios/{id}/roles — Consultar roles (ADMIN) — **implementado**
- PUT : /usuarios/{id}/roles — Asignar roles (ADMIN) — **implementado**

### Enrutamiento gateway (relacionado)

- /auth/** → msvc-usuarios
- /usuarios/** → msvc-usuarios

### Notas del global

- msvc-usuarios administra identidad/roles y emisión de credenciales; el gateway autentica y enruta.
- Roles: CLIENTE | ORGANIZADOR | ADMIN | PERSONAL_ACCESO
- Header Authorization: Bearer *** en rutas autenticadas (gateway).
- Hacia este msvc el gateway propaga `X-User-Id` / `X-User-Email` / `X-User-Role(s)`.
