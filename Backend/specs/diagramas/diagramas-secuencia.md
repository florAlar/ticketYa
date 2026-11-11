# Diagramas de secuencia — Compra de entradas

Fuente: `msvc-ventas` (`VentaServiceImpl`, `EmisionEntradasService`, `ReservaExpirationJob`) y [`reglas-de-negocio.md`](../reglas-de-negocio.md) §5–§6.
Las rutas son las públicas del gateway (`/api/...`).

## Estados que intervienen

| Entidad | Estados |
|---|---|
| Reserva | `PENDIENTE` → `CONFIRMADA` → `COMPRADA` \| `CANCELADA` \| `EXPIRADA` |
| Compra | `PENDIENTE_PAGO` → `APROBADA` \| `RECHAZADA` |
| Pago | `APROBADO` \| `RECHAZADO` |
| Emisión de QR (`compra.emisionEstado`) | `NO_REQUIERE` → `PENDIENTE` → `EMITIDA` \| `FALLIDA` |
| Cupo por sector | `disponible` ⇄ `reservada` → `vendida` |
| Asiento de sector numerado (`asiento_reservado`) | `RESERVADO` → `VENDIDO` (o se elimina al liberarse) |

## 1. Compra exitosa (reserva → compra → pago aprobado → entradas con QR)

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant GW as API Gateway
    participant V as msvc-ventas
    participant R as Redis
    participant E as msvc-eventos
    participant N as msvc-entradas

    Note over C,N: 1) Reserva temporal (TTL 10 min)
    C->>GW: POST /api/reservas {idEvento, items[{idSector, cantidad | asientos[{fila,numero}]}]}
    GW->>V: POST /reservas (X-User-Id)
    V->>E: GET /eventos/{id} (precio del sector)
    E-->>V: evento + precios
    V->>V: validar límite de 6 entradas por cliente y evento
    V->>V: SELECT cupo_sector FOR UPDATE
    V->>V: disponible -= n, reservada += n
    V->>R: hold ventas:reserva:{id} (TTL)
    V-->>C: 201 reserva PENDIENTE (vence en 10 min)

    Note over C,N: 2) Confirmación y compra
    C->>GW: POST /api/reservas/{id}/confirmacion
    GW->>V: confirmar
    V-->>C: reserva CONFIRMADA
    C->>GW: POST /api/compras {idReserva} + Idempotency-Key
    GW->>V: crear compra
    V->>V: verificar reserva activa y sin compra previa
    V-->>C: 201 compra PENDIENTE_PAGO

    Note over C,N: 3) Pago aprobado (pasarela mock)
    C->>GW: POST /api/compras/{id}/pagos {resultado: APROBAR}
    GW->>V: procesar pago
    V->>V: pago APROBADO, compra APROBADA, reserva COMPRADA
    V->>V: reservada -= n, vendida += n
    V->>R: liberar hold
    V->>V: emisionEstado = PENDIENTE
    V-->>C: 201 pago APROBADO

    Note over C,N: 4) Emisión de entradas (tras el commit del pago)
    V->>N: POST /entradas/emision {idCompra, ...}
    N->>N: crear entradas con codigoQR único (idempotente por compra)
    N-->>V: entradas emitidas
    V->>V: emisionEstado = EMITIDA

    C->>GW: GET /api/entradas
    GW->>N: listar entradas del cliente
    N-->>C: entradas VENDIDA con QR
```

Si el paso 4 falla (entradas no responde): se reintenta hasta 3 veces de inmediato; si sigue fallando, la compra queda `emisionEstado=PENDIENTE` y un job la reprocesa cada 30 s (máx. 20 intentos, luego `FALLIDA`). La emisión es idempotente: reintentar no duplica entradas. El pago y el cupo vendido **no** se revierten.

## 2. Pago rechazado

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant GW as API Gateway
    participant V as msvc-ventas
    participant R as Redis

    Note over C,R: Precondición: reserva CONFIRMADA y compra PENDIENTE_PAGO
    C->>GW: POST /api/compras/{id}/pagos {resultado: RECHAZAR}
    GW->>V: procesar pago
    V->>V: pago RECHAZADO
    V->>V: compra RECHAZADA
    V->>V: SELECT cupo_sector FOR UPDATE
    V->>V: reservada -= n, disponible += n (cupo liberado)
    V->>V: reserva CANCELADA
    V->>R: liberar hold
    V-->>C: 201 pago RECHAZADO
    Note over C,R: No se emiten entradas. Para volver a intentar se crea una nueva reserva
```

Limitación conocida: `POST /pagos/{id}/reintentos` solo reabre un pago rechazado si la reserva sigue activa; como el rechazo libera el cupo y cancela la reserva, tras un rechazo hay que crear una reserva nueva (la colección Postman lo documenta como `409`).

