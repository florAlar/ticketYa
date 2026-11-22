# Bundle OpenAPI estático — TicketYa (E1 + E2)

Contratos OpenAPI (3.1.0) generados por **springdoc-openapi-starter-webmvc-ui 3.2.0** en cada microservicio, exportados estáticamente a `Backend/docs/openapi/`.

## Archivos

| Archivo | Contenido |
|---------|-----------|
| `msvc-usuarios.json` | Contrato de usuarios/auth (12 ops / 10 paths) |
| `msvc-eventos.json` | Contrato de eventos/lugares/participantes (28 ops / 18 paths) |
| `msvc-ventas.json` | Contrato de disponibilidad/reservas/compras/pagos/reportes del organizador y fila virtual (28 ops / 26 paths, 30 schemas) |
| `msvc-entradas.json` | Contrato de entradas/QR/devoluciones/validación de acceso (14 ops / 13 paths, 11 schemas) |
| `msvc-preferencias.json` | Contrato de preferencias, historial y recomendaciones (13 ops / 11 paths, 11 schemas) |
| `msvc-notificaciones.json` | Contrato de notificaciones: bandeja, alta administrativa y estado de envío (7 ops / 6 paths, 4 schemas). |
| `openapi-bundle.json` | Bundle consolidado: metadatos + los 6 specs bajo la clave `specs` |

> **Actualizado 2026-11-10:** los specs incluyen los asientos virtuales (`filas`/`asientosPorFila` en sectores; `asientos` en `POST /reservas`, `POST /eventos/{id}/cupos/liberacion` y `POST /entradas/emision`; `asientosOcupados` en disponibilidad; `fila`/`asiento` en la entrada).
>
> **Actualizado 2026-11-11:** `msvc-entradas.json` suma el control de acceso QR (`POST` y `GET /eventos/{idEvento}/validaciones`, schemas `ValidacionRequest`, `ValidacionResponse` y `ValidacionRegistro`). `openapi-bundle.json` regenerado (los otros tres specs sin cambios).
>
> **Actualizado 2026-11-11 (reportes):** `msvc-ventas.json` suma los reportes del organizador (`GET /reportes/eventos/{idEvento}` y `/ventas`, `/sectores`, `/ocupacion`, `/recaudacion`; schemas `Reporte*`). `msvc-entradas.json` suma el resumen interno `GET /eventos/{idEvento}/entradas/resumen` (marcado `x-internal`: lo consume ventas por Feign y el gateway no lo expone).
>
> **Actualizado 2026-11-11 (lugares):** `msvc-eventos.json`: el lugar suma `latitud` y `longitud` opcionales (`LugarRequest`, `Lugar`, `LugarDetalle`) y `quitarCoordenadas` en el pedido. `openapi-bundle.json` regenerado.
>
> **Actualizado 2026-11-11 (baja de lugares):** `msvc-eventos.json` suma `DELETE /lugares/{id}` (solo ADMIN; 409 si el lugar tiene eventos). Eventos (estado actual): 28 operaciones / 18 paths.
>
> **Actualizado 2026-11-20:** `msvc-eventos.json` y `openapi-bundle.json` corrigen `POST /lugares`: decía “Requiere ORGANIZADOR o ADMIN” y en realidad es **solo ADMIN** (lo revalida `LugarController` y lo fija `specs/endpoints.md`).
>
> **Actualizado 2026-11-11 (PAR-01):** `msvc-eventos.json` suma `DELETE /participantes/{id}` (solo ADMIN; 409 si participa en eventos); `PUT /participantes/{id}` pasa a solo ADMIN y `POST` exige `tipo` válido (409 si el nombre ya existe). `openapi-bundle.json` regenerado.

> **Actualizado 2026-11-11 (LUG-04):** `msvc-eventos.json`: `EventoRequest` ya no acepta `lugarNombre/lugarCiudad/lugarDireccion` (el evento referencia un `idLugar`); los `sectores[]` son del lugar (`id` obligatorio, `precio`, `capacidadTotal` = cupo del evento); alta y edición de lugares y sectores son solo ADMIN.
>
> **Actualizado 2026-11-16 (E2-PRE):** nuevo `msvc-preferencias.json` con preferencias, artistas, historial, recomendaciones propias/similares y backfill interno (`X-Internal-Token`). `openapi-bundle.json` regenerado con 5 specs.
>
> **Actualizado 2026-11-19 (FIL-04):** `msvc-ventas.json` suma la carpeta `Fila virtual` con los 5 endpoints (`POST/GET /eventos/{idEvento}/fila/ingreso|posicion|estado|validacion|habilitacion` en el contrato interno; el gateway los expone como `/api/eventos/{id}/fila/**`), los schemas `Fila*`, el schema de error `ApiError` con `code`/`extras` y los 8 códigos `FILA_*` en `x-error-codes`. `GET .../fila/estado` es público (sin `security`); los otros cuatro usan `bearerAuth`. `POST /reservas` documenta el campo opcional `tokenFila`. `openapi-bundle.json` regenerado (ventas: 28 ops / 26 paths / 30 schemas). Los paths internos usan `{idEvento}` porque ese es el nombre de la variable en `@RequestMapping`; la documentación de gateway/endpoints usa `{id}` como marcador equivalente.
>
> **Actualizado 2026-11-20 (completitud):** `msvc-eventos.json` suma `GET /lugares/geocodificar` y el schema `Coordenadas`; `msvc-ventas.json` suma `GET /compras/evento/{idEvento}`. `openapi-bundle.json` regenerado (eventos: 28 ops / 18 paths / 20 schemas; ventas: 28 ops / 26 paths / 30 schemas).
>
> **Actualizado 2026-11-20 (notificaciones):** se incorpora `msvc-notificaciones.json` al `openapi-bundle.json`. El detalle y el marcar leída documentan **404** cuando la notificación no existe o es de otro usuario (el msvc no revela su existencia; antes figuraba 403); `ApiError` refleja timestamp/status/error/message/path.
>
> **Actualizado 2026-11-21 (notificaciones + springdoc):** `msvc-notificaciones` suma `springdoc-openapi-starter-webmvc-ui 3.2.0`, `OpenApiConfig` y anotaciones Swagger en el controller/DTOs. Ahora expone `/v3/api-docs` y `/swagger-ui/index.html` como los otros cinco servicios; el JSON estático puede regenerarse con el comando de la sección siguiente. El `GlobalExceptionHandler` devuelve `ApiError` uniforme (timestamp/status/error/message/path), incluidos los errores de validación.
>
> **Actualizado 2026-11-21 (alineación runtime/estático):** `msvc-notificaciones.json` se regeneró desde `/v3/api-docs`. Quedan alineados los tags (`Bandeja`, `Administración`, `Salud`), los `operationId` (`listarPropias`, `detallePropio`, `marcarLeida`, `crearAdministrativa`, `estadoEnvio`, `ok`, `okRaiz`), los headers `X-User-Id`/`X-User-Role` como obligatorios, las respuestas `401` de las rutas administrativas y los media types (`application/json`; `text/plain` en los smoke). La nullability usa sintaxis OpenAPI 3.1 (`type: [..., "null"]`).
>
> **Actualizado 2026-11-20 (fila virtual):** `msvc-ventas.json` documenta el **404** de `POST /eventos/{id}/fila/ingreso` (evento inexistente) y el campo `reingreso` en `FilaPosicion`; las respuestas 400/401/409 de `POST /reservas` usan el schema `ApiError` real. `openapi-bundle.json` regenerado.

## Regeneración (tras cambios en anotaciones)

Cada microservicio expone su spec en runtime. Desde `Backend/`:

```bash
# 1. Reconstruir el microservicio modificado
docker compose build ms-usuarios   # o ms-eventos / ms-ventas / ms-entradas
docker compose up -d --no-deps --force-recreate ms-usuarios

# 2. Esperar a que levante y exportar el spec
curl -s http://localhost:8081/v3/api-docs -o docs/openapi/msvc-usuarios.json   # puerto 8081
curl -s http://localhost:8082/v3/api-docs -o docs/openapi/msvc-eventos.json     # puerto 8082
curl -s http://localhost:8083/v3/api-docs -o docs/openapi/msvc-ventas.json      # puerto 8083
curl -s http://localhost:8084/v3/api-docs -o docs/openapi/msvc-entradas.json    # puerto 8084
curl -s http://localhost:8085/v3/api-docs -o docs/openapi/msvc-preferencias.json # puerto 8085
curl -s http://localhost:8086/v3/api-docs -o docs/openapi/msvc-notificaciones.json # puerto 8086

# 3. Reconstruir el bundle consolidado
python3 - <<'PY'
import json, glob, os
d='docs/openapi'
specs={}
for f in sorted(glob.glob(d+'/msvc-*.json')):
    name=os.path.basename(f).replace('.json','')
    specs[name]=json.load(open(f))
bundle={
    "generatedAt":"2026-11-21",
    "generator":"springdoc-openapi-starter-webmvc-ui 3.2.0",
    "openapi":"3.1.0",
    "microservices":{k:{"paths":len(v.get('paths',{})),"operations":sum(len(p) for p in v.get('paths',{}).values()),"schemas":len(v.get('components',{}).get('schemas',{}))} for k,v in specs.items()},
    "specs":specs
}
json.dump(bundle,open(d+'/openapi-bundle.json','w'),ensure_ascii=False,indent=2)
print("bundle OK")
PY
```

