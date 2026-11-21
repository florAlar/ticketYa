# Endpoints — msvc-preferencias

Microservicio dueño de: preferencias del cliente, historial de compras y recomendaciones.
Base: MongoDB (`preferencias_clientes`, `interacciones_eventos`).

**Fuente canónica (no borrar):** `Backend/specs/endpoints.md`.
**Contexto del servicio:** [`README.md`](README.md) — alcance, agregados, modelo, eventos y operación.
Paths públicos vía gateway: prefijo `/api`.

---

## Preferencias

- `GET /preferencias/me` — Consultar preferencias propias (defaults si no hay documento).
- `PUT /preferencias/me` — Actualizar categorías, ciudades, rangoPrecio, artistas y notificaciones.
- `PUT /preferencias/me/artistas` — Actualizar solo artistas favoritos.
  Body: `{ "artistas": ["Metallica", "Iron Maiden"] }`.
- `GET /preferencias/me/notificaciones` — Canales (`push`, `email`) y tipos habilitados.
- `PUT /preferencias/me/notificaciones` — Actualizar canales/tipos.

Identidad pública: `X-User-Id` que inyecta el gateway.

### Modelo (`preferencias_clientes`)

```json
{
  "idCliente": "uuid",
  "categorias": ["RECITAL"],
  "ciudades": ["Buenos Aires"],
  "rangoPrecio": { "min": 20000, "max": 300000, "moneda": "ARS" },
  "artistas": ["Metallica", "Iron Maiden"],
  "notificaciones": {
    "push": true,
    "email": true,
    "tipos": ["CompraConfirmada", "EventoCancelado"]
  },
  "fechaActualizacion": "2026-10-01T12:00:00Z"
}
```

`tipos` vacío = todos los tipos habilitados.

---

## Recomendaciones

- `GET /recomendaciones/me?limit=10` — **Requiere JWT**.
  Híbrido: preferencias explícitas (categoría, ciudad, artista, rango de precio) +
  co-compra (“quienes compraron X también compraron Y”) + fallback a próximos eventos
  con alta demanda. Excluye eventos ya comprados.

  Respuesta: `[EventoRecomendadoDTO]` — evento ya armado (id, nombre, tipo, fechaInicio,
  lugar, precioDesde, artistas, motivo).

- `GET /eventos/{id}/recomendaciones?limit=6` — **Público**.
  Similares al evento: primero co-compra, luego mismo artista/tipo/ciudad; fallback a
  próximos publicados. Excluye el evento fuente. `404` si el evento no existe.

### Algoritmo (simplificado y configurable)

Pesos en `ticketya.recomendaciones.pesos.*` (`application.yml`):

| Señal | Peso por defecto |
|---|---|
| Artista favorito en el evento | 3.0 |
| Tipo/categoría coincide | 2.0 |
| Ciudad coincide | 2.0 |
| Co-compra (tope 5) | 1.0 por co-compra |
| Precio dentro del rango | 1.0 |
| Alta demanda | 0.5 |

Desempate por `fechaInicio` ascendente. Si no hay señal, se completa con los próximos
eventos publicados. La co-compra se calcula con 2 consultas a Mongo; para volumen alto
se puede precomputar una colección de similitud (fuera del alcance del TP).

---

## Historial

- `GET /clientes/{id}/historial?page=0&size=20` — Dueño (`X-User-Id == id`) o `ADMIN`.
  Devuelve compras ordenadas por `fechaCompra` desc con snapshot del evento.

```json
{
  "content": [
    {
      "id": "mongo-id",
      "tipo": "COMPRA",
      "idEvento": "uuid",
      "nombreEvento": "Metallica — M72 World Tour",
      "categoria": "RECITAL",
      "ciudad": "Buenos Aires",
      "artistas": ["Metallica", "Gojira"],
      "fechaEvento": "2027-03-15T21:00:00Z",
      "fechaCompra": "2026-10-01T14:05:00Z",
      "monto": 95000.00,
      "cantidadEntradas": 1
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### Ingesta

`msvc-ventas` publica `compra.confirmada` después del commit. `msvc-preferencias`
consume la cola `preferencias.compras` y hace un **upsert idempotente por `idCompra`**.
El snapshot del evento se obtiene con Feign a `GET /eventos/{id}`; si eventos no
responde, la interacción se guarda igual (snapshot nulo) y se loguea.

### Backfill

- `POST /internas/backfill` — **Interno, no expuesto por el gateway**.
  Importa las compras aprobadas de `msvc-ventas` (`GET /internas/compras`) y las
  upsertea por `idCompra`. Idempotente.
  Exige `X-Internal-Token` (`TICKETYA_INTERNAL_TOKEN`, default `ticketya_dev_internal`).
  Script: `scripts/backfill-preferencias.sh` (envía el header).

## OpenAPI / Swagger

- JSON: `GET http://localhost:8085/v3/api-docs`
- UI: `http://localhost:8085/swagger-ui/index.html`
- Spec estático: `Backend/docs/openapi/msvc-preferencias.json`

---

## Endpoints internos (Feign)

- `POST /preferencias/clientes/matching` — `{ ciudad, artistas[] }` → ids de clientes
  con alguno de esos artistas y esa ciudad (case-insensitive). Usado por notificaciones.
- `GET /preferencias/clientes/{idCliente}/notificaciones` — Preferencias de canal con
  defaults (`push=true`, `email=true`, `tipos=[]`).

## Enrutamiento gateway (relacionado)

- `/preferencias/**` → msvc-preferencias
- `/recomendaciones/**` → msvc-preferencias (JWT)
- `/clientes/**` → msvc-preferencias (JWT; dueño o ADMIN)
- `/eventos/{id}/recomendaciones` → msvc-preferencias (público; order 13)
