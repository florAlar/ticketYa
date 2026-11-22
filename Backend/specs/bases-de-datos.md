# Bases De Datos
- **MySQL:** Base de datos relacional para persistencia transaccional.
    - `msvc-usuarios`
    - `msvc-eventos`
    - `msvc-ventas`
    - `msvc-entradas`
    - `msvc-notificaciones` (E2)

- **MongoDB:** Base de datos NoSQL orientada a documentos.
    - `msvc-preferencias`

- **Redis:** Almacenamiento en memoria para operaciones de alta velocidad.
    - `msvc-ventas`: stock/cupos y retención temporal de reservas (hecho); fila virtual, posiciones, tandas y tokens temporales (hecho en FIL-01…FIL-04). La fila es **Redis-only** con claves `fila:*` (prefijo configurable por `ticketya.fila.key-prefix` / `TICKETYA_FILA_PREFIX`; decisión D1 de `specs/fila-virtual.md`): no hay tabla MySQL ni mensajes Rabbit para ella, y su pérdida no afecta las reservas confirmadas en MySQL.