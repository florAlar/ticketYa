# Arquitectura de microservicios — Entregas 1 y 2

Fuente: `specs/arquitectura.md`, `specs/microservicios.md`, `specs/comunicacion.md`, `specs/endpoints.md` y el código.

## 1. Diagrama de módulos

```mermaid
flowchart LR
    FE["Frontend React<br/>(Nginx :3000)"]
    GW["API Gateway<br/>:8080<br/>(enrutado, CORS, identidad)"]

    subgraph MS["Microservicios Spring Boot"]
        US["msvc-usuarios :8081"]
        EV["msvc-eventos :8082"]
        VE["msvc-ventas :8083"]
        EN["msvc-entradas :8084"]
        PR["msvc-preferencias :8085<br/>(E2)"]
        NO["msvc-notificaciones :8086<br/>(E2)"]
    end

    subgraph DB["Persistencia (una por servicio)"]
        DBU[("MySQL usuarios")]
        DBE[("MySQL eventos")]
        DBV[("MySQL ventas")]
        RV[("Redis ventas")]
        DBN[("MySQL entradas")]
        MG[("MongoDB preferencias")]
        DBO[("MySQL notificaciones")]
    end

    FE -->|HTTP/JSON| GW
    GW --> US
    GW --> EV
    GW --> VE
    GW --> EN
    GW --> PR
    GW --> NO

    US --- DBU
    EV --- DBE
    VE --- DBV
    VE --- RV
    EN --- DBN
    PR --- MG
    NO --- DBO

    EV -. "Feign: abrir cupos al publicar" .-> VE
    VE -. "Feign: precio por sector; dueño y sectores del evento (reportes)" .-> EV
    VE -. "Feign: emitir entradas tras pago" .-> EN
    EN -. "Feign: liberar cupo al aprobar devolución" .-> VE
    EV -. "Feign: reintegro al cancelar evento" .-> EN
    EN -. "Feign: nombre/lugar del evento; estado y dueño (validación de acceso)" .-> EV
    VE -. "Feign: resumen de entradas por sector (reportes)" .-> EN
```

Reglas de la arquitectura:
- El navegador habla **solo con el gateway**; nunca con un microservicio directo.
- **Una base de datos por microservicio.** No se comparten entidades ni tablas; las referencias entre servicios son lógicas (UUID sin FK física). Ver `modelo-datos.md`.
- Comunicación **REST síncrona** (OpenFeign) entre microservicios, según la consigna E1. No hay broker en esta entrega.
- Cada servicio es un proyecto Maven independiente con su `Dockerfile` y su `docker-compose.yml`; `Backend/docker-compose.yml` los integra con `include`.

## 2. Responsabilidades

| Servicio | Puerto | Persistencia | Responsabilidad |
|---|---:|---|---|
| **API Gateway** | 8080 | — | Punto único de entrada, enrutado por prefijo (`/api/...` → servicio), CORS, identificación del usuario y propagación de `X-User-*`, generación del `X-Correlation-Id` y autorización por rol |
| **msvc-usuarios** | 8081 | MySQL | Cuentas, login/registro, roles, perfil |
| **msvc-eventos** | 8082 | MySQL | Eventos, **lugares** (con latitud/longitud opcionales) y sus **sectores** (los carga el ADMIN), participantes, precio y cupo de cada sector por evento, publicación, estados y búsqueda. Los asientos son virtuales: no hay tabla de asientos |
| **msvc-ventas** | 8083 | MySQL + Redis | Disponibilidad y cupos, reservas temporales, compras, pagos (mock), emisión de entradas, anti doble venta y **reportes del organizador** (ventas, ocupación y recaudación) |
| **msvc-entradas** | 8084 | MySQL | Entradas con QR único, devoluciones, reintegro por cancelación y **control de acceso**: validación del QR en la puerta (un QR ingresa una sola vez) con auditoría de intentos |
| msvc-preferencias | 8085 | MongoDB | *(E2)* preferencias y recomendaciones |
| msvc-notificaciones | 8086 | MySQL | *(E2)* avisos y plantillas |

## 3. APIs públicas por servicio

Todas se consumen vía gateway con prefijo `/api`. Listado completo, parámetros y códigos: `specs/endpoints.md` y `docs/openapi/`.

| Servicio | Rutas principales |
|---|---|
| usuarios | `POST /auth/login`, `POST /auth/register`, `GET/PATCH /usuarios/me`, `GET /usuarios`, `PATCH /usuarios/{id}/estado`, `PUT /usuarios/{id}/roles` |
| eventos | `GET/POST /eventos`, `GET/PUT/DELETE /eventos/{id}`, `POST /eventos/{id}/publicacion`, `PATCH /eventos/{id}/estado`, `PATCH /eventos/{id}/fecha`, `PUT /eventos/{id}/sectores/{sectorId}/precio`, `/eventos/{id}/participantes`, `/lugares/**`, `/sectores/**`, `/participantes/**`, `/organizadores/me/eventos` |
| ventas | `GET /eventos/{id}/disponibilidad`, `GET /eventos/disponibilidad?ids=`, `POST /reservas`, `/reservas/{id}/{confirmacion\|renovacion\|cancelacion}`, `POST /compras`, `POST /compras/{id}/pagos`, `POST /pagos/{id}/reintentos`, `GET /reportes/eventos/{id}[/ventas\|sectores\|ocupacion\|recaudacion]` |
| entradas | `GET /entradas`, `GET /entradas/{id}`, `GET /entradas/{id}/qr`, `POST /entradas/{id}/devoluciones`, `GET /devoluciones`, `POST /devoluciones/{id}/{aprobacion\|rechazo}`, `POST /eventos/{id}/validaciones`, `GET /eventos/{id}/validaciones` |

Solapes de `/eventos/**`: el gateway enruta `/eventos/{id}/disponibilidad` y `/eventos/{id}/cupos` a **ventas** antes del catch-all hacia **eventos**.

## 4. Comunicación entre servicios

| Origen → Destino | Cuándo | Para qué |
|---|---|---|
| eventos → ventas | Al publicar un evento | `POST /eventos/{id}/cupos`: abre el stock por sector (idempotente; si falla, no se publica) |
| ventas → eventos | Al crear una reserva | `GET /eventos/{id}`: obtener el precio unitario del sector |
| entradas → eventos | Al listar/consultar entradas | `GET /eventos/{id}`: enriquecer la entrada con nombre, lugar y estado del evento (best-effort: si falla, esos campos quedan `null`) |
| ventas → entradas | Tras un pago aprobado | `POST /entradas/emision`: crear entradas con QR (idempotente por compra, con reintentos y job) |
| entradas → ventas | Al aprobar una devolución | `POST /eventos/{id}/cupos/liberacion`: devolver 1 lugar al cupo |
| eventos → entradas | Al cancelar un evento | `POST /eventos/{id}/reintegro-cancelacion` (best-effort) |
| ventas → entradas | Al consultar un reporte | `GET /eventos/{id}/entradas/resumen`: entradas del evento por sector y estado (devueltas, usadas, a reintegrar y montos). Interno: el gateway no lo expone. Si falla, el reporte responde 503 |
| ventas → eventos | Al consultar un reporte | `GET /eventos/{id}`: sectores y dueño del evento (un organizador solo ve los reportes de sus eventos) |
| entradas → eventos | Al validar o listar validaciones | `GET /eventos/{id}`: estado del evento (cancelado no permite ingreso) y dueño del evento |

Headers relevantes: `X-User-Id`, `X-User-Role` (identidad), `Idempotency-Key` (reservas y compras) y `X-Correlation-Id` (trazabilidad: lo genera el gateway y lo propagan los servicios y las llamadas Feign).

## 5. Identificación del usuario

Según la consigna, en la Entrega 1 no hay autenticación formal: la identidad llega por encabezado (`X-User-Id`, `X-User-Role`) y los microservicios confían en ella. El gateway admite también JWT con roles, activable con `TICKETYA_JWT_ENABLED=true` (adelanto de la Entrega 2). Detalle en el `README.md` raíz.

## 6. Despliegue

`cd Backend && docker compose up -d --build` levanta gateway, microservicios, bases y frontend (15 contenedores). Puertos y diagrama Docker: `specs/infraestructura.md`.

## 7. Trazabilidad y bitácora

Cada request lleva un `X-Correlation-Id` que genera el gateway y que aparece en cada línea de log de todos los servicios y en la respuesta; las acciones críticas (login, publicación, pagos, devoluciones, validaciones de acceso…) quedan en una bitácora con ese mismo id. Detalle y cómo seguir una operación entre servicios: [`observabilidad.md`](./observabilidad.md).
