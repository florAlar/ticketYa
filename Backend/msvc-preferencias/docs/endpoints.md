# Endpoints — msvc-preferencias

Copia de referencia para este microservicio.
**Fuente canónica (no borrar):** `Backend/docs/endpoints.md`

Paths públicos vía gateway: prefijo `/api`. Abajo se listan como en el global.
(En el global el servicio figura como msvc-preferencias-recomendaciones.)

---

## preferencias — preferencias, historial, recs

Servicio: msvc-preferencias-recomendaciones | Prefijos: /preferencias, /recomendaciones, /clientes + /eventos/{id}/recomendaciones

- GET /preferencias/me — Consultar preferencias propias
- PUT /preferencias/me — Actualizar preferencias (categorías, ciudad, rangoPrecio, etc.)
- GET /preferencias/me/artistas — Consultar artistas favoritos
- PUT /preferencias/me/artistas — Actualizar artistas favoritos
- GET /recomendaciones/me — Eventos recomendados (?limit=)
- GET /eventos/{id}/recomendaciones — Eventos similares (público)

Solape de prefijo: /eventos/{id}/recomendaciones → msvc-preferencias (no eventos).

- GET /clientes/{id}/historial — Historial del cliente (CLIENTE dueño / ADMIN)

Ingesta async típica: CompraConfirmada, EntradaValidada (no REST entre msvcs).

### Enrutamiento gateway (relacionado)

- /preferencias/** → msvc-preferencias-recomendaciones
- /recomendaciones/** → msvc-preferencias-recomendaciones
- /clientes/** → msvc-preferencias-recomendaciones
- /eventos/{id}/recomendaciones → msvc-preferencias (antes del catch-all de eventos)