## 3. Vencimiento de la reserva

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant J as Job de expiración (cada 30 s)
    participant GW as API Gateway
    participant V as msvc-ventas
    participant R as Redis

    Note over C,R: Reserva PENDIENTE/CONFIRMADA, el cliente no completa el pago
    J->>V: expirarReservasVencidas()
    V->>V: buscar reservas activas con fechaExpiracion < ahora
    alt la compra asociada ya estaba APROBADA
        V->>V: alinear reserva a COMPRADA (no se libera cupo)
    else sin compra aprobada
        V->>V: compra PENDIENTE_PAGO → RECHAZADA (si existía)
        V->>V: SELECT cupo_sector FOR UPDATE
        V->>V: reservada -= n, disponible += n
        V->>V: reserva EXPIRADA
    end
    V->>R: liberar hold

    C->>GW: POST /api/compras/{id}/pagos (tarde)
    GW->>V: procesar pago
    V-->>C: 409 Conflict (reserva no activa o expirada)
```

La expiración también ocurre de forma *lazy*: al consultar `GET /reservas/{id}` con el TTL vencido, la reserva se expira en el momento. Renovar (`POST /reservas/{id}/renovacion`) extiende el vencimiento en MySQL y en Redis.

## 4. Devolución (complementa el circuito)

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    actor A as Admin
    participant GW as API Gateway
    participant N as msvc-entradas
    participant V as msvc-ventas

    C->>GW: POST /api/entradas/{id}/devoluciones
    GW->>N: solicitar devolución
    N->>N: validar: entrada VENDIDA, no usada, dentro de 72 h
    N-->>C: 201 devolución PENDIENTE

    A->>GW: POST /api/devoluciones/{id}/aprobacion
    GW->>N: aprobar
    N->>N: entrada DEVUELTA, QR invalidado
    N->>V: POST /eventos/{idEvento}/cupos/liberacion {idSector, cantidad: 1}
    V->>V: SELECT cupo_sector FOR UPDATE (vendida -= 1, disponible += 1)
    V-->>N: 200
    N-->>A: devolución APROBADA
    Note over N,V: Si ventas falla, la aprobación hace rollback local
```

## 5. Entrada numerada (asiento concreto)

Los sectores pueden ser **generales** (se reserva por cantidad) o **numerados** (se reservan asientos concretos: fila + número). En un sector numerado el layout se define con `filas` × `asientosPorFila` y la capacidad es el producto; los asientos son **virtuales** (se calculan, no se guardan los libres). En `msvc-ventas` solo se persisten los asientos **tomados**, en `asiento_reservado` (estado `RESERVADO` o `VENDIDO`), con clave única `(id_evento, id_sector, fila, numero)`.

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant GW as API Gateway
    participant V as msvc-ventas
    participant N as msvc-entradas

    Note over C,N: 1) Reserva de asientos concretos
    C->>GW: POST /api/reservas {idEvento, items[{idSector, asientos:[{fila:"C", numero:14}]}]}
    GW->>V: crear reserva
    V->>V: validar asientos contra el layout (filas x asientosPorFila) y que no estén repetidos
    V->>V: SELECT cupo_sector FOR UPDATE (serializa el sector)
    V->>V: verificar que ningún asiento esté tomado
    alt todos los asientos libres
        V->>V: INSERT asiento_reservado (RESERVADO) + disponible -= n, reservada += n
        V-->>C: 201 reserva PENDIENTE (TTL 10 min) con asientos
    else algún asiento ya RESERVADO o VENDIDO
        V-->>C: 409 asiento no disponible
    end

    Note over C,N: 2) Pago aprobado
    C->>GW: POST /api/compras/{id}/pagos {resultado: APROBAR}
    GW->>V: procesar pago
    V->>V: asientos RESERVADO → VENDIDO, reservada -= n, vendida += n
    V->>N: POST /entradas/emision {items[{idSector, asientos:[{fila,numero}]}]}
    N->>N: una entrada por asiento, con fila, numero y codigoQR único
    N-->>V: entradas emitidas

    Note over C,N: 3) Pago rechazado, vencimiento o cancelación
    V->>V: eliminar asientos de la reserva (quedan libres) + liberar cupo
