# Comunicación

## Cliente → backend

- Protocolo HTTP + JSON.
- Único punto de entrada externo: **API Gateway** (`http://localhost:8080` en local; prefijo de producto `https://ticketya.com.ar/api/`).
- El frontend (`Frontend/`) habla **solo** con el gateway (`VITE_API_BASE_URL`). No llama puertos de msvc (8081–8086).

## Gateway → microservicios

- Comunicación principal: **REST síncrono**.
- Enrutado por prefijo (detalle en `endpoints.md`):

| Prefijo (tras gateway) | Servicio |
| --- | --- |
| `/auth/**`, `/usuarios/**` | msvc-usuarios |
| `/eventos/**` (catálogo/CRUD), `/lugares/**`, `/sectores/**`, `/participantes/**`, `/organizadores/**` | msvc-eventos |
| `/reservas/**`, `/compras/**`, `/pagos/**`, `/reportes/**` | msvc-ventas |
| `/entradas/**`, `/devoluciones/**` | msvc-entradas |
| `/preferencias/**`, `/recomendaciones/**`, `/clientes/**` | msvc-preferencias |
| `/notificaciones/**` | msvc-notificaciones |

### Solapes de `/eventos/**` (rutear antes del catch-all a eventos)

| Ruta | Servicio |
| --- | --- |
| `/eventos/{id}/disponibilidad`, `/eventos/{id}/fila/**` | msvc-ventas |
| `/eventos/{idEvento}/validaciones` | msvc-entradas |
| `/eventos/{id}/recomendaciones` | msvc-preferencias |

## Async (justificado)

- Notificaciones y efectos de dominio (compra confirmada, cancelación de evento, etc.): vía broker/eventos de dominio cuando se implemente (E2).
- Fila virtual / tokens temporales: apoyo Redis en msvc-ventas (E2).
- No hay REST obligatorio entre msvcs para esas ingestas.

## Headers de correlación / idempotencia

| Header | Uso |
| --- | --- |
| `Authorization: Bearer …` | JWT (E2); E1 puede usar id por header/param según diseño |
| `Content-Type: application/json` | Cuerpos JSON |
| `Idempotency-Key` | Compras/pagos/reservas (anti reintento duplicado) |
| `X-Correlation-Id` | Seguimiento entre servicios; el gateway puede generarlo |

Contrato de rutas: `specs/endpoints.md`. Vista front: `Frontend/docs/endpoints-front.md`.
