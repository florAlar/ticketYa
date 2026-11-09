# API Gateway — Ruteo, rutas públicas y RBAC (E1-GW)

**API-05 · msvc `api-gateway` · puerto 8080**  
Documenta el contrato de enrutamiento del gateway: tabla de rutas, paths públicos, matriz de autorización por rol y headers propagados hacia los microservicios.

---

## 1. Tabla de rutas

Todas las rutas viven en `APIGateway/src/main/resources/application.yml` bajo `spring.cloud.gateway.server.webmvc.routes`. El prefijo externo `/api` se elimina con `StripPrefix=1` antes de reenviar al microservicio destino (path interno sin `/api`).

| # | id | order | Path externo (predicate) | Método | URI destino | StripPrefix | Notas |
|---|----|-------|--------------------------|--------|-------------|-------------|-------|
| 1 | `eventos-disponibilidad-batch-ventas` | 9 | `/api/eventos/disponibilidad` | GET | ms-ventas:8083 | 1 | Excepción antes del catch-all eventos |
| 2 | `eventos-disponibilidad-ventas` | 10 | `/api/eventos/{id}/disponibilidad` | GET | ms-ventas:8083 | 1 | — |
| 3 | `eventos-cupos-liberacion-ventas` | 9 | `/api/eventos/{id}/cupos/liberacion` | POST | ms-ventas:8083 | 1 | Uso interno Feign msvc-entradas |
| 4 | `eventos-cupos-ventas` | 10 | `/api/eventos/{id}/cupos` | POST | ms-ventas:8083 | 1 | Uso interno Feign msvc-eventos |
| 5 | `eventos-fila-ventas` | 11 | `/api/eventos/{id}/fila/**` | — | ms-ventas:8083 | 1 | Fila virtual no implementada en E1 |
| 6 | `eventos-validaciones-entradas` | 12 | `/api/eventos/{id}/validaciones` | — | ms-entradas:8084 | 1 | Validación de acceso en evento |
| 7 | `eventos-recomendaciones-preferencias` | 13 | `/api/eventos/{id}/recomendaciones` | — | ms-preferencias:8085 | 1 | — |
| 8 | `eventos` | 20 | `/api/eventos`, `/api/eventos/**` | — | ms-eventos:8082 | 1 | Catch-all eventos/lugares |
| 9 | `lugares` | 21 | `/api/lugares`, `/api/lugares/**` | — | ms-eventos:8082 | 1 | — |
| 10 | `sectores` | 22 | `/api/sectores`, `/api/sectores/**` | — | ms-eventos:8082 | 1 | — |
| 11 | `participantes` | 23 | `/api/participantes`, `/api/participantes/**` | — | ms-eventos:8082 | 1 | — |
| 12 | `organizadores` | 24 | `/api/organizadores`, `/api/organizadores/**` | — | ms-eventos:8082 | 1 | — |
| 13 | `auth` | 30 | `/api/auth`, `/api/auth/**` | — | ms-usuarios:8081 | 1 | Login / registro |
| 14 | `usuarios` | 31 | `/api/usuarios`, `/api/usuarios/**` | — | ms-usuarios:8081 | 1 | Gestión de identidad |
| 15 | `reservas` | 40 | `/api/reservas`, `/api/reservas/**` | — | ms-ventas:8083 | 1 | Self-service comprador |
| 16 | `compras` | 41 | `/api/compras`, `/api/compras/**` | — | ms-ventas:8083 | 1 | Self-service comprador |
| 17 | `pagos` | 42 | `/api/pagos`, `/api/pagos/**` | — | ms-ventas:8083 | 1 | Self-service comprador |
| 18 | `reportes` | 43 | `/api/reportes`, `/api/reportes/**` | — | ms-ventas:8083 | 1 | Endpoints no implementados en E1 |
| 19 | `entradas` | 50 | `/api/entradas`, `/api/entradas/**` | — | ms-entradas:8084 | 1 | Self-service comprador |
| 20 | `devoluciones` | 51 | `/api/devoluciones`, `/api/devoluciones/**` | — | ms-entradas:8084 | 1 | Self-service comprador + admin |
| 21 | `preferencias` | 60 | `/api/preferencias`, `/api/preferencias/**` | — | ms-preferencias:8085 | 1 | — |
| 22 | `recomendaciones` | 61 | `/api/recomendaciones`, `/api/recomendaciones/**` | — | ms-preferencias:8085 | 1 | — |
| 23 | `clientes-preferencias` | 62 | `/api/clientes`, `/api/clientes/**` | — | ms-preferencias:8085 | 1 | — |
| 24 | `notificaciones` | 70 | `/api/notificaciones`, `/api/notificaciones/**` | — | ms-notificaciones:8086 | 1 | — |

