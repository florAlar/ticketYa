# Redis — TicketYa (msvc-ventas)

Conf montada en `db-redis-ventas` (no es un init de schema: Redis no tiene tablas).

## Archivo

| Uso | Path |
|---|---|
| conf | `ventas/redis.conf` |

## Compose

`msvc-ventas/docker-compose.yml`:

- image `redis:7`
- command: `redis-server /usr/local/etc/redis/redis.conf`
- volume conf ro + data volume

## Uso de negocio

- **E1-VEN-08 (hecho):** `StockCache` guarda holds de reserva (TTL 10 min) e `Idempotency-Key` (TTL 24 h).
- **E2-FILA-01 (hecho, FIL-01…FIL-04):** `FilaRedisStore` guarda turnos, tandas y tokens (`fila:*`), con lock del scheduler. Ver `specs/fila-virtual.md`.
- Si Redis no responde, las operaciones de fila fallan cerradas (503); el contenedor debe estar healthy (`PING`).
