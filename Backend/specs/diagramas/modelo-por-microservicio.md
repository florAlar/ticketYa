# Modelo entidad-relación por microservicio

## msvc-usuarios

```mermaid
erDiagram
    USUARIO {
        CHAR36 id_usuario PK
        VARCHAR nombre
        VARCHAR apellido
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR ciudad
        DATETIME fecha_registro
        BOOL activo
    }
    ROL {
        CHAR36 id_rol PK
        VARCHAR nombre UK
    }
    USUARIO_ROL {
        CHAR36 id_usuario PK, FK
        CHAR36 id_rol PK, FK
    }

    USUARIO ||--o{ USUARIO_ROL : tiene
    ROL ||--o{ USUARIO_ROL : asignado
```

## msvc-eventos

```mermaid
erDiagram
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
        BOOL es_numerado
        INT filas
        INT asientos_por_fila
    }
    ASIENTO {
        CHAR36 id PK
        CHAR36 id_sector FK
        VARCHAR fila
        INT numero
        VARCHAR codigo
    }
    EVENTO {
        CHAR36 id PK
        CHAR36 id_organizador "ref lógica usuarios.usuario"
        CHAR36 id_lugar FK
        VARCHAR nombre
        TEXT descripcion
        VARCHAR tipo
        DATETIME fecha_inicio
        DATETIME fecha_fin
        VARCHAR estado
        DATETIME fecha_publicacion
        BOOL es_alta_demanda
    }
    PRECIO_SECTOR {
        CHAR36 id PK
        CHAR36 id_evento FK
        CHAR36 id_sector FK
        DECIMAL precio
        CHAR moneda
        BOOL activo
    }
    PRECIO_ASIENTO {
        CHAR36 id PK
        CHAR36 id_evento FK
        CHAR36 id_asiento FK
        DECIMAL precio
        CHAR moneda
        BOOL activo
    }
    PARTICIPANTE {
        CHAR36 id PK
        VARCHAR nombre
        VARCHAR tipo
        TEXT descripcion
    }
    EVENTO_PARTICIPANTE {
        CHAR36 id_evento PK, FK
        CHAR36 id_participante PK, FK
        VARCHAR rol
        INT orden
    }
    EXT_USUARIO["usuario (msvc-usuarios)"] {
        CHAR36 id_usuario PK
    }

    LUGAR ||--o{ SECTOR : contiene
    LUGAR ||--o{ EVENTO : alberga
    SECTOR ||--o{ ASIENTO : tiene
    EVENTO ||--o{ PRECIO_SECTOR : define
    SECTOR ||--o{ PRECIO_SECTOR : tarifado
    EVENTO ||--o{ PRECIO_ASIENTO : define
    ASIENTO ||--o{ PRECIO_ASIENTO : tarifado
    EVENTO ||--o{ EVENTO_PARTICIPANTE : tiene
    PARTICIPANTE ||--o{ EVENTO_PARTICIPANTE : participa
    EVENTO }o..|| EXT_USUARIO : "id_organizador (ref lógica)"
```

## msvc-ventas

