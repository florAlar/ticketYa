# Política de roles y aprovisionamiento de cuentas

> **Estado:** `hecho` · **Fecha:** 2026-11-21 · **Alcance:** qué roles existen, cómo se obtienen y
> qué puede hacer cada uno · **No incluye:** catálogo de endpoints (ver `endpoints.md`/OpenAPI).
>
> Regla base: **toda cuenta nueva es CLIENTE**; los privilegios (ORGANIZADOR, ADMIN,
> PERSONAL_ACCESO) los otorga un **ADMIN**. No hay autoservicio de privilegios.

## 1. Roles

| Rol | Cómo se obtiene | Alcance |
| --- | --- | --- |
| `CLIENTE` | Registro público (`POST /api/auth/register`) | Compra, reservas, entradas, devoluciones, preferencias, historial y bandeja propias |
| `ORGANIZADOR` | Un **ADMIN** lo asigna sobre una cuenta existente (`PUT /api/usuarios/{id}/roles`) | Crear/publicar/editar **sus** eventos, ver **sus** reportes, leer lugares y dar de alta participantes (editar/eliminar participantes es solo ADMIN) |
| `ADMIN` | Solo otro ADMIN (no hay autoservicio) | Gestión completa: usuarios/roles, eventos de todos, lugares, participantes, reportes, devoluciones, cola de fila, acceso |
| `PERSONAL_ACCESO` | Un **ADMIN** lo asigna | Validar QR en la puerta (no listar validaciones: `403`) y comprar como cualquier usuario autenticado (self-service comprador); no gestiona eventos ni usuarios |
| `ACCESO` (legado) | No se asigna | El gateway lo normaliza a `PERSONAL_ACCESO` |

Los seeds de demo (`demo@` CLIENTE, `org@` ORGANIZADOR, `admin@` ADMIN, `acceso@` PERSONAL_ACCESO) se
mantienen para pruebas; no pasan por un flujo de solicitud.

## 2. Aprovisionamiento

- **CLIENTE:** autoservicio en `/login?tab=register`; email único y contraseña válida.
- **ORGANIZADOR:** no hay alta pública ni solicitud self-service. Un ADMIN edita los roles del usuario.
  La idea de “solicitar cuenta de organizador” (`X-ORG-01`) quedó **descartada**: la consigna no la
  pide y el rol se otorga administrativamente.
- **ADMIN / PERSONAL_ACCESO:** solo un ADMIN los asigna; no se autorregistran.
- **Cambio de roles:** `PUT /api/usuarios/{id}/roles` reemplaza el conjunto de roles del usuario. Un
  usuario puede tener más de un rol (p. ej. CLIENTE + ORGANIZADOR) porque `usuario_rol` es M2M.
- **Efecto en la sesión:** el JWT incluye `rol` (principal) y `roles`; tras un cambio de rol, el
  usuario debe refrescar la sesión o volver a loguearse para ver el privilegio nuevo.

## 3. Autorización

- El **API Gateway** valida el JWT y aplica la primera barrera por rol/ruta (`docs/api/gateway.md`).
  Descarta cualquier `X-User-*` enviado por el cliente y los reescribe desde el token.
- Cada microservicio **revalida** permisos y propiedad: un ORGANIZADOR solo edita sus eventos y ve
  sus reportes; un CLIENTE solo accede a sus reservas/compras/entradas/devoluciones/notificaciones.
- Las rutas internas (`/internas/**`, `/fila/**`) exigen `X-Internal-Token`; no se consumen desde el
  navegador.
- **Simplificación declarada (`X-ACC-01`):** cualquier `PERSONAL_ACCESO` puede validar cualquier
  evento; no hay asignación por evento.

## 4. Matriz resumida

| Acción | CLIENTE | ORGANIZADOR | ADMIN | PERSONAL_ACCESO |
| --- | --- | --- | --- | --- |
| Comprar/reservar/entradas propias | ✅ | ✅ | ✅ | ✅ |
| Crear/publicar sus eventos | ❌ | ✅ | ✅ (cualquiera) | ❌ |
| Ver reportes | ❌ | ✅ (sus eventos) | ✅ | ❌ |
| Crear lugares / editar lugares y sectores | ❌ | ❌ | ✅ | ❌ |
| Alta de participantes | ❌ | ✅ | ✅ | ❌ |
| Editar/eliminar participantes | ❌ | ❌ | ✅ | ❌ |
| Gestionar usuarios y roles | ❌ | ❌ | ✅ | ❌ |
| Validar QR | ❌ | ❌ | ✅ | ✅ |
| Resolver devoluciones | ❌ | ❌ | ✅ | ❌ |
| Abrir tandas de fila | ❌ | ❌ | ✅ | ❌ |

El detalle por endpoint y la matriz completa (90 endpoints × 5 actores) están en
`docs/api/gateway.md` y `AutorizacionPorRolMatrixTest`.

## 5. Decisiones y deudas

- **X-ORG-01 descartado:** no hay solicitud/aprobación de cuentas de organizador; el ADMIN asigna el
  rol. La guía `/cuenta/organizador` queda como instructivo.
- **X-ORG-02 (este documento):** política de roles base; CLIENTE self-service, el resto lo otorga ADMIN.
- **Refresh:** no hay revocación de tokens (logout stateless); un cambio de rol requiere refresh/re-login.
- **Roles múltiples:** se conserva CLIENTE + ORGANIZADOR; si el producto definiera reemplazo, se
  cambia la semántica de `PUT /usuarios/{id}/roles`.

## 6. Enlaces

- `specs/reglas-de-negocio.md` (reglas de negocio y roles).
- `docs/api/gateway.md` (rutas públicas y RBAC), `specs/endpoints.md` (contrato).
- `msvc-usuarios/docs/endpoints.md` (`/usuarios/{id}/roles`).
