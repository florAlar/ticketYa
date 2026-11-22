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
- **Un único `init.sql` por base**: es la fuente de verdad del esquema y del seed demo para volúmenes nuevos. `notificaciones/migrations/` conserva migraciones **manuales** para volúmenes ya creados; el entrypoint de MySQL no las ejecuta (solo procesa `init.sql` en el primer arranque).

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

## Seed "plataforma en funcionamiento"

Además del seed mínimo, cada `init.sql` (y `mongodb/preferencias/init.js`) trae al final un bloque
que simula una plataforma con movimiento: eventos en todos los estados, ventas, entradas y notificaciones.
Mismo estilo que el catálogo base: recitales de metal y deportes extremos. Los eventos del catálogo base no
se modificaron (nombres, bandas e imágenes); solo se ajustaron sus cupos para que aparezcan los badges de cupo.

**Cuentas nuevas** (contraseña `demo1234`, salvo `metal@` = `org1234`)

| Email | Rol | Nota |
|---|---|---|
| `lucia@`, `martin@`, `sofia@`, `nicolas@`, `camila@`, `tomas@` `ticketya.com` | CLIENTE | `b0000000-0000-4000-8000-0000000000{05..10}` (ids decimales `…05` a `…10`) |
| `metal@ticketya.com` | ORGANIZADOR | segundo organizador (`…11`): sus eventos no los ve `org@` |
| `inactivo@ticketya.com` | CLIENTE | cuenta desactivada (`…12`) |

**Eventos agregados** (`d0000000-0000-4000-8000-0000000000{16..21}`, ids hexadecimales)

| Estado | Eventos |
|---|---|
| `PUBLICADO` con cupo casi agotado | Sepultura (< 20 libres, «Últimos lugares»), Mastodon (< 50), Judas Priest (< 100), Red Bull Rampage (< 100) |
| `PUBLICADO` con fecha más próxima | Sepultura (2027-01-23), Mastodon (2027-01-30), Supercross Indoor (2027-02-20) |
| `PUBLICADO` con poca venta | X Games |
| `AGOTADO` | Lamb of God (más Oasis del catálogo base) |
| `SUSPENDIDO` | WRC Rally de Argentina (más UCL Final del catálogo base) |
| `FINALIZADO` | Motocross de las Naciones, Pantera (venta cerrada; fecha futura, sin entradas usadas) |
| `BORRADOR` | Megadeth (completo, sin publicar), Metal Fest Argentina (incompleto, sin sectores) |

También se ajustaron los cupos de Iron Maiden, Rammstein, Foo Fighters, Boca vs River y UFC (catálogo base).

**Ventas y entradas** (`f0000000-…` y `aa000000-…`): 21 compras de 7 clientes con compras `APROBADA` con
emisión `EMITIDA`/`PENDIENTE`/`FALLIDA`, una `RECHAZADA` (pago rechazado) y una reserva `EXPIRADA`; 22 entradas
(`VENDIDA`, `DEVUELTA`, `A_REINTEGRAR`), 5 devoluciones (`PENDIENTE`, `APROBADA`, `RECHAZADA`,
`EN_CURSO`) y los asientos vendidos de los sectores numerados.

**Notificaciones y Mongo:** 12 notificaciones (leídas, sin leer, `PENDIENTE` y `FALLIDA`), preferencias de los
6 clientes nuevos y su historial de compras.

Notas:
- `cantidad_vendida` de los cupos es la cifra "de mercado" (decenas de miles) y **no** coincide con la cantidad
  de filas de `entrada`: el seed solo materializa unas pocas entradas, igual que el catálogo base.
- Las fechas son fijas y están pensadas para el 2026-11-21: Supercross, Sepultura, Mastodon, Lamb of God y los
  dos `FINALIZADO` quedan en el pasado con el tiempo. Se actualizan editando el `init.sql` de eventos.