```mermaid
erDiagram
    RESERVA {
        CHAR36 id_reserva PK
        CHAR36 id_cliente "ref lógica usuarios.usuario"
        CHAR36 id_evento "ref lógica eventos.evento"
        DATETIME fecha_creacion
        DATETIME fecha_expiracion
        VARCHAR estado
        DECIMAL total
    }
    DETALLE_RESERVA {
        CHAR36 id_detalle PK
        CHAR36 id_reserva FK
        CHAR36 id_sector "ref lógica eventos.sector"
        INT cantidad
        DECIMAL precio_unitario
    }
    ASIENTO_RESERVADO {
        CHAR36 id_asiento_reservado PK
        CHAR36 id_reserva FK
        CHAR36 id_evento "ref lógica eventos.evento; UK compuesta (id_evento, id_sector, fila, numero)"
        CHAR36 id_sector "ref lógica eventos.sector"
        VARCHAR fila
        INT numero
        VARCHAR estado
    }
    COMPRA {
        CHAR36 id_compra PK
        CHAR36 id_reserva FK
        CHAR36 id_cliente "ref lógica usuarios.usuario"
        DATETIME fecha_compra
        DECIMAL total
        CHAR moneda
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
    CUPO_SECTOR {
        CHAR36 id_cupo PK
        CHAR36 id_evento "ref lógica eventos.evento; UK compuesta (id_evento, id_sector)"
        CHAR36 id_sector "ref lógica eventos.sector"
        INT capacidad_total
        INT cantidad_disponible
        INT cantidad_reservada
        INT cantidad_vendida
    }
    EXT_USUARIO["usuario (msvc-usuarios)"] {
        CHAR36 id_usuario PK
    }
    EXT_EVENTO["evento (msvc-eventos)"] {
        CHAR36 id PK
    }
    EXT_SECTOR["sector (msvc-eventos)"] {
        CHAR36 id PK
    }

    RESERVA ||--|{ DETALLE_RESERVA : contiene
    RESERVA ||--o{ ASIENTO_RESERVADO : bloquea
    RESERVA |o--o{ COMPRA : origina
    COMPRA ||--o{ PAGO : registra
    RESERVA }o..|| EXT_USUARIO : "id_cliente (ref lógica)"
    COMPRA }o..|| EXT_USUARIO : "id_cliente (ref lógica)"
    RESERVA }o..|| EXT_EVENTO : "id_evento (ref lógica)"
    CUPO_SECTOR }o..|| EXT_EVENTO : "id_evento (ref lógica)"
    ASIENTO_RESERVADO }o..|| EXT_EVENTO : "id_evento (ref lógica)"
    ASIENTO_RESERVADO }o..|| EXT_SECTOR : "id_sector (ref lógica)"
    DETALLE_RESERVA }o..|| EXT_SECTOR : "id_sector (ref lógica)"
    CUPO_SECTOR }o..|| EXT_SECTOR : "id_sector (ref lógica)"
```

## msvc-entradas

```mermaid
erDiagram
    ENTRADA {
        CHAR36 id_entrada PK
        CHAR36 id_compra "ref lógica ventas.compra"
        CHAR36 id_cliente "ref lógica usuarios.usuario"
        CHAR36 id_evento "ref lógica eventos.evento"
        CHAR36 id_sector "ref lógica eventos.sector"
        VARCHAR fila
        INT numero
        DECIMAL precio
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
        CHAR36 id_personal_acceso "ref lógica usuarios.usuario"
        DATETIME fecha_validacion
        VARCHAR resultado
        VARCHAR dispositivo
    }
    EXT_COMPRA["compra (msvc-ventas)"] {
        CHAR36 id_compra PK
    }
    EXT_USUARIO["usuario (msvc-usuarios)"] {
        CHAR36 id_usuario PK
    }
    EXT_EVENTO["evento (msvc-eventos)"] {
        CHAR36 id PK
    }
    EXT_SECTOR["sector (msvc-eventos)"] {
        CHAR36 id PK
    }

    ENTRADA ||--o{ DEVOLUCION : "puede tener"
    ENTRADA ||--o{ VALIDACION_ACCESO : registra
    ENTRADA }o..|| EXT_COMPRA : "id_compra (ref lógica)"
    ENTRADA }o..|| EXT_USUARIO : "id_cliente (ref lógica)"
    VALIDACION_ACCESO }o..|| EXT_USUARIO : "id_personal_acceso (ref lógica)"
    ENTRADA }o..|| EXT_EVENTO : "id_evento (ref lógica)"
    ENTRADA }o..|| EXT_SECTOR : "id_sector (ref lógica)"
```

## msvc-notificaciones

```mermaid
erDiagram
    PLANTILLA_NOTIFICACION {
        CHAR36 id_plantilla PK
        VARCHAR tipo_evento
        VARCHAR canal
        VARCHAR asunto
        TEXT contenido
        BOOL activa
    }
    NOTIFICACION {
        CHAR36 id_notificacion PK
        CHAR36 id_usuario "ref lógica usuarios.usuario"
        CHAR36 id_evento_origen "ref lógica eventos.evento"
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
    EXT_USUARIO["usuario (msvc-usuarios)"] {
        CHAR36 id_usuario PK
    }
    EXT_EVENTO["evento (msvc-eventos)"] {
        CHAR36 id PK
    }

    PLANTILLA_NOTIFICACION |o--o{ NOTIFICACION : utiliza
    NOTIFICACION ||--o{ INTENTO_ENVIO : registra
    NOTIFICACION }o..|| EXT_USUARIO : "id_usuario (ref lógica)"
    NOTIFICACION }o..o| EXT_EVENTO : "id_evento_origen (ref lógica)"
```

## msvc-preferencias (MongoDB)

Ver [`modelo-mongodb.md`](./modelo-mongodb.md).
