# Contexto — msvc-usuarios

| Campo | Valor |
| --- | --- |
| Microservicio | `msvc-usuarios` |
| Bounded context | Identidad, credenciales, roles y emisión de JWT |
| Puerto local | `8081` |
| Base de datos | MySQL `usuarios` (host `3307`) |
| Mensajería | No aplica (sin colas propias) |
| Consumidores | `APIGateway`, frontend (login/perfil) y, de forma indirecta, el resto de los msvc que confían en los headers del JWT |
| Actualizado | `2026-11-21` |

**Fuentes canónicas**

- API: [`endpoints.md`](endpoints.md) y [`docs/openapi/msvc-usuarios.json`](../../docs/openapi/msvc-usuarios.json)
- Modelo: [`docs/modelo-datos.md`](../../docs/modelo-datos.md) §1
- Mensajes: [`specs/comunicacion.md`](../../specs/comunicacion.md) (no participa del broker)
- Reglas: [`specs/reglas-de-negocio.md`](../../specs/reglas-de-negocio.md) §3
- Arquitectura: [`docs/arquitectura.md`](../../docs/arquitectura.md)
- Gateway: [`docs/api/gateway.md`](../../docs/api/gateway.md)

## 1. Alcance y límites

- **Es responsable de:** alta de clientes, login, refresh/logout stateless, perfil propio, administración de usuarios y asignación de roles, hash BCrypt y emisión de JWT.
- **No es responsable de:** autorización por endpoint (la aplica `APIGateway`), preferencias/historial (msvc-preferencias), datos de eventos/compras/entradas ni solicitudes de extensión a organizador (X-ORG-01, pendiente).
- **Consumidores:** el gateway para `/api/auth/**` y `/api/usuarios/**`; el frontend para login/registro/perfil; los demás msvc solo reciben `X-User-Id`, `X-User-Email` y `X-User-Role(s)` ya verificados por el gateway.
- **Dependencias síncronas:** ninguna hacia otros microservicios.
- **Dependencias asíncronas:** ninguna.

## 2. Agregados e invariantes

| Agregado / entidad raíz | Entidades relacionadas | Invariantes principales |
| --- | --- | --- |
| `Usuario` | `Rol` (M2M `usuario_rol`) | Email normalizado y único; `password_hash` BCrypt; `activo=false` no puede iniciar sesión; el alta pública crea solo `CLIENTE` |
| `Rol` | `Usuario` | `nombre` único; roles válidos: `CLIENTE`, `ORGANIZADOR`, `ADMIN`, `PERSONAL_ACCESO`; asignar/editar roles es solo `ADMIN` |

**JWT:** claims `userId`, `email`, `nombre`, `rol` y `roles`; el secreto (`jwt.secret` / `JWT_SECRET`) es el mismo que usa el gateway. `logout` es un no-op: el token no se persiste ni se revoca.

## 3. API pública

| Método | Path | Rol / auth | Uso |
| --- | --- | --- | --- |
| `POST` | `/auth/register` | Público | Alta de cliente (`CLIENTE`) |
| `POST` | `/auth/login` | Público | Autenticación y emisión de JWT |
| `POST` | `/auth/refresh` | Bearer válido | Renovar token |
| `POST` | `/auth/logout` | Bearer | Cierre stateless (204) |
| `GET/PATCH` | `/usuarios/me` | JWT (`X-User-Id`) | Perfil propio |
| `GET` | `/usuarios` | ADMIN (gateway) | Listado de usuarios |
| `GET` | `/usuarios/{id}` | ADMIN (gateway) | Detalle de usuario |
| `PATCH` | `/usuarios/{id}/estado` | ADMIN (gateway) | Activar/desactivar |
| `GET/PUT` | `/usuarios/{id}/roles` | ADMIN (gateway) | Consultar/asignar roles |

El detalle de cuerpos, códigos y ejemplos está en [`endpoints.md`](endpoints.md). Swagger: `http://localhost:8081/swagger-ui/index.html`.

## 4. Modelo local

| Tabla | Clave | Índices / restricciones | Notas |
| --- | --- | --- | --- |
| `rol` | `id_rol` | UK `nombre` | Catálogo de roles |
| `usuario` | `id_usuario` | UK `email`; `activo`; `fecha_registro` | Datos de identidad y perfil |
| `usuario_rol` | `(id_usuario, id_rol)` | PK compuesta | M2M de roles |

Referencias lógicas: `id_usuario` se usa en eventos, ventas, entradas, preferencias y notificaciones, sin FK física entre bases. Esquema/seed: `infrastructure/mysql/usuarios/init.sql`.

## 5. Eventos y mensajes

No publica ni consume mensajes de RabbitMQ. Los eventos de login/registro/fallo se registran en la bitácora local (`Bitacora.registrar`), no en el broker.

## 6. Reglas y criterios de aceptación

| Regla / escenario | Resultado esperado | Test que lo cubre |
| --- | --- | --- |
| Registro con email existente | `409` | `AuthServiceImplTest` |
| Login con credenciales inválidas o cuenta inactiva | `401` y bitácora de fallo | `AuthServiceImplTest` |
| Refresh sin Bearer válido | `401` | `AuthServiceImplTest` |
| Alta pública de un rol distinto de `CLIENTE` | `400/403` | `AuthServiceImplTest` |
| Asignación de roles por un no-ADMIN | `403` en gateway | `AutorizacionPorRolMatrixTest` (APIGateway) |
| Perfil propio sin `X-User-Id` | `401` | `UsuarioServiceImplTest` |
| JWT con claims y expiración correctos | Token verificable por el gateway | `JwtUtilTest` |

## 7. Operación

```bash
cd Backend
docker compose up -d --build ms-usuarios

cd msvc-usuarios
mvn test              # 58 unitarios
mvn -Pit verify       # +1 IT de contexto con MySQL real (Testcontainers)
```

- **Variables:** `SPRING_DATASOURCE_URL` (default `jdbc:mysql://localhost:3307/usuarios`), `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`.
- **Smoke:** `GET /` → `microservicio usuarios ok!`; OpenAPI en `/v3/api-docs`.
- **Seeds:** `demo@ticketya.com` / `demo1234`, `org@ticketya.com` / `org1234`, `admin@ticketya.com` / `admin1234`, `acceso@ticketya.com` / `acceso1234`.

## 8. Deuda y decisiones

| Tema | Estado | Nota |
| --- | --- | --- |
| Refresh tokens / revocación | `pendiente` | Logout stateless: no invalida tokens emitidos |
| Extensión CLIENTE → ORGANIZADOR (X-ORG-01) | `pendiente` | Diseño en `specs/backlog.md` §7.3.1; hoy solo seeds/ADMIN asignan el rol |
| Aplicación de DOC-MS-00 | `parcial` | README completo; falta revisión final del equipo |
