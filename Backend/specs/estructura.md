# Estructura del repositorio

> **Estado:** `hecho` · **Fecha:** 2026-11-22 · **Alcance:** mapa de carpetas, capas y dónde vive cada
> artefacto · **No incluye:** detalle de despliegue (ver `infraestructura.md`) ni contrato HTTP
> (ver `endpoints.md`).

Árbol de carpetas de TicketYa (Frontend, Backend y documentación transversal).

```text
ticketYa/
├── AGENTS.md                          # guía para agentes de IA
├── README.md                          # inicio rápido y mapa general
├── start.sh                           # arranque opcional del stack (Linux/macOS)
├── .env.example                       # variables de entorno de ejemplo
├── .gitignore
│
├── Frontend/                          # SPA React + Vite. En Docker la sirve Nginx.
│   ├── src/{api,auth,components,config,context,domain,hooks,lib,pages,providers,routes,styles}/
│   ├── public/                        # estáticos e imágenes de eventos
│   ├── conf/nginx-default.conf        # copia embebida para la imagen
│   ├── docs/                          # endpoints-back.md, endpoints-front.md
│   ├── .env / .env.example            # VITE_API_BASE_URL + VITE_USE_MOCK (versionado)
│   ├── docker-compose.yml
│   ├── Dockerfile                     # multi-stage: Node build → Nginx
│   ├── package.json
│   └── vite.config.js
│
├── Backend/
│   ├── docker-compose.yml             # stack completo vía include de los Compose individuales
│   ├── README.md                      # detalle de ejecución del backend
│   ├── scripts/                       # e2e.sh, status.sh, up-e2e.sh, seeds/backfill
│   ├── e2e-tests/                     # REST Assured + Awaitility (perfil -Pe2e)
│   │
│   ├── APIGateway/                    # entrada HTTP única; no tiene DB propia
│   │   ├── src/
│   │   ├── docs/endpoints.md          # rutas, roles y permisos del gateway
│   │   ├── docker-compose.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── msvc-usuarios/                 # identidad y JWT (MySQL)
│   ├── msvc-eventos/                  # eventos, lugares, sectores y participantes (MySQL + RabbitMQ)
│   ├── msvc-ventas/                   # reservas, compras, pagos, reportes y fila (MySQL + Redis + RabbitMQ)
│   ├── msvc-entradas/                 # emisión/QR, devoluciones y validación (MySQL + RabbitMQ)
│   ├── msvc-preferencias/             # preferencias, historial y recomendaciones (MongoDB + RabbitMQ)
│   ├── msvc-notificaciones/           # plantillas y entrega email/push (MySQL + RabbitMQ + MailHog)
│   │   # Cada msvc: src/main/java/<contexto>/{controller,service,repository,entity|document,
│   │   #   dto,mapper,clients,exception,config,security}/ · src/test/ ·
│   │   #   docs/{README.md,endpoints.md} · docker-compose.yml · Dockerfile · pom.xml
│   │
│   ├── infrastructure/                # esquemas, seeds y servicios compartidos
│   │   ├── docker-compose.yml
│   │   ├── mysql/{usuarios,eventos,ventas,entradas,notificaciones}/init.sql
│   │   ├── mongodb/preferencias/init.js
│   │   ├── redis/ventas/redis.conf
│   │   ├── rabbitmq/docker-compose.yml
│   │   ├── nginx/nginx.conf           # estáticos del Frontend + proxy /api → gateway
│   │   └── readme.md
│   │
│   ├── specs/                         # documentación de trabajo (fuente de verdad del equipo)
│   │   ├── README.md                  # flujo Spec-Driven, convenciones y checklist
│   │   ├── consigna.md                # alcance y entregables (no se modifica)
│   │   ├── backlog.md                 # estado por ID/US
│   │   ├── reglas-de-negocio.md
│   │   ├── politica-de-roles.md
│   │   ├── endpoints.md               # contrato HTTP de referencia
│   │   ├── comunicacion.md            # gateway, Feign, RabbitMQ e idempotencia
│   │   ├── arquitectura.md
│   │   ├── microservicios.md
│   │   ├── infraestructura.md
│   │   ├── estructura.md
│   │   ├── bases-de-datos.md          # motores e índices al modelo
│   │   ├── plantilla-docs-msvc.md     # plantilla DOC-MS-00
│   │   ├── fila-virtual.md, integracion-entre-msvc.md, carga-e2-tst-03.md, …
│   │   └── diagramas/                 # modelo*.md + diagramas-secuencia.md
│   │
│   └── docs/                          # documentación formal de entrega (no DDD de un solo msvc)
│       ├── README.md
│       ├── arquitectura.md
│       ├── modelo-datos.md
│       ├── observabilidad.md
│       ├── consistencia-y-compensaciones.md
│       ├── decisiones-tecnologicas.md
│       ├── api/gateway.md
│       ├── openapi/                   # 6 specs + bundle consolidado
│       ├── postman/                   # colección + environment
│       └── testing/                   # concurrencia, integración, notificaciones y carga
│
└── (Frontend/dist y node_modules son generados y no se versionan)
```

## Cómo proceder (resumen)

1. `specs/` → escribir/revisar la spec de la feature **antes** de tocar código (ver `README.md`).
2. `infrastructure/` → scripts de init, configuración de Redis/RabbitMQ y Nginx.
3. Cada msvc → modelar en `docs/` (DDD), luego código por capas; levantar solo con su `docker-compose.yml`.
4. `APIGateway` → rutas hacia msvcs ya expuestos (`docs/endpoints.md` y `docs/api/gateway.md`).
5. `Frontend/` → UI; en Docker, multi-stage con Nginx usando `conf/nginx-default.conf`.
6. `Backend/docker-compose.yml` → integración completa cuando los contratos entre capas cierren.
