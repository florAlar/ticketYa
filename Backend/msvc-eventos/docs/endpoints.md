# Endpoints — msvc-eventos

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/docs/endpoints.md`

Paths públicos vía gateway: prefijo `/api`. Abajo paths **internos** (sin `/api`).
Directo al msvc (dev): `http://localhost:8082`.

Identidad write: headers gateway `X-User-Id` / `X-User-Role` (JWT).

---

## Estado E1 (2026-11-07)

| Endpoint | Estado | Notas |
| --- | --- | --- |
| GET /eventos | **hecho** | Catálogo; default `estado=PUBLICADO`. Gestión admin: `estado=TODOS\|ALL` (todos los estados). |
| GET /eventos/{id} | **hecho** | Detalle + sectores con precio activo |
| POST /eventos | **hecho** | Crear BORRADOR (ORG/ADMIN). Body form FE: nombre, fechaInicio, lugar* o idLugar, sectores[{nombre,precio,capacidadTotal,sinCupo,esNumerado}]. Si `estado=PUBLICADO` publica al final. |
| PUT /eventos/{id} | **hecho** | Editar config (dueño o ADMIN); no CANCELADO/FINALIZADO |
| DELETE /eventos/{id} | **hecho** | Solo BORRADOR |
| POST /eventos/{id}/publicacion | **hecho** | Valida nombre/fecha/lugar/≥1 precio sector activo |
| PATCH /eventos/{id}/estado | **hecho** | Body `{estado}`. PUBLICADO→misma validación publicar. CANCELADO→Feign reintegro entradas **best-effort**. ADMIN; ORG solo SUSPENDIDO de propios. |
| PATCH /eventos/{id}/fecha | **hecho** | `{fechaInicio,fechaFin?}` dueño/ADMIN |
| GET /organizadores/me/eventos | **hecho** | Listado del org (todos los estados) |
| GET /eventos/{id}/sectores | **hecho** | Precios (activos e inactivos) |
| PUT /eventos/{id}/sectores/{sectorId}/precio | **hecho** | Upsert precio |
| GET/POST/PUT /lugares… | **hecho** | CRUD lugar + POST sectores + GET asientos físicos |
| PUT /sectores/{id} | **hecho** | Modificar sector |
| Participantes | pendiente | E1-EVE-06 — no hay tabla en init |

Smoke: `GET /` → `microservicio eventos ok!`

---

## Catálogo (GET /eventos)

Query: `tipo` | `organizador` | `ciudad` | `fechaDesde` | `fechaHasta` | `estado` | `orden(fecha|precio|relevancia)` | `page` | `size` (max 200)

Default catálogo: `estado=PUBLICADO` si no se envía.  
`estado=TODOS` | `ALL` | `*`: sin filtro de estado (panel admin).

Respuesta listado:

```json
{ "content": [ /* EventoResponseDTO */ ], "page", "size", "totalElements", "totalPages" }
```

`EventoResponseDTO`: `id`, `nombre`, `tipo`, `fechaInicio`, `lugar{nombre,ciudad}`, `imagenUrl` (null), `precioDesde`, `destacado` (false), `estado`, `idOrganizador`, `esAltaDemanda`.

Detalle: + `descripcion`, `fechaFin`, `fechaPublicacion`, `lugar{id,nombre,direccion,ciudad,sectores[]}`, `sectores[]` precio/moneda.

---

## Alta (POST /eventos) — shape FE

```json
{
  "nombre": "…",
  "descripcion": "…",
  "tipo": "RECITAL",
  "fechaInicio": "2026-12-01T21:00:00",
  "estado": "BORRADOR",
  "esAltaDemanda": false,
  "idLugar": null,
  "lugarNombre": "Anfiteatro",
  "lugarCiudad": "Tandil",
  "lugarDireccion": "…",
  "sectores": [
    { "nombre": "General", "precio": 15000, "capacidadTotal": 500, "sinCupo": false, "esNumerado": false }
  ]
}
```

Sin `idLugar`: crea lugar + sectores nuevos. Con `idLugar`: reutiliza lugar; sectores sin `id` se crean bajo ese lugar.

---

## Publicación — validaciones

- nombre, fechaInicio, lugar
- ≥1 `precio_sector` activo con precio ≥ 0
- sector con nombre

Estados: `BORRADOR` | `PUBLICADO` | `AGOTADO` | `SUSPENDIDO` | `CANCELADO` | `FINALIZADO`

---

## Deuda natural (consistencia)

| Deuda | Nota |
| --- | --- |
| Cancelación → reintegro entradas | Feign **best-effort** (header interno ADMIN); si entradas cae, evento igual queda CANCELADO |
| Cupo ventas al publicar | No se crea `cupo_sector` automáticamente en msvc-ventas (sigue seed / flujo ventas) |
| imagenUrl / destacado | Sin columnas en init → siempre null/false |
| Participantes | Sin schema → E1-EVE-06 pendiente |
| Notifs al cancelar/cambiar fecha | E2-NOT |

---

## Config

`ticketya.uri.entradas` / `TICKETYA_URI_ENTRADAS` (Docker: `http://ms-entradas:8084`)

---

## Enrutamiento gateway (relacionado)

- /eventos/** → msvc-eventos (EXCEPCIONES: disponibilidad/fila → ventas; validaciones → entradas; recomendaciones → preferencias)
- /lugares/** /sectores/** /organizadores/** → msvc-eventos
