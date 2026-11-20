# TicketYa — Guía para agentes de IA

Monorepo académico (TPE UNICEN): `Backend/` (API Gateway + microservicios Spring Boot, Java 17, Maven) y `Frontend/` (SPA React + Vite). No hay CI ni build raíz; cada módulo se compila y prueba de forma independiente. Toda la documentación del proyecto está en español.

## Fuentes de verdad (leer antes de cambiar lógica)
- **Alcance y entregables:** `Backend/specs/consigna.md` (no modificar).
- **Diseño y trabajo del equipo:** `Backend/specs/` (`reglas-de-negocio.md`, `endpoints.md`, `comunicacion.md`, `arquitectura.md`, `bases-de-datos.md`, `infraestructura.md`, `backlog.md`).
- **Documentación formal de entrega:** `Backend/docs/` (`arquitectura.md`, `modelo-datos.md`, `observabilidad.md`, `api/gateway.md`, `openapi/`, `postman/`, `testing/`).
- **Contrato local por microservicio:** `Backend/msvc-<nombre>/docs/endpoints.md`.

## Invariantes de arquitectura
- El navegador habla **únicamente con `Backend/APIGateway`** (en Docker, Nginx hace proxy de `/api/` hacia el gateway).
- La autenticación JWT y la autorización base por rol viven en `APIGateway`, que sobreescribe los headers `X-User-*` desde el token verificado y descarta los que envíe el cliente.
- **Una base de datos por microservicio** (sin FKs físicas entre servicios; solo referencias lógicas por UUID).
- Los esquemas y seeds viven centralizados en `Backend/infrastructure/{mysql,mongodb,redis}/` (no usar `DataLoader` en Java). Los scripts de inicialización solo corren sobre volúmenes nuevos (`docker compose down -v` para re-aplicar).
- Comunicación entre servicios: REST síncrona con OpenFeign para el flujo transaccional y RabbitMQ para eventos de dominio.
- En `Frontend/`, el flag `VITE_USE_MOCK` alterna entre datos mock (`true`) y el API Gateway real (`false`).

## Comandos
- **Stack completo:** `cd Backend && docker compose up -d --build` (requiere `.env` copiado desde `.env.example`).
- **Un microservicio con su DB:** `cd Backend/msvc-<nombre> && docker compose up -d --build`.
- **Ejecutar un microservicio en local:** `mvn -DskipTests spring-boot:run` dentro del módulo (en Windows `mvn.cmd`; levantar su DB primero).
- **Tests unitarios:** `mvn test` (o `mvn test -Dtest=ClaseTest`) dentro del módulo. **No ejecutar varios `mvn` en paralelo** sobre el mismo `~/.m2`.
- **Tests de integración (Testcontainers, requiere Docker):** `mvn -Pit verify` dentro del módulo.
- **Frontend (en `Frontend/`):** `npm run dev`, `npm run lint`, `npm run build`.

## Workflows

### 1. Antes de empezar una tarea
- Partir siempre de una rama nueva desde `origin/main` (nunca trabajar directo en `main`).
- Revisar `Backend/specs/backlog.md` y la spec relevante antes de diseñar o codificar.
- Si el cambio afecta rutas, roles o contratos, verificar el impacto en `APIGateway`, los microservicios consumidores y `Frontend/`.

### 2. Durante la implementación
- Respetar la estructura de capas del microservicio (`controller`, `service`, `repository`, `entity|document`, `dto`, `mapper`, `clients`, `exception`, `config`).
- No inventar reglas de negocio que contradigan `Backend/specs/reglas-de-negocio.md`; ante ambigüedad, consultar antes de asumir.
- Verificar los cambios con los tests del módulo afectado (y `npm run build` si se tocó `Frontend/`).

### 3. Al cerrar una tarea (obligatorio antes de commit/PR)
- **Backlog:** actualizar en `Backend/specs/backlog.md` el estado (`hecho` / `parcial`), la nota con fecha y el resumen de estado.
- **Documentación afectada:**
  - Si cambiaron rutas, filtros o permisos: actualizar `Backend/docs/api/gateway.md`.
  - Si cambiaron endpoints o DTOs: actualizar `<msvc>/docs/endpoints.md`, `Backend/specs/endpoints.md`, `Backend/docs/openapi/` y `Backend/docs/postman/`.
  - Si cambió el esquema o la arquitectura: actualizar `Backend/infrastructure/`, `Backend/docs/modelo-datos.md` y/o `Backend/docs/arquitectura.md`.
- **Higiene Git:**
  - Usar commits convencionales (`feat|fix|test|docs|refactor(scope): descripción`).
  - Nunca commitear `.env`, credenciales ni archivos temporales/generados fuera de alcance.
