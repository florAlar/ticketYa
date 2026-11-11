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
- **Un único `init.sql` por base**: es la fuente de verdad del esquema y del seed demo. No hay scripts de migración sueltos.

## Re-aplicar / actualizar el esquema

`init.sql` solo corre en un volumen vacío. Si cambia el esquema (o el volumen quedó de una versión anterior), se recrea la base desde cero:

```bash
cd Backend
docker compose down -v      # borra los volúmenes (se pierden los datos locales)
docker compose up -d --build
```

Para recrear una sola base: `docker compose rm -sf db-mysql-<servicio>`, borrar su volumen (`docker volume ls`) y `docker compose up -d`.

Los `init.sql` son idempotentes (`CREATE TABLE IF NOT EXISTS` y `ON DUPLICATE KEY UPDATE`), por lo que también se pueden volver a ejecutar a mano sobre una base ya creada para recargar el seed. Eso **no** agrega columnas a tablas existentes: ante un cambio de esquema, recrear el volumen.

## Asientos virtuales (sectores numerados)

- El sector numerado guarda `filas` y `asientos_por_fila`; su capacidad es el producto. No hay filas de asiento libres en ninguna tabla (las tablas `asiento` y `precio_asiento` se eliminaron).
- Solo `ventas.asiento_reservado` guarda asientos, y únicamente los **tomados** (RESERVADO | VENDIDO). Clave única: evento + sector + fila + número.
- Seeds: los 3 sectores numerados (Platea `…012` 100×250, Cadeira `…014` 80×250, Club Wembley `…016` 72×250) mantienen su capacidad (25.000 / 20.000 / 18.000).