**Sobre los endpoints no implementados en E1:** `/api/reportes/*`, `/api/eventos/{id}/fila/**`, `/api/eventos/{id}/validaciones` y `/api/eventos/{id}/recomendaciones` se rutean pero sus microservicios destino no los implementan todavía (se documentan como pendientes de E2).

---

## 2. Rutas públicas (sin JWT)

El filtro `gateway.filter.JwtAuthenticationFilter` las deja pasar sin validar token (`PUBLIC_PREFIXES` + lectura pública de catálogo):

| Path | Método | Descripción |
|------|--------|-------------|
| `/api/auth/login` | POST | Emisión de JWT (msvc-usuarios) |
| `/api/auth/register` | POST | Alta de usuario (msvc-usuarios) |
| `/api/eventos` | GET | Catálogo paginado público (PUBLICADO + AGOTADO) |
| `/api/eventos/{id}` | GET | Detalle de evento público |
| `/api/participantes` | GET | Catálogo abierto de artistas/bandas |
| `/api/participantes/{id}` | GET | Detalle de participante público |
| `/api/eventos/{id}/participantes` | GET | Elenco de un evento (público) |
| `/api/eventos/disponibilidad` | GET | Batch disponibilidad comercial (público) |
| `/api/eventos/{id}/disponibilidad` | GET | Disponibilidad de un evento (público) |
| `/api/recomendaciones` y subpaths | GET | Recomendaciones (público) |
| `/swagger-ui`, `/v3/api-docs`, `/swagger-ui.html`, `/webjars/`, `/api-docs` | — | Documentación OpenAPI de cada msvc (directo, no vía gateway) |
| `/` | GET | Smoke del gateway |

Todo lo demás requiere `Authorization: Bearer <JWT>`.

---

## 3. Matriz de autorización (RBAC)

Decisión fina en `JwtAuthenticationFilter.hasPermission()`. Los roles válidos son `ADMIN`, `ORGANIZADOR`, `CLIENTE`, `PERSONAL_ACCESO` (el claim legado `ACCESO` se normaliza a `PERSONAL_ACCESO` en `JwtUtil.normalizeRole`).

### 3.1 Siempre permitido (cualquier rol autenticado)

`ADMIN` tiene acceso a **todo**. Para el resto, se aplican las reglas por rol:

| Rol | Paths permitidos | Nota |
|-----|------------------|------|
| `CLIENTE` | `/api/usuarios/me/**`, `/api/auth/**`, compras propias (`/api/reservas/**`, `/api/compras/**`, `/api/pagos/**`, `/api/entradas/**`, `/api/devoluciones/**`), disponibilidad GET, fila GET, `/api/preferencias/**`, `/api/clientes/**`, `GET /api/eventos/**` | El msvc filtra recursos por `X-User-Id` |
| `ORGANIZADOR` | `/api/eventos/**`, `/api/lugares/**`, `/api/sectores/**`, `/api/participantes/**`, `/api/organizadores/**`, `/api/reportes/**`, `GET /api/eventos/{id}/validaciones`, `/api/usuarios/me/**`, `/api/auth/**`, compras propias (self-service) | Revalida localmente en msvc-eventos (dueño del evento) |
| `PERSONAL_ACCESO` | `/api/usuarios/me/**`, `/api/auth/**`, `/api/eventos/{id}/validaciones` (POST/GET), compras propias (self-service) | Validación de ingreso en evento |
| `ADMIN` | Todo | Short-circuit en `hasPermission` |

