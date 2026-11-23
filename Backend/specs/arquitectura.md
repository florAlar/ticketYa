# Arquitectura del proyecto

> **Estado:** `hecho` · **Fecha:** 2026-11-22 · **Alcance:** organización del monorepo, despliegue
> Docker y estado de implementación · **No incluye:** contrato HTTP (ver `endpoints.md`) ni modelo de
> datos (ver `bases-de-datos.md`).

## Organización general

- Cada microservicio tiene su propio `pom.xml`, donde se definen sus dependencias y configuraciones de Maven.
- Cada microservicio tiene su propio `Dockerfile`, que permite construir y ejecutar su aplicación de manera independiente.
- Cada microservicio y el API Gateway tienen su propio `docker-compose.yml`, donde se define la aplicación y sus dependencias. El API Gateway no necesita una base de datos.
- `Backend/docker-compose.yml` integra los Compose individuales mediante `include` para levantar todo el entorno.
- `infrastructure/` centraliza los scripts de inicialización y las configuraciones de las bases de datos.
- `specs/` contiene las decisiones arquitectónicas y la documentación del proyecto.
- No se comparten entidades JPA ni documentos MongoDB entre microservicios. Cada uno administra sus propios modelos y datos.

## Estructura del proyecto

Árbol completo del repo (capas, carpetas y rol de cada una): [estructura.md](./estructura.md).

- **Microservicios:** `APIGateway`, `msvc-usuarios`, `msvc-eventos`, `msvc-ventas`, `msvc-entradas`, `msvc-preferencias` y `msvc-notificaciones`. Contienen la lógica de negocio, controladores, servicios, repositorios y modelos.
- **Infrastructure:** contiene los recursos de inicialización y configuración de las bases de datos, separados de la lógica de cada microservicio.
    - `mysql/*/init.sql`: inicializa estructuras o datos SQL de cada microservicio.
    - `mongo/*/init.js`: inicializa colecciones e índices de MongoDB (hoy `mongodb/preferencias/init.js`).
    - `redis/*/redis.conf`: define la configuración de Redis.
    - `docker-compose.yml`: integra los Compose individuales para levantar todo el entorno.

## Estado de implementación

- E1 y E2 implementados, salvo la transferencia de entradas, que no tiene API; el alta de organizador es manual por un ADMIN (X-ORG-01 descartado).
- Seguridad: JWT emitido por `msvc-usuarios` y validado en `APIGateway`; autorización por rol en cada endpoint.
- Comunicación: REST con OpenFeign para el flujo transaccional y RabbitMQ para eventos de dominio; Redis en ventas para stock, idempotencia y fila virtual.
- Detalle de responsabilidades y estado por feature: [microservicios.md](./microservicios.md) y [backlog.md](./backlog.md).

## Dockerfile y Docker Compose

Cumplen funciones diferentes y complementarias:

### 1. Dockerfile: construcción de imágenes

Cada microservicio tiene su propio `Dockerfile`, que contiene las instrucciones para construir su imagen:

- Qué imagen base utilizar (Java 17).
- Dónde trabajar dentro del contenedor.
- Qué archivo `.jar` copiar.
- Qué puerto exponer.
- Cómo iniciar la aplicación.

### 2. Docker Compose: ejecución del entorno

Define cómo se levantan y conectan los contenedores.

- Cada microservicio puede levantarse individualmente mediante su propio `docker-compose.yml`.
- El `Backend/docker-compose.yml` integra los Compose individuales mediante `include` para ejecutar el entorno completo.

### Servicios del entorno completo

- Los seis microservicios.
- El API Gateway.
- MySQL: una instancia independiente por cada microservicio relacional.
- MongoDB: utilizado por preferencias.
- Redis: utilizado por ventas.
- nginx (servicio `ms-frontend`): sirve la SPA en `:3000` y proxya `/api/` al gateway.

Compose configura las dependencias mediante `depends_on`, para que al levantar un microservicio también se inicien los servicios de los que depende, como su base de datos.

**Detalle de puertos y comandos:** consultar `specs/infraestructura.md`.
