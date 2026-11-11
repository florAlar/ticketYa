# Modelo de datos — Entrega 1

Fuente: `Backend/infrastructure/mysql/*/init.sql` (esquema real) y `specs/diagramas/modelo.md` (ER global de trabajo).
**Una base de datos relacional por microservicio.** No hay FK físicas entre bases: las referencias entre servicios son **lógicas** (columna `CHAR(36)` con el UUID del recurso del otro servicio).

Leyenda: `PK` clave primaria · `FK` clave foránea física (misma base) · `UK` clave única · `ref` referencia lógica a otro servicio.

## Índice

1. [msvc-usuarios](#1-msvc-usuarios-mysql-usuarios)
2. [msvc-eventos](#2-msvc-eventos-mysql-eventos)
3. [msvc-ventas](#3-msvc-ventas-mysql-ventas)
4. [msvc-entradas](#4-msvc-entradas-mysql-entradas)
5. [msvc-notificaciones](#5-msvc-notificaciones-mysql-notificaciones-e2)
6. [Relaciones entre servicios](#6-relaciones-entre-servicios-referencias-lógicas)
7. [Entidades solapadas](#7-entidades-solapadas-entre-bases)
8. [Preferencias (MongoDB)](#8-preferencias-mongodb-e2)

---

## 1. msvc-usuarios (MySQL `usuarios`)

```mermaid
erDiagram
    USUARIO ||--o{ USUARIO_ROL : tiene
    ROL ||--o{ USUARIO_ROL : asignado

    USUARIO {
        CHAR36 id_usuario PK
        VARCHAR nombre
        VARCHAR apellido
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR ciudad
        DATETIME fecha_registro
        TINYINT activo
    }
    ROL {
        CHAR36 id_rol PK
        VARCHAR nombre UK
    }
    USUARIO_ROL {
        CHAR36 id_usuario PK,FK
        CHAR36 id_rol PK,FK
    }
```

Roles: `CLIENTE`, `ORGANIZADOR`, `ADMIN`, `PERSONAL_ACCESO`. Un usuario puede tener más de un rol.

## 2. msvc-eventos (MySQL `eventos`)

```mermaid
erDiagram
    LUGAR ||--o{ SECTOR : contiene
    LUGAR ||--o{ EVENTO : alberga
    EVENTO ||--o{ PRECIO_SECTOR : define
    SECTOR ||--o{ PRECIO_SECTOR : tarifado
    EVENTO ||--o{ EVENTO_PARTICIPANTE : tiene
    PARTICIPANTE ||--o{ EVENTO_PARTICIPANTE : participa

    LUGAR {
        CHAR36 id PK
        VARCHAR nombre
        VARCHAR direccion
        VARCHAR ciudad
        INT capacidad_total
    }
    SECTOR {
        CHAR36 id PK
        CHAR36 id_lugar FK
        VARCHAR nombre
        INT capacidad
        VARCHAR tipo
        TINYINT es_numerado
        INT filas "solo numerado"
        INT asientos_por_fila "solo numerado"
    }
    EVENTO {
        CHAR36 id PK
        CHAR36 id_organizador "ref usuarios"
        VARCHAR nombre
        TEXT descripcion
        VARCHAR tipo
        DATETIME fecha_inicio
        DATETIME fecha_fin
        VARCHAR estado
        CHAR36 id_lugar FK
        DATETIME fecha_publicacion
        TINYINT es_alta_demanda
    }
    PRECIO_SECTOR {
        CHAR36 id PK
        CHAR36 id_evento FK
        CHAR36 id_sector FK
        DECIMAL precio
        CHAR3 moneda
        TINYINT activo
    }
    PARTICIPANTE {
        CHAR36 id PK
        VARCHAR nombre
        VARCHAR tipo
        TEXT descripcion
    }
    EVENTO_PARTICIPANTE {
        CHAR36 id_evento PK,FK
        CHAR36 id_participante PK,FK
        VARCHAR rol
        INT orden
    }
```

Único: `(id_evento, id_sector)` en `precio_sector`.

**Asientos virtuales:** un sector numerado guarda `filas` y `asientos_por_fila` (capacidad = filas × asientos por fila); los asientos se calculan y no se persisten. Las tablas `asiento` y `precio_asiento` se eliminaron (SEAT-07): el precio se define solo por sector (`precio_sector`). Estados del evento: `BORRADOR`, `PUBLICADO`, `AGOTADO`, `SUSPENDIDO`, `CANCELADO`, `FINALIZADO`.

## 3. msvc-ventas (MySQL `ventas`; Redis para holds e idempotencia)

```mermaid
erDiagram
    RESERVA ||--|{ DETALLE_RESERVA : contiene
    RESERVA ||--o| COMPRA : genera
    COMPRA ||--o{ PAGO : registra
    RESERVA ||--o{ ASIENTO_RESERVADO : toma

    RESERVA {
        CHAR36 id_reserva PK
        CHAR36 id_cliente "ref usuarios"
        CHAR36 id_evento "ref eventos"
        DATETIME fecha_creacion
        DATETIME fecha_expiracion
        VARCHAR estado
        DECIMAL total
    }
    DETALLE_RESERVA {
        CHAR36 id_detalle PK
        CHAR36 id_reserva FK
        CHAR36 id_sector "ref eventos"
        INT cantidad
        DECIMAL precio_unitario
    }
    COMPRA {
        CHAR36 id_compra PK
        CHAR36 id_reserva FK
        CHAR36 id_cliente "ref usuarios"
        DATETIME fecha_compra
        DECIMAL total
        CHAR3 moneda
        VARCHAR estado
        VARCHAR emision_estado
        INT emision_intentos
        VARCHAR emision_ultimo_error
        DATETIME emision_actualizado
    }
    PAGO {
        CHAR36 id_pago PK
        CHAR36 id_compra FK
        VARCHAR proveedor
        VARCHAR id_transaccion_externa UK
        DECIMAL monto
        VARCHAR estado
        DATETIME fecha_pago
    }
    ASIENTO_RESERVADO {
        CHAR36 id_asiento_reservado PK
        CHAR36 id_reserva FK
        CHAR36 id_evento "ref eventos"
        CHAR36 id_sector "ref eventos"
        VARCHAR fila
        INT numero
        VARCHAR estado "RESERVADO|VENDIDO"
    }
    CUPO_SECTOR {
        CHAR36 id_cupo PK
        CHAR36 id_evento "ref eventos"
        CHAR36 id_sector "ref eventos"
        INT capacidad_total
        INT cantidad_disponible
        INT cantidad_reservada
        INT cantidad_vendida
    }
```

`CUPO_SECTOR` tiene clave única `(id_evento, id_sector)` y es la fila sobre la que se aplica `SELECT … FOR UPDATE` (anti doble venta). `ASIENTO_RESERVADO` guarda solo los asientos **tomados** de sectores numerados, con clave única `(id_evento, id_sector, fila, numero)` como segunda defensa contra el doble venta del mismo asiento. No tiene FK física a `DETALLE_RESERVA`: se relacionan por `(id_evento, id_sector)`.

## 4. msvc-entradas (MySQL `entradas`)

```mermaid
erDiagram
    ENTRADA ||--o{ DEVOLUCION : puede_tener
    ENTRADA ||--o{ VALIDACION_ACCESO : registra

    ENTRADA {
        CHAR36 id_entrada PK
        CHAR36 id_compra "ref ventas"
        CHAR36 id_cliente "ref usuarios"
        CHAR36 id_evento "ref eventos"
        CHAR36 id_sector "ref eventos"
        DECIMAL precio
        VARCHAR fila "solo sector numerado"
        INT numero "solo sector numerado"
        VARCHAR codigo_qr UK
        VARCHAR estado
        DATETIME fecha_emision
        DATETIME fecha_validacion
    }
    DEVOLUCION {
        CHAR36 id_devolucion PK
        CHAR36 id_entrada FK
        VARCHAR motivo
        DECIMAL monto
        DATETIME fecha_solicitud
        VARCHAR estado
    }
    VALIDACION_ACCESO {
        CHAR36 id_validacion PK
        CHAR36 id_entrada FK
        CHAR36 id_personal_acceso "ref usuarios"
        DATETIME fecha_validacion
        VARCHAR resultado
        VARCHAR dispositivo
    }
```

`VALIDACION_ACCESO` existe en el esquema pero se usa recién en la Entrega 2. Estados de entrada: `VENDIDA`, `USADA`, `DEVUELTA`, `A_REINTEGRAR`.

## 5. msvc-notificaciones (MySQL `notificaciones`, E2)

```mermaid
erDiagram
    PLANTILLA_NOTIFICACION ||--o{ NOTIFICACION : utiliza
    NOTIFICACION ||--o{ INTENTO_ENVIO : registra

    PLANTILLA_NOTIFICACION {
        CHAR36 id_plantilla PK
        VARCHAR tipo_evento
        VARCHAR canal
        VARCHAR asunto
        TEXT contenido
        TINYINT activa
    }
    NOTIFICACION {
        CHAR36 id_notificacion PK
        CHAR36 id_usuario "ref usuarios"
        CHAR36 id_evento_origen "ref eventos"
        CHAR36 id_plantilla FK
        VARCHAR tipo
        VARCHAR canal
        VARCHAR asunto
        TEXT contenido
        DATETIME fecha_creacion
        DATETIME fecha_envio
        VARCHAR estado
    }
    INTENTO_ENVIO {
        CHAR36 id_intento PK
        CHAR36 id_notificacion FK
        INT numero_intento
        DATETIME fecha_intento
        VARCHAR resultado
        VARCHAR detalle_error
    }
```

## 6. Relaciones entre servicios (referencias lógicas)

No existen FK entre bases. El servicio que consume el dato conoce el UUID y consulta al dueño por API cuando lo necesita.

| Columna (servicio) | Referencia a | Dueño del dato |
|---|---|---|
| `evento.id_organizador` (eventos) | `usuario.id_usuario` | usuarios |
| `reserva.id_cliente`, `compra.id_cliente` (ventas) | `usuario.id_usuario` | usuarios |
| `entrada.id_cliente` (entradas) | `usuario.id_usuario` | usuarios |
| `validacion_acceso.id_personal_acceso` (entradas) | `usuario.id_usuario` | usuarios |
| `notificacion.id_usuario` (notificaciones) | `usuario.id_usuario` | usuarios |
| `reserva.id_evento`, `cupo_sector.id_evento` (ventas) | `evento.id` | eventos |
| `detalle_reserva.id_sector`, `cupo_sector.id_sector` (ventas) | `sector.id` | eventos |
| `entrada.id_evento`, `entrada.id_sector` (entradas) | `evento.id`, `sector.id` | eventos |
| `notificacion.id_evento_origen` (notificaciones) | `evento.id` | eventos |
| `entrada.id_compra` (entradas) | `compra.id_compra` | ventas |

## 7. Entidades solapadas entre bases

La consigna pide identificar entidades que aparecen en más de una base. En TicketYa son **referencias por identificador y copias puntuales de datos**, nunca tablas duplicadas:

| Concepto | Dónde vive la fuente de verdad | Dónde aparece más | Tratamiento |
|---|---|---|---|
| **Usuario / cliente** | usuarios | ventas, entradas, eventos (organizador), notificaciones | Solo el UUID. El nombre o email no se copian |
| **Evento** | eventos | ventas (`reserva`, `cupo_sector`), entradas, notificaciones | Solo el UUID. Datos de presentación (nombre, lugar) se obtienen por Feign (entradas → eventos, best-effort) |
| **Sector** | eventos | ventas (`detalle_reserva`, `cupo_sector`), entradas | UUID. La **capacidad** se copia a `cupo_sector.capacidad_total` al publicar el evento |
| **Precio** | eventos (`precio_sector`) | ventas (`detalle_reserva.precio_unitario`), entradas (`entrada.precio`) | Copia **congelada** al reservar y al emitir: un cambio posterior de precio no altera lo ya vendido |
| **Compra** | ventas | entradas (`entrada.id_compra`) | Solo el UUID; sirve de clave de idempotencia en la emisión |
| **Cupo / stock** | ventas (`cupo_sector`) | eventos (`sector.capacidad`) | Eventos define la capacidad; ventas mantiene el stock vivo (disponible, reservada, vendida) |
| **Asiento** | layout en eventos (`filas`, `asientos_por_fila`); ocupación en ventas (`asiento_reservado`) | entradas (`entrada.fila`, `entrada.numero`) | Eventos define la grilla; ventas registra los tomados; entradas guarda fila/número de cada entrada emitida |

Consistencia entre bases: se resuelve con llamadas REST síncronas, idempotencia (`Idempotency-Key`, emisión por compra) y reintentos; no hay transacciones distribuidas.

## 8. Preferencias (MongoDB, E2)

`msvc-preferencias` usa MongoDB (`preferencias_clientes`, `interacciones_eventos`). Esquema y diagrama: `Backend/specs/diagramas/modelo-mongodb.md`. Referencia lógica a usuarios y eventos por UUID, igual que el resto.