### 3.2 Exclusivas de ADMIN (403 para cualquier otro rol)

| Path | Método |
|------|--------|
| `/api/usuarios` | GET |
| `/api/usuarios/{id}/estado` | PATCH |
| `/api/usuarios/{id}/roles` | GET / PUT / PATCH |
| `/api/devoluciones/{id}/aprobacion` | POST |
| `/api/devoluciones/{id}/rechazo` | POST |
| `/api/reportes/**` | DELETE |

### 3.3 Notas de política

- **Lugares:** `GET /api/lugares*` y `GET /api/eventos/{id}/sectores` requieren JWT (no están en la lista pública). Un `CLIENTE` recibe **403** en `/api/lugares` (solo ORG/ADMIN).  
- **Self-service comprador:** `CLIENTE`, `ORGANIZADOR` y `PERSONAL_ACCESO` pueden usar `/api/reservas/**`, `/api/compras/**`, `/api/pagos/**`, `/api/entradas/**`, `/api/devoluciones/**`; el msvc destino filtra por `X-User-Id` (recursos propios) o rechaza si no es dueño (403 del msvc).
- **Organizador no es dueño automático:** el msvc-eventos revalida que el usuario sea dueño del evento o ADMIN en operaciones de escritura (`POST /eventos`, `PUT /eventos/{id}`, `DELETE /eventos/{id}`, `POST /eventos/{id}/publicacion`, etc.).

---

## 4. Headers propagados

Tras validar el JWT, el gateway inyecta/sobrescribe estos headers en la petición reenviada al microservicio (vía `MutableRequest` en `JwtAuthenticationFilter`):

| Header | Origen | Descripción |
|--------|--------|-------------|
| `X-User-Id` | claim `userId` (fallback `id`) | UUID del usuario autenticado |
| `X-User-Email` | claim `email` (fallback `sub`) | Correo del usuario |
| `X-User-Role` | claim `rol` / `role` (primero) | Rol principal |
| `X-User-Roles` | claim `roles` (lista o CSV) | Roles, separados por coma |

Cada microservicio lee `X-User-Id` (obligatorio en operaciones protegidas) y, cuando aplica, `X-User-Role` para revalidar autorización localmente.

---

## 5. Respuestas de error del gateway

Cuerpo JSON, sin depender de Spring Security para la decisión fina:

**401 — No autenticado**
```http
HTTP/1.1 401
Content-Type: application/json

{"error":"No autenticado","message":"Debe proporcionar un token JWT válido"}
```
Cuándo falta token, `Authorization` no empieza con `Bearer `, token inválido o expirado, o error al procesarlo.

**403 — No autorizado**
```http
HTTP/1.1 403
Content-Type: application/json

{"error":"No autorizado","message":"No tiene permisos para acceder a este recurso"}
```
Cuándo el rol autenticado no cumple `hasPermission` para ese path/método (p.ej. `demo@ticketya.com` en `GET /api/lugares`; `POST /api/devoluciones/{id}/aprobacion` y `rechazo` son exclusivas de `ADMIN`).

---

## 6. Verificación operativa

```bash
# 401 sin token
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/entradas
# -> 401

# 403 cliente en lugares (verificado)
TOKEN_DEMO=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"email":"demo@ticketya.com","password":"demo1234"}' \
  | python3 -c "import json,sys;print(json.load(sys.stdin)['token'])")
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/lugares -H "Authorization: Bearer $TOKEN_DEMO"
# -> 403 {"error":"No autorizado","message":"No tiene permisos para acceder a este recurso"}

# Público sin token
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/eventos?size=1
# -> 200

# Smoke
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/
# -> 200 "api gateway ok!"
```

---

*Origen: `APIGateway/src/main/java/gateway/filter/JwtAuthenticationFilter.java`, `APIGateway/src/main/java/gateway/config/SecurityConfig.java`, `APIGateway/src/main/resources/application.yml`.*
