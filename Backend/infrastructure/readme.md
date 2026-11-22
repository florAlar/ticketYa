esta carpeta existe para centralizar la inicializacion de las DB y carga inicial de datos, de esta manera queda desacoplada la infraestructura de la funcionalidad de cada ms.
Archivo	Responsabilidad
mysql/usuarios/init.sql	Schema + seed mínimo usuarios (rol, usuario, usuario_rol).
mysql/eventos/init.sql	Schema + seed mínimo eventos (lugar, sector con layout filas×asientos_por_fila, evento, precios).
mysql/ventas/init.sql	Schema + seed mínimo ventas (reserva, detalle, compra, pago, cupo, asiento_reservado).
mysql/entradas/init.sql	Schema + seed mínimo entradas (entrada con fila/numero, devolucion, validacion_acceso).
mysql/notificaciones/init.sql	Schema + seed mínimo notificaciones (plantilla; tablas notif/intento).
mysql/README.md	Mapa de UUIDs seed y notas de montaje.
mongodb/preferencias/init.js	Colecciones + índices + seed mínimo preferencias (según modelo-mongodb.md).
mongodb/README.md	Notas de montaje Mongo.
redis/ventas/redis.conf	Conf Redis ventas (AOF, maxmemory).
redis/README.md	Notas de montaje Redis.
nginx/nginx.conf	SPA + proxy `/api/` → `ms-gateway`.

Los `init.sql` se montan en cada `db-mysql-*` vía `docker-entrypoint-initdb.d` (solo primer arranque del volumen). No hay DataLoader Java por microservicio.
El `init.js` de Mongo se monta igual en `db-mongo-preferencias` (`docker-entrypoint-initdb.d`, solo primer arranque).
El `redis.conf` se monta en `db-redis-ventas` y se arranca con `redis-server /usr/local/etc/redis/redis.conf`.

## Integración del frontend y Nginx

Se incorpora Nginx como servidor web para publicar el frontend de TicketYa y actuar como proxy reverso hacia el API Gateway.

### 1. Preparación de infraestructura

Dentro de `infrastructure/` está la configuración de Nginx:

```text
infrastructure/
├── nginx/
│   └── nginx.conf
└── docker-compose.yml
```

El archivo `nginx.conf` define:
- La publicación de los archivos estáticos del frontend.
- La redirección de las solicitudes `/api/` hacia el API Gateway.
- El fallback SPA para las rutas del frontend.

La imagen del frontend usa una copia embebida (`Frontend/conf/nginx-default.conf`), alineada a mano con este archivo porque el build de Docker no puede montar rutas del Backend.

### 2. Incorporación del frontend

El frontend React vive en `Frontend/`:

```text
Frontend/
├── src/
├── public/
├── conf/nginx-default.conf
├── package.json
└── Dockerfile
```

El Dockerfile usa una construcción de múltiples etapas:

1. **Build:** Node.js instala las dependencias y compila React, generando los archivos estáticos en `dist/`.
2. **Producción:** Nginx sirve esos archivos mediante HTTP.

### 3. Contenedor y comunicación

No es necesario ejecutar React y Nginx en dos contenedores independientes para producción. Ambos forman parte del mismo contenedor de frontend: React se compila previamente y Nginx publica el resultado.

El contenedor se incorpora al compose raíz (`Backend/docker-compose.yml`) como `ms-frontend` (`:3000` → `:80`).

Flujo de comunicación:

```text
Navegador
    |
    v
Nginx + React
    |
    v
API Gateway
    |
    v
Microservicios
```

Nginx se encargará de servir el frontend y redirigir las solicitudes de API. El API Gateway será responsable de distribuirlas entre los microservicios.

### 4. Responsabilidades

| Componente | Responsabilidad |
|---|---|
| React | Interfaz e interacción con el usuario. |
| Node.js | Construcción del frontend. |
| Nginx | Publicación del frontend y proxy inverso. |
| API Gateway | Enrutamiento de solicitudes hacia los microservicios. |
| Docker Compose | Integración y ejecución de los contenedores. |

**Decisión de arquitectura:** el frontend es un proyecto propio con su Dockerfile; la configuración canónica de Nginx vive en `infrastructure/nginx/nginx.conf` y la imagen usa una copia embebida en `Frontend/conf/nginx-default.conf`. El contenedor `ms-frontend` se publica en el compose raíz.