```

Reglas y errores:
- Sector numerado sin asientos, asiento inexistente en el layout o repetido en el pedido → `400`.
- Sector general con asientos → `400`.
- Asiento ya tomado (`RESERVADO`/`VENDIDO`) → `409`.
- Pago rechazado, reserva vencida o cancelada: los asientos se eliminan de `asiento_reservado` y vuelven a estar libres, igual que el cupo.
- **Devolución aprobada:** `POST /eventos/{id}/cupos/liberacion` con `{idSector, asientos:[{fila,numero}]}` (deben figurar `VENDIDO`) libera esos asientos y devuelve el cupo.
- `GET /eventos/{id}/disponibilidad` informa por sector `asientosOcupados[{fila,numero,estado}]` para que el frontend marque los no disponibles.

## 6. Control de acceso: validar el QR en la puerta

El personal de acceso escanea el QR y `msvc-entradas` decide. Un QR solo puede ingresar una vez, aun con escaneos simultáneos en varias puertas: la fila de la entrada se lee con `SELECT … FOR UPDATE`, así que el segundo escaneo espera, ve la entrada `USADA` y se rechaza.

```mermaid
sequenceDiagram
    autonumber
    actor P as Personal de acceso
    participant GW as API Gateway
    participant N as msvc-entradas
    participant E as msvc-eventos
    participant DB as MySQL entradas

    P->>GW: POST /api/eventos/{idEvento}/validaciones {codigoQR, dispositivo}
    GW->>GW: valida JWT y rol (PERSONAL_ACCESO o ADMIN)
    GW->>N: POST /eventos/{idEvento}/validaciones (X-User-Id, X-User-Role)
    N->>E: GET /eventos/{idEvento} (estado del evento; best-effort)
    N->>DB: SELECT entrada WHERE codigo_qr = ? FOR UPDATE
    alt no existe ninguna entrada con ese QR
        N-->>P: 404 (no se registra: no hay entrada)
    else existe
        alt la entrada es de otro evento
            N->>DB: INSERT validacion_acceso (OTRO_EVENTO)
            N-->>P: 409 OTRO_EVENTO (sin datos de la entrada)
        else ya USADA
            N->>DB: INSERT validacion_acceso (YA_USADA)
            N-->>P: 409 YA_USADA
        else DEVUELTA o A_REINTEGRAR
            N->>DB: INSERT validacion_acceso (NO_VALIDA)
            N-->>P: 409 NO_VALIDA
        else evento CANCELADO
            N->>DB: INSERT validacion_acceso (EVENTO_CANCELADO)
            N-->>P: 409 EVENTO_CANCELADO
        else VENDIDA y evento vigente
            N->>DB: UPDATE entrada SET estado = USADA, fecha_validacion = ahora
            N->>DB: INSERT validacion_acceso (VALIDA, id_personal_acceso)
            N-->>P: 200 VALIDA + entrada (evento, sector, asiento)
        end
    end
```

- **Auditoría:** todo intento sobre una entrada existente queda en `validacion_acceso` (resultado, quién escaneó, dispositivo, fecha), aceptado o rechazado. `GET /api/eventos/{idEvento}/validaciones` lo lista (ADMIN; ORGANIZADOR solo el dueño del evento).
- **Si `msvc-eventos` no responde** no se bloquea la puerta: se confía en el estado de la entrada (una entrada de un evento cancelado ya está `A_REINTEGRAR`).
- **Simplificación:** cualquier `PERSONAL_ACCESO` puede validar cualquier evento; no se modela la asignación de eventos por persona.

## 7. Reportes del organizador

El organizador (o un ADMIN) consulta cómo viene su evento. `msvc-ventas` arma el reporte: lo que se vendió sale de su propia base y lo que ocurrió después (devoluciones, ingresos) se pide a `msvc-entradas`.

```mermaid
sequenceDiagram
    autonumber
    actor O as Organizador / Admin
    participant GW as API Gateway
    participant V as msvc-ventas
    participant E as msvc-eventos
    participant N as msvc-entradas

    O->>GW: GET /api/reportes/eventos/{idEvento}
    GW->>GW: valida JWT y rol (ORGANIZADOR o ADMIN)
    GW->>V: GET /reportes/eventos/{idEvento} (X-User-Id, X-User-Role)
    V->>E: GET /eventos/{idEvento} (sectores y dueño)
    alt evento inexistente
        V-->>O: 404
    else ORGANIZADOR que no es el dueño
        V-->>O: 403
    else permitido
        V->>V: compras APROBADAS y capacidad por sector (SQL)
        V->>N: GET /eventos/{idEvento}/entradas/resumen (devueltas, usadas, a reintegrar, montos)
        alt eventos o entradas no responden
            V-->>O: 503 (no se entrega un reporte parcial)
        else
            V-->>O: 200 totales + detalle por sector
        end
    end
```

- **Definiciones:** vigentes = vendidas − devueltas − a reintegrar; ocupación = vigentes / capacidad; recaudación neta = bruta − devuelta − a reintegrar.
- **Por qué dos servicios:** las ventas y los cupos viven en `msvc-ventas`; las devoluciones y los ingresos, en `msvc-entradas`. Cada servicio tiene su base y no se comparten tablas.

## Garantía de no doble venta

Hay dos defensas combinadas:
1. **Bloqueo pesimista** (`SELECT … FOR UPDATE`) sobre `cupo_sector`: serializa las reservas concurrentes de un mismo sector. Si N clientes piden el último lugar (o el mismo asiento) a la vez, uno obtiene la reserva y los demás reciben `409`.
2. **Clave única** `(id_evento, id_sector, fila, numero)` en `asiento_reservado`: segunda defensa a nivel de base para el mismo asiento.

Evidencia: [`docs/testing/concurrencia.md`](../../docs/testing/concurrencia.md) (último cupo y mismo asiento, con MySQL real) y `ReservaAsientoConcurrenciaTest` (20 hilos compiten por el mismo asiento; un solo éxito).
