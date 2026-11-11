# Bundle OpenAPI estático — TicketYa E1

Contratos OpenAPI (3.1.0) generados por **springdoc-openapi-starter-webmvc-ui 3.2.0** en cada microservicio, exportados estáticamente a `Backend/docs/openapi/`.

## Archivos

| Archivo | Contenido |
|---------|-----------|
| `msvc-usuarios.json` | Contrato de usuarios/auth (12 ops / 10 paths) |
| `msvc-eventos.json` | Contrato de eventos/lugares/participantes (25 ops / 17 paths) |
| `msvc-ventas.json` | Contrato de disponibilidad/reservas/compras/pagos (16 ops / 14 paths, 16 schemas) |
| `msvc-entradas.json` | Contrato de entradas/QR/devoluciones/validación de acceso (13 ops / 12 paths, 10 schemas) |
| `openapi-bundle.json` | Bundle consolidado: metadatos + los 4 specs bajo la clave `specs` |

> **Actualizado 2026-11-10:** los specs incluyen los asientos virtuales (`filas`/`asientosPorFila` en sectores; `asientos` en `POST /reservas`, `POST /eventos/{id}/cupos/liberacion` y `POST /entradas/emision`; `asientosOcupados` en disponibilidad; `fila`/`asiento` en la entrada).
>
> **Actualizado 2026-11-11:** `msvc-entradas.json` suma el control de acceso QR (`POST` y `GET /eventos/{idEvento}/validaciones`, schemas `ValidacionRequest`, `ValidacionResponse` y `ValidacionRegistro`). `openapi-bundle.json` regenerado (los otros tres specs sin cambios).

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

# 3. Reconstruir el bundle consolidado
python3 - <<'PY'
import json, glob, os
d='docs/openapi'
specs={}
for f in sorted(glob.glob(d+'/msvc-*.json')):
    name=os.path.basename(f).replace('.json','')
    specs[name]=json.load(open(f))
bundle={
    "generatedAt":"2026-11-10",
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

Comandos rápidos:

```bash
curl -s -o /dev/null -w 'usuarios  -> %{http_code}\n' http://localhost:8081/swagger-ui/index.html
curl -s -o /dev/null -w 'eventos   -> %{http_code}\n' http://localhost:8082/swagger-ui/index.html
curl -s -o /dev/null -w 'ventas    -> %{http_code}\n' http://localhost:8083/swagger-ui/index.html
curl -s -o /dev/null -w 'entradas  -> %{http_code}\n' http://localhost:8084/swagger-ui/index.html
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
- No hay Newman collection en esta fase; la colección Postman (v2.1 + entorno `local` con auto-login) se entrega como parte de los artefactos E1 aparte de este bundle.
- Endpoints no implementados en E1 (`/reportes`, `/fila`, webhooks) no aparecen en los specs.
