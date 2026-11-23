# Bases de datos y modelo de datos

> **Estado:** `hecho` · **Fecha:** 2026-11-22 · **Alcance:** motores por servicio y dónde vive el
> modelo · **No incluye:** DDL completo (ver `infrastructure/` y `../docs/modelo-datos.md`).

Una base por microservicio, sin FKs físicas entre servicios: las referencias cruzadas son UUID
lógicos. Los esquemas y seeds viven centralizados en [`infrastructure/`](../infrastructure/) y solo
se aplican sobre volúmenes nuevos.

## 1. Motores por servicio

| Servicio | Motor | Contenedor (host) | Qué persiste |
| --- | --- | --- | --- |
| `msvc-usuarios` | MySQL 8.4 | `db-mysql-usuarios` (3307) | Usuarios, roles y credenciales |
| `msvc-eventos` | MySQL 8.4 | `db-mysql-eventos` (3308) | Eventos, lugares, sectores, participantes y precios |
| `msvc-ventas` | MySQL 8.4 + Redis 7 | `db-mysql-ventas` (3309) + `db-redis-ventas` (6380) | Cupos, reservas, compras, pagos, reportes y estado efímero de la fila |
| `msvc-entradas` | MySQL 8.4 | `db-mysql-entradas` (3310) | Entradas/QR, devoluciones y validaciones |
| `msvc-preferencias` | MongoDB 7 | `db-mongo-preferencias` (27018) | Preferencias, historial y recomendaciones (documentos) |
| `msvc-notificaciones` | MySQL 8.4 | `db-mysql-notificaciones` (3311) | Plantillas, bandeja e intentos de envío |

## 2. Dónde está el modelo

- **ER global MySQL y por microservicio:** [`diagramas/modelo.md`](diagramas/modelo.md) y
  [`diagramas/modelo-por-microservicio.md`](diagramas/modelo-por-microservicio.md).
- **Modelo documental de preferencias:** [`diagramas/modelo-mongodb.md`](diagramas/modelo-mongodb.md).
- **Documento formal de entrega:** [`../docs/modelo-datos.md`](../docs/modelo-datos.md).
- **DDL y seeds reales:** `../infrastructure/mysql/*/init.sql` y
  `../infrastructure/mongodb/preferencias/init.js`.

Regla: **enlazar, no duplicar**. Este archivo es el índice; el detalle de tablas, columnas y
relaciones vive en los diagramas y en `docs/modelo-datos.md`.

## 3. Redis y la fila virtual

`msvc-ventas` usa Redis para el stock temporal y los holds de reserva (TTL 10 min), las claves de
idempotencia (TTL 24 h) y la fila virtual. La fila es **Redis-only**: posiciones, tandas y tokens
viven en claves `fila:*` (prefijo configurable por `ticketya.fila.key-prefix` /
`TICKETYA_FILA_PREFIX`, decisión D1 de [`fila-virtual.md`](fila-virtual.md)). No hay tabla MySQL ni
mensaje Rabbit para ella, y su pérdida no afecta reservas ya confirmadas en MySQL.

Configuración del motor: `../infrastructure/redis/ventas/redis.conf` (`maxmemory 256mb`,
`volatile-lru`, `appendonly yes`).
