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
| Lugar Estadio Más Monumental | `c0000000-0000-4000-8000-000000000001` |
| Sector Campo | `…0011` |
| Sector Platea | `…0012` |
| Evento Metallica PUBLICADO | `d0000000-0000-4000-8000-000000000001` |
| Evento UCL Final SUSPENDIDO | `…0002` |
| Evento Rock in Rio CANCELADO | `…0003` |
| Participante Metallica | `p0000000-0000-4000-8000-000000000001` |
| Participante Gojira | `…0002` |
| Participante Real Madrid | `…0003` |
| Participante Manchester City | `…0004` |
| Participante Iron Maiden | `…0005` |
| Participante Ghost | `…0006` |
| Compra Metallica | `f0000000-0000-4000-8000-000000000021` |
| Entrada Metallica QR-SEED-METALLICA-001 | `aa000000-0000-4000-8000-000000000001` |

- FK **físicas** solo dentro de la misma DB.
- Cross-msvc: columnas comentadas `ref lógica` (CHAR 36), sin FK.
- Para re-aplicar: borrar volumen Docker del MySQL correspondiente, o script `eventos/migrations/03-reseed-clean-demo.sql` (+ ventas/entradas).

## Migraciones (`migrations/`)

Los `02-*` y `03-*` viven en `<db>/migrations/` y **no** corren solos: MySQL solo ejecuta los `.sql` de primer nivel de `docker-entrypoint-initdb.d`, y los ejecuta en orden alfabético (un `02-*` correría antes que `init.sql`). Aplicarlos a mano sobre un volumen existente.
