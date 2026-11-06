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

- E1-VEN-08: stock/TTL reservas (código Spring aún pendiente)
- E2 fila virtual / tokens (posterior)

Hasta entonces el contenedor solo tiene que estar healthy (`PING`).
