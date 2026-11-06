# MySQL init — TicketYa

Scripts montados en `docker-entrypoint-initdb.d` de cada `db-mysql-*`.
Solo corren en **primer arranque** del volumen vacío.

## Archivos

| DB | Script |
|---|---|
| usuarios | `usuarios/init.sql` |
| eventos | `eventos/init.sql` |
| ventas | `ventas/init.sql` |
| entradas | `entradas/init.sql` |
| notificaciones | `notificaciones/init.sql` |

Mongo preferencias: `../mongodb/preferencias/` (aparte). No hay loader por msvc Java.

## IDs seed compartidos (refs lógicas)

| Concepto | UUID |
|---|---|
| Rol CLIENTE | `a0000000-0000-4000-8000-000000000001` |
| Rol ORGANIZADOR | `…0002` |
| Rol ADMIN | `…0003` |
| Rol PERSONAL_ACCESO | `…0004` |
| User demo@ | `b0000000-0000-4000-8000-000000000001` |
| User org@ | `…0002` |
| User admin@ | `…0003` |
| User acceso@ | `…0004` |
| Lugar Anfiteatro | `c0000000-0000-4000-8000-000000000001` |
| Sector General | `…0011` |
| Sector Platea | `…0012` |
| Evento Indie PUBLICADO | `d0000000-0000-4000-8000-000000000001` |
| Evento Hamlet PUBLICADO | `…0002` |
| Evento Festival CANCELADO | `…0003` |
| Compra indie | `f0000000-0000-4000-8000-000000000021` |
| Entrada indie QR-SEED-INDIE-001 | `aa000000-0000-4000-8000-000000000001` |

- FK **físicas** solo dentro de la misma DB.
- Cross-msvc: columnas comentadas `ref lógica` (CHAR 36), sin FK.
- Para re-aplicar: borrar volumen Docker del MySQL correspondiente.
