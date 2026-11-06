# Estructura del repositorio

Árbol de carpetas de TicketYa (Frontend, Backend y docs transversales).

```text
TicketYa/
│
├── Frontend/                          # UI React. Build estático; en prod lo sirve Nginx. Habla solo con el API Gateway (/api).
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── Dockerfile                     # Multi-stage: Node build → imagen Nginx con dist/
│
├── Backend/
│   ├── docker-compose.yml             # Compose general: include de Gateway + msvcs (stack completo).
│   │
│   ├── APIGateway/                    # Entrada HTTP del backend. Enruta al msvc que corresponda; no tiene DB propia.
│   │   ├── src/
│   │   ├── docs/                      # Decisiones DDD / diseño del gateway (vacío hasta documentar).
│   │   ├── docker-compose.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   │   # Microservicios: un bounded context cada uno. Implementar dominio → aplicación → infraestructura → API.
│   │   # Dentro de cada msvc, docs/ es para modelado DDD de ese contexto (agregados, ubícuo, eventos).
│   │   # Capas Java comunes (todos los msvc; NO el API Gateway):
│   │   #   controller/  → adaptadores de entrada HTTP (no mete reglas de negocio).
│   │   #   service/     → casos de uso / orquestación de dominio.
│   │   #   repository/  → puertos de persistencia.
│   │   #   entity|document/ → modelo persistido (entity=MySQL JPA; document=Mongo).
│   │   #   dto/         → contratos de entrada/salida de la API.
│   │   #   mapper/      → entity/document <-> DTO.
│   │   #   clients/     → Feign clients hacia otros msvc (HTTP saliente).
│   │   #   exception/   → excepciones y handlers del contexto.
│   │   #   config/      → cableado Spring (beans, datasources).
│   │   #   security/    → auth/autorización del msvc (si aplica).
│   │   #   redis|queue/ → infra auxiliar del contexto (solo ventas hoy).
│   │   # Orden de trabajo sugerido: docs DDD → entity/document + repository → mapper/service → controller → clients.
│   │
│   ├── msvc-usuarios/                 # Identidad, registro, autenticación. MySQL.
│   │   ├── src/main/java/usuarios/{controller,service,repository,entity,dto,mapper,clients,exception,security,config}/
│   │   ├── src/main/resources/application.yml
│   │   ├── src/test/
│   │   ├── docs/
│   │   ├── docker-compose.yml         # App + MySQL del contexto.
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── msvc-eventos/                  # Catálogo y ciclo de vida de eventos. MySQL.
│   │   ├── src/main/java/eventos/{controller,service,repository,entity,dto,mapper,clients,exception,config}/
│   │   ├── src/main/resources/application.yml
│   │   ├── docs/
│   │   ├── docker-compose.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── msvc-ventas/                   # Compra, fila virtual, stock temporal. MySQL + Redis.
│   │   ├── src/main/java/ventas/{controller,service,repository,entity,dto,mapper,clients,exception,redis,queue,security,config}/
│   │   ├── src/main/resources/application.yml
│   │   ├── docs/
│   │   ├── docker-compose.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── msvc-entradas/                 # Emisión y estado de entradas. MySQL.
│   │   ├── src/main/java/entradas/{controller,service,repository,entity,dto,mapper,clients,exception,config}/
│   │   ├── src/main/resources/application.yml
│   │   ├── docs/
│   │   ├── docker-compose.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── msvc-preferencias/             # Preferencias de usuario (documentos). MongoDB.
│   │   ├── src/main/java/preferencias/{controller,service,repository,document,dto,mapper,clients,exception,config}/
│   │   ├── src/main/resources/application.yml
│   │   ├── docs/
│   │   ├── docker-compose.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── msvc-notificaciones/           # Plantillas e intentos de envío. MySQL.
│   │   ├── src/main/java/notificaciones/{controller,service,repository,entity,dto,mapper,clients,exception}/
│   │   ├── src/main/resources/application.yml
│   │   ├── docs/
│   │   ├── docker-compose.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── infrastructure/                # Inicializa bases y orquesta lo necesario para el despliegue de las apps.
│   │   ├── docker-compose.yml         # Puede reutilizar el stack vía include de los Compose de cada app.
│   │   ├── mysql/                     # init.sql por contexto (usuarios, eventos, ventas, entradas).
│   │   ├── mongodb/                   # init de preferencias.
│   │   ├── redis/                     # conf de Redis (ventas).
│   │   └── nginx/                     # nginx.conf: estáticos del Frontend + proxy /api → API Gateway.
│   │
│   └── docs/                          # Documentación transversal del backend (no DDD de un solo msvc).
│       ├── arquitectura.md
│       ├── infraestructura.md
│       ├── microservicios.md
│       ├── bases-de-datos.md
│       ├── comunicacion.md
│       └── diagramas/
│
├── .env.example                       # Variables de entorno de ejemplo (sin secretos reales).
├── .gitignore
└── README.md
```

## Cómo proceder (resumen)

1. `infrastructure/` → scripts de init y conf de Nginx cuando haga falta el entorno.
2. Cada msvc → modelar en `docs/` (DDD), luego código por capas; levantar solo con su docker-compose.
3. `APIGateway` → rutas hacia msvcs ya expuestos.
4. `Frontend/` → UI; en Docker, multi-stage con Nginx usando `infrastructure/nginx/nginx.conf`.
5. `Backend/docker-compose.yml` → integración completa cuando los contratos entre capas cierren.
