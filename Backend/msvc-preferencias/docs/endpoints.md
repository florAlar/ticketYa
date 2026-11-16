# Endpoints — msvc-preferencias

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/specs/endpoints.md`

Paths públicos vía gateway: prefijo `/api`. Abajo se listan como en el global.

---

## preferencias — preferencias, historial, recs

Servicio: msvc-preferencias | Prefijos: /preferencias, /recomendaciones, /clientes + /eventos/{id}/recomendaciones

- `GET /preferencias/me` — Consultar preferencias propias (devuelve defaults si el cliente no tiene documento).
- `PUT /preferencias/me` — Actualizar categorías, ciudades, rangoPrecio, artistas y notificaciones.
- `GET /preferencias/me/notificaciones` — Canales (`push`, `email`) y tipos habilitados.
- `PUT /preferencias/me/notificaciones` — Actualizar canales/tipos.
- `POST /preferencias/clientes/matching` — **Interno** (Feign de msvc-notificaciones):
  body `{ "ciudad": "Buenos Aires", "artistas": ["Metallica"] }` → ids de clientes que
  tienen alguno de esos artistas y esa ciudad. Comparación case-insensitive.
- `GET /preferencias/clientes/{idCliente}/notificaciones` — **Interno** (Feign): preferencias de
  canal del cliente con defaults (`push=true`, `email=true`, `tipos=[]` = todos).

Identidad pública: `X-User-Id` que inyecta el gateway.

### Modelo (`preferencias_clientes`, MongoDB)

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

### Enrutamiento gateway (relacionado)

- /preferencias/** → msvc-preferencias
- /recomendaciones/** → msvc-preferencias
- /clientes/** → msvc-preferencias
- /eventos/{id}/recomendaciones → msvc-preferencias (antes del catch-all de eventos)