## Verificación en Swagger UI

Cada microservicio sirve su propia UI (no hay agregación en el gateway):

| Microservicio | Swagger UI | YAML | JSON |
|---------------|------------|------|------|
| usuarios (8081) | `http://localhost:8081/swagger-ui/index.html` | `/v3/api-docs.yaml` | `/v3/api-docs` |
| eventos (8082) | `http://localhost:8082/swagger-ui/index.html` | `/v3/api-docs.yaml` | `/v3/api-docs` |
| ventas (8083) | `http://localhost:8083/swagger-ui/index.html` | `/v3/api-docs.yaml` | `/v3/api-docs` |
| entradas (8084) | `http://localhost:8084/swagger-ui/index.html` | `/v3/api-docs.yaml` | `/v3/api-docs` |
| preferencias (8085) | `http://localhost:8085/swagger-ui/index.html` | `/v3/api-docs.yaml` | `/v3/api-docs` |
| notificaciones (8086) | `http://localhost:8086/swagger-ui/index.html` | `/v3/api-docs.yaml` | `/v3/api-docs` |

Comandos rápidos:

```bash
curl -s -o /dev/null -w 'usuarios  -> %{http_code}\n' http://localhost:8081/swagger-ui/index.html
curl -s -o /dev/null -w 'eventos   -> %{http_code}\n' http://localhost:8082/swagger-ui/index.html
curl -s -o /dev/null -w 'ventas    -> %{http_code}\n' http://localhost:8083/swagger-ui/index.html
curl -s -o /dev/null -w 'entradas  -> %{http_code}\n' http://localhost:8084/swagger-ui/index.html
curl -s -o /dev/null -w 'preferencias -> %{http_code}\n' http://localhost:8085/swagger-ui/index.html
curl -s -o /dev/null -w 'notificaciones -> %{http_code}\n' http://localhost:8086/swagger-ui/index.html
```

Todos deben devolver **200**.

## Lint del bundle (local, sin Newman)

Validación estructural básica de cada spec (OpenAPI 3.x, `info.title`, `paths`, `components`, `servers`):

```bash
cd Backend
python3 - <<'PY'
import json, glob, os
d='docs/openapi'
ok=True
for f in sorted(glob.glob(d+'/msvc-*.json')):
    s=json.load(open(f))
    errs=[]
    if not s.get('openapi','').startswith('3.'): errs.append('openapi')
    if not s.get('info',{}).get('title'): errs.append('info.title')
    if 'paths' not in s: errs.append('paths')
    if 'components' not in s: errs.append('components')
    if 'servers' not in s: errs.append('servers')
    print(f"{os.path.basename(f)}: {'LINT OK' if not errs else 'LINT ERR '+','.join(errs)}")
    ok = ok and not errs
print("TODO OK" if ok else "HAY ERRORES")
PY
```

## Notas

- Los paths documentados son internos (sin `/api`); el gateway aplica `StripPrefix=1` y el RBAC se documenta en `docs/api/gateway.md`.
- La colección Postman (v2.1 + entorno `local` con auto-login) se entrega aparte de este bundle y se corre con Postman o Newman (ver `docs/postman/README.md`).
- La fila virtual (`/fila`, FIL-01…FIL-04) y los reportes están documentados en los specs.
- Los errores de fila se documentan en cada respuesta con `x-error-codes` (`FILA_*`) y el payload `ApiError`; `code` y `extras` aparecen solo cuando la excepción los trae.
