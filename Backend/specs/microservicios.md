# Microservicios

Cada contexto es un proyecto Spring Boot independiente (carpeta + `pom.xml` + `Dockerfile` +
`docker-compose.yml`). No se comparten entidades JPA/documentos entre servicios; las referencias
cruzadas son UUID sin FK física. El detalle de contratos vive en `specs/endpoints.md`,
`docs/openapi/` y en el `docs/` de cada servicio.

## 1. Servicios, puertos y persistencia

| Servicio (repo) | Contenedor app | Puerto | Persistencia | Responsabilidad |
| --- | --- | ---: | --- | --- |
| `APIGateway` | ms-gateway | 8080 | — | Punto único de entrada HTTP; enruta al msvc; no tiene DB |
| `msvc-usuarios` | ms-usuarios | 8081 | MySQL (`db-mysql-usuarios`, host 3307) | Identidad, roles, credenciales, JWT |
| `msvc-eventos` | ms-eventos | 8082 | MySQL (`db-mysql-eventos`, host 3308) | Eventos, lugares, sectores, participantes, publicación, búsqueda |
| `msvc-ventas` | ms-ventas | 8083 | MySQL + Redis (`db-mysql-ventas` 3309, `db-redis-ventas` 6380) | Disponibilidad, reservas, compras, pagos, reportes y fila virtual |
| `msvc-entradas` | ms-entradas | 8084 | MySQL (`db-mysql-entradas`, host 3310) | Emisión/QR, devoluciones, validación de acceso |
| `msvc-preferencias` | ms-preferencias | 8085 | MongoDB (`db-mongo-preferencias`, host 27018) | Preferencias, historial y recomendaciones |
| `msvc-notificaciones` | ms-notificaciones | 8086 | MySQL (`db-mysql-notificaciones`, host 3311) | Plantillas, bandeja y entrega push/email |

## 2. APIs públicas y límites

Todas se consumen por el gateway con prefijo `/api`. El contrato completo (métodos, roles,
códigos) está en `specs/endpoints.md`, `docs/openapi/` y en el `endpoints.md` de cada servicio.

| Servicio | Rutas principales (vía `/api`) | Límite de responsabilidad |
| --- | --- | --- |
| usuarios | `/auth/**`, `/usuarios/**` | No autoriza por endpoint (lo hace el gateway) ni guarda datos de negocio |
| eventos | `/eventos/**` (salvo solapes), `/lugares/**`, `/sectores/**`, `/participantes/**`, `/organizadores/**` | No gestiona stock ni ventas; los asientos son virtuales |
| ventas | `/reservas/**`, `/compras/**`, `/pagos/**`, `/reportes/**`, `/eventos/{id}/disponibilidad`, `/eventos/{id}/fila/**` | No emite entradas ni QR (lo pide a entradas); no configura eventos |
| entradas | `/entradas/**`, `/devoluciones/**`, `/eventos/{id}/validaciones` | No cobra ni fija precios; pide liberar cupo a ventas |
| preferencias | `/preferencias/**`, `/recomendaciones/**`, `/clientes/**`, `/eventos/{id}/recomendaciones` | No entrega notificaciones ni posee compras |
| notificaciones | `/notificaciones/**` | No decide reglas de negocio; solo compone, entrega y registra avisos |

**Solapes de `/eventos/**`:** el gateway rutea `disponibilidad`, `cupos` y `fila` a **ventas**,
`validaciones` a **entradas** y `recomendaciones` a **preferencias** antes del catch-all a eventos.

## 3. Dependencias

- **Síncronas (OpenFeign):** ver el detalle y el comportamiento ante fallos en
  [`comunicacion.md`](comunicacion.md) §Síncrono entre microservicios. En resumen: eventos → ventas
  (abrir cupos e invalidar caché de fila), ventas → eventos (precio/flag/dueño), ventas ↔ entradas
  (emisión y resumen / liberación de cupo), eventos → entradas (reintegro por cancelación),
  preferencias → eventos/ventas (matching e historial) y notificaciones → preferencias/ventas/usuarios
  (destinatarios y canales).
- **Asíncronas (RabbitMQ):** eventos publica `evento.*`, ventas `compra.confirmada` y entradas
  `devolucion.aprobada`; los consumen notificaciones (3 colas) y preferencias (historial). Contratos,
  DLQ e idempotencia en [`comunicacion.md`](comunicacion.md) §Async.

## 4. Estado y verificación

- Arquitectura operativa: API Gateway + **6 microservicios** + frontend, orquestados por Docker
  Compose (`include`).
- **E1 completo:** eventos/lugares/sectores, publicación y búsqueda, reserva temporal, compra, pago
  mock, emisión de QR, devoluciones y cancelaciones.
- **E2 implementado:** fila virtual (Redis), control de acceso QR, preferencias/recomendaciones/
  historial (MongoDB), notificaciones (PUSH mock + EMAIL SMTP), reportes del organizador, JWT + RBAC,
  caché/resiliencia, correlation id + bitácora y geocodificación.
- **Verificado 2026-11-21:** E2E F1–F5 5/5, 37 ITs de borde, matriz de carga 25/50/100 (normal y
  fila), Postman 216 requests/587 assertions/0 fallos y corrida limpia del dominio completo
  (`E2-DEP-01`).
- **Pendiente funcional:** extensión CLIENTE → ORGANIZADOR (`X-ORG-01`) y transferencia de entradas
  (sin API). El resto del backlog es documental (`US-03`, `DOC-MS-SYNC`).

## 5. Documentación y Compose

- Contexto por contexto: `Backend/<msvc>/docs/` (plantilla `DOC-MS-00`), arquitectura canónica en
  `docs/arquitectura.md`, modelo en `docs/modelo-datos.md`.
- Individual: `Backend/<msvc>/docker-compose.yml` (app + su DB).
- Completo: `Backend/docker-compose.yml` (`include` de todos).
- Puertos y diagrama Docker: `specs/infraestructura.md`.
