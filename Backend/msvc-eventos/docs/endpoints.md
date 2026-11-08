# Endpoints — msvc-eventos

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/specs/endpoints.md`

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
| GET/POST/PUT /participantes | **hecho** | Artistas/bandas (E1-EVE-06). Sin authz msvc aún. |
| GET/PUT /eventos/{id}/participantes | **hecho** | Listar / reemplazar set (ids o items con rol/orden) |
| GET /eventos?participante= | **hecho** | Filtro por id de artista |

Smoke: `GET /` → `microservicio eventos ok!`

---

## Catálogo (GET /eventos)

Query: `tipo` | `organizador` | `participante` (id) | `ciudad` | `fechaDesde` | `fechaHasta` | `estado` | `orden(fecha|precio|relevancia)` | `page` | `size` (max 200)

Default catálogo: `estado=PUBLICADO` si no se envía.  
`estado=TODOS` | `ALL` | `*`: sin filtro de estado (panel admin).

Respuesta listado:

```json
{ "content": [ /* EventoResponseDTO */ ], "page", "size", "totalElements", "totalPages" }
```

`EventoResponseDTO`: `id`, `nombre`, `tipo`, `fechaInicio`, `lugar{nombre,ciudad}`, `imagenUrl` (null), `precioDesde`, `destacado` (false), `estado`, `idOrganizador`, `esAltaDemanda`.

Detalle: + `descripcion`, `fechaFin`, `fechaPublicacion`, `lugar{id,nombre,direccion,ciudad,sectores[]}`, `sectores[]` precio/moneda, `participantes[]` (id,nombre,tipo,descripcion,rol,orden).

---

## Participantes (= artistas/bandas, no compradores)

```http
GET  /participantes?q=&tipo=
GET  /participantes/{id}
POST /participantes          body { nombre, tipo?, descripcion? }
PUT  /participantes/{id}
GET  /eventos/{id}/participantes
PUT  /eventos/{id}/participantes
```

Asociar (reemplaza set completo):

```json
{ "ids": ["p000…001", "p000…002"] }
```

o con rol/orden:

```json
{
  "participantes": [
    { "id": "p000…001", "rol": "PRINCIPAL", "orden": 0 },
    { "id": "p000…002", "rol": "TELONERO", "orden": 1 }
  ]
}
```

Authz: lectura abierta (GET). Escritura POST/PUT `/participantes` y PUT asociación: **ORG|ADMIN**; asociar además **dueño del evento o ADMIN**.

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

- nombre, fechaInicio **futura** (no pasada), lugar con nombre/ciudad reales (no placeholder «Por definir»)
- ≥1 `precio_sector` activo con precio ≥ 0
- sector con nombre y **capacidad ≥ 1** (no hay cupo ilimitado; `sinCupo` se ignora)
- Alta en **BORRADOR**: solo `nombre` obligatorio; fecha/lugar/sectores opcionales (lugar provisional si falta; fecha provisional +1 año si falta — schema NOT NULL)

Estados: `BORRADOR` | `PUBLICADO` | `AGOTADO` | `SUSPENDIDO` | `CANCELADO` | `FINALIZADO`

---

## Deuda natural (consistencia)

| Deuda | Nota |
| --- | --- |
| Cancelación → reintegro entradas | Feign **best-effort** (header interno ADMIN); si entradas cae, evento igual queda CANCELADO |
| Cupo ventas al publicar | **hecho** — `doPublicar` Feign `POST msvc-ventas /eventos/{id}/cupos` fail-hard si ventas cae |
| imagenUrl / destacado | Sin columnas en init → siempre null/false |
| Authz participantes | **hecho** — writes ORG/ADMIN; asociar dueño/ADMIN; GET público |
| Notifs al cancelar/cambiar fecha | E2-NOT |

---

## Config

`ticketya.uri.entradas` / `TICKETYA_URI_ENTRADAS` (Docker: `http://ms-entradas:8084`)  
`ticketya.uri.ventas` / `TICKETYA_URI_VENTAS` (Docker: `http://ms-ventas:8083`) — apertura cupos al publicar

---

## Enrutamiento gateway (relacionado)

- /eventos/** → msvc-eventos (EXCEPCIONES: disponibilidad/fila → ventas; validaciones → entradas; recomendaciones → preferencias)
- /lugares/** /sectores/** /organizadores/** /participantes/** → msvc-eventos
