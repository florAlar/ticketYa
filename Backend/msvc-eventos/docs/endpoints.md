# Endpoints — msvc-eventos

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/docs/endpoints.md`

Paths públicos vía gateway: prefijo `/api`. Abajo se listan como en el global.
Directo al msvc (dev): `http://localhost:8082` (sin `/api`).

---

## Endpoint principal (catálogo)

Endpoint principal (catálogo público): GET /api/eventos

Ejemplo: GET /api/eventos?tipo=recital&ciudad=Tandil&fechaDesde=2027-01-01&fechaHasta=2027-01-31&organizador=456&orden=fecha&page=0&size=20

Query implementada: `tipo` | `organizador` | `ciudad` | `fechaDesde` | `fechaHasta` | `estado` | `orden(fecha|precio|relevancia)` | `page` | `size`  
Query documentada global aún no cableada: `participante` | `precioMin` | `precioMax` (sin tabla participante / filtro precio en listado).

Default catálogo: `estado=PUBLICADO` si no se envía `estado`.

Respuesta listado:

```json
{
  "content": [ /* EventoResponseDTO */ ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

`EventoResponseDTO` (catálogo): `id`, `nombre`, `tipo`, `fechaInicio`, `lugar{nombre,ciudad}`, `imagenUrl` (null — no hay columna), `precioDesde` (min `precio_sector` activo), `destacado` (false — no hay columna), `estado`, `idOrganizador`, `esAltaDemanda`.

---

## eventos — eventos, lugares, sectores, participantes

Servicio: msvc-eventos | Prefijos: /eventos (catálogo), /lugares, /sectores, /participantes, /organizadores

### recurso Eventos

- GET: /eventos — Buscar eventos con filtros/paginación (endpoint principal) — **implementado (E1-EVE-04)**
- GET: /eventos/{id} — Detalle de evento — **implementado (E1-EVE-04)**
  - 200: `EventoDetalleResponseDTO` (`descripcion`, `fechaFin`, `fechaPublicacion`, `lugar{id,nombre,direccion,ciudad,sectores[]}`, `sectores[]` con precio/moneda, resto catálogo)
  - 404: `{ timestamp, status, error, message }`
- POST: /eventos — Crear evento en borrador (ORGANIZADOR)
- PUT: /eventos/{id} — Actualizar configuración del evento (ORGANIZADOR)
- DELETE: /eventos/{id} — Eliminar borrador (ORGANIZADOR)
- POST: /eventos/{id}/publicacion — Publicar evento (valida fecha/lugar/sectores/precios/capacidad)
- PATCH: /eventos/{id}/estado — Suspender o cancelar evento (ADMIN); cancelación dispara reintegros/notifs
- PATCH: /eventos/{id}/fecha — Modificar fecha/hora (ORGANIZADOR/ADMIN)
- GET: /organizadores/me/eventos — Listar eventos propios (ORGANIZADOR)
- GET: /eventos/{id}/sectores — Sectores configurados del evento
- PUT: /eventos/{id}/sectores/{sectorId}/precio — Configurar precio de sector en el evento (ORGANIZADOR)
- PUT: /eventos/{id}/participantes — Asociar participantes al evento (ORGANIZADOR)

Smoke infra (no negocio): `GET /` → `microservicio eventos ok!`

### recurso Lugares y sectores

- GET: /lugares — Buscar lugares
- GET: /lugares/{id} — Consultar lugar y sectores
- POST: /lugares — Crear lugar (ORGANIZADOR/ADMIN)
- PUT: /lugares/{id} — Modificar lugar (ORGANIZADOR/ADMIN)
- POST: /lugares/{id}/sectores — Crear sector (ORGANIZADOR/ADMIN)
- PUT: /sectores/{id} — Modificar sector (ORGANIZADOR/ADMIN)
- GET: /lugares/{id}/sectores/{sectorId}/asientos — Config física filas/asientos de sector numerado (no disponibilidad comercial)

Disponibilidad comercial = msvc-ventas.

### Recurso Participantes

- GET: /participantes — Buscar artistas/bandas principales/teloneros
- GET: /participantes/{id} — Consultar participante
- POST: /participantes — Crear participante (ORGANIZADOR/ADMIN)

### Enrutamiento gateway (relacionado)

- /eventos/** → msvc-eventos  (EXCEPCIONES abajo)
- /lugares/** → msvc-eventos
- /participantes/** → msvc-eventos
- /organizadores/** → msvc-eventos
- /sectores/** → msvc-eventos

### Superposición /eventos/** (NO implementar en este msvc)

El gateway enruta estas rutas a otros servicios (antes del catch-all de eventos):

- /eventos/{id}/disponibilidad | /eventos/{id}/fila/** → msvc-ventas
- /eventos/{idEvento}/validaciones → msvc-entradas
- /eventos/{id}/recomendaciones → msvc-preferencias
