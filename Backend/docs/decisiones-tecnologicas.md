# Decisiones tecnológicas y patrones arquitectónicos (ARQ-06)

**Entregable 2 — Consigna §6.C**  
Justifica el stack tecnológico elegido (con énfasis en la base NoSQL y el almacenamiento en memoria) y los patrones arquitectónicos y de diseño aplicados en TicketYa, indicando dónde vive cada uno en el repositorio.

---

## 1. Justificación de tecnologías

| Tecnología | Dónde se usa | Por qué se eligió | Alternativa descartada y motivo |
|---|---|---|---|
| **Java 17 + Spring Boot 3** | Gateway y los 6 microservicios | Ecosistema homogéneo y maduro para REST, JPA, MongoDB, Redis, RabbitMQ, OpenAPI (`springdoc`) y testing (`JUnit 5`, `Mockito`, `Testcontainers`). Reduce el costo cognitivo del equipo al compartir convenciones entre los 7 módulos Maven. | Mezclar lenguajes (p. ej. Node/Python en algún msvc) agregaba complejidad operativa y de despliegue sin beneficio funcional en el alcance del TP. |
| **Spring Cloud Gateway (MVC)** | `APIGateway` (`:8080`) | Punto único de entrada alineado al stack servlet del resto del proyecto; soporta enrutamiento declarativo por `order` (clave para resolver solapes bajo `/api/eventos/**`), `StripPrefix=1`, CORS, JWT y filtros `OncePerRequestFilter`. | Gateway WebFlux reactivo: innecesario porque los msvcs son bloqueantes (JDBC/Feign) y complica la propagación de contexto `MDC` para `X-Correlation-Id`. |
| **MySQL 8 (una instancia por msvc)** | `usuarios`, `eventos`, `ventas`, `entradas`, `notificaciones` | Datos relacionales con reglas fuertes de integridad: locks pesimistas (`FOR UPDATE`, `SKIP LOCKED`), restricciones `UNIQUE` (anti doble venta de asientos) y transacciones ACID. Cada servicio posee su propia base para mantener bajo acoplamiento. | Una única base compartida violaría el aislamiento de microservicios y acoplaría despliegues/esquemas. |
| **MongoDB (NoSQL)** | `msvc-preferencias` (`:8085`) | **Requisito NoSQL (§3 y §6.C).** Almacena dos colecciones documentales: `preferencias_clientes` (preferencias de estructura flexible y evolutiva: listas de categorías, ciudades, artistas favoritos, rango de precio y canales de notificación) e `interacciones_eventos` (historial de compras con *snapshot* embebido del evento: nombre, categoría, ciudad, artistas, fecha). Embeber el snapshot evita llamadas N+1 a `msvc-eventos` al paginar el historial o calcular recomendaciones por co-compra. | Modelar preferencias e historial en MySQL requería múltiples tablas puente (`cliente_artista`, `cliente_categoria`, `cliente_ciudad`) y joins costosos para un dominio orientado a lectura agregada y evolución frecuente de atributos. |
| **Redis** | `msvc-ventas` (`:8083`) | Almacenamiento en memoria con **TTL nativo** y ejecución atómica (scripts Lua). Resuelve tres necesidades efímeras y de baja latencia: (1) holds temporales de reserva (10 min), (2) claves `Idempotency-Key` de compra (24 h) y (3) fila virtual de alta demanda (colas ordenadas por llegada, tandas y tokens temporales de compra). | Persistir holds, idempotencia y posiciones de fila en MySQL generaría alta contención de escritura y limpieza constante de filas expiradas bajo picos de demanda. |
| **RabbitMQ** | Eventos de dominio (`eventos.exchange`) | Desacopla productores (`ventas`, `eventos`, `entradas`) de consumidores (`notificaciones`, `preferencias`). Permite *fan-out* por *routing key* (`compra.confirmada`, `evento.*`, `devolucion.aprobada`) sin bloquear la respuesta HTTP de una compra por el envío de emails o la actualización de recomendaciones. | Llamadas REST síncronas para notificar o registrar historial acoplarían disponibilidad y latencia de la compra a servicios auxiliares. |
| **OpenFeign** | Comunicación síncrona entre msvcs | Clientes declarativos concisos para operaciones que requieren respuesta inmediata (validar precio del evento, abrir/liberar cupo, emitir entradas, consultar resumen para reportes) con propagación automática de `X-Correlation-Id` y `X-Internal-Token` vía interceptores. | `RestTemplate` manual duplicaría código de serialización, headers y manejo de errores. |
| **React 19 + Vite + Nginx** | `Frontend/` (`:3000`) | SPA liviana servida por Nginx con proxy `/api/` hacia `ms-gateway:8080`, eliminando problemas de CORS en Docker y manteniendo la regla de que el navegador solo habla con el Gateway. | Renderizado server-side (Thymeleaf) acoplaría la UI a un microservicio particular. |
| **MailHog** | Entorno Docker (`:8025` / `:1025`) | Servidor SMTP local con bandeja web para verificar entregas reales de email de `msvc-notificaciones` sin depender de credenciales ni cuotas de un proveedor externo. | Solo mock en consola no permitía inspeccionar el correo MIME generado ni probar el adaptador SMTP real. |
| **Docker + Docker Compose (`include`)** | `Backend/docker-compose.yml` + compose por msvc | Cada servicio y cada base corre en su contenedor independiente, con `Dockerfile` multi-stage y healthchecks. El uso de `include` permite levantar un solo microservicio en desarrollo o el stack completo con un único comando. | Scripts manuales de arranque no garantizan reproducibilidad entre Windows y Linux. |

---

## 2. Patrones arquitectónicos y de diseño aplicados

### 2.1 API Gateway
- **Dónde:** `Backend/APIGateway` (`application.yml`, `JwtAuthenticationFilter`, `CorrelationIdFilter`).
- **Por qué:** centraliza en un solo borde el enrutamiento (incluyendo solapes `/api/eventos/{id}/disponibilidad`, `/cupos`, `/fila/**`, `/validaciones`, `/recomendaciones`), la validación de firma/expiración JWT, la matriz RBAC por rol, CORS y la generación de `X-Correlation-Id`. Descarta cualquier header `X-User-*` enviado por el cliente y lo reemplaza con los claims verificados del token.

### 2.2 Database per Service + Referencias lógicas
- **Dónde:** `Backend/infrastructure/{mysql,mongodb,redis}/`.
- **Por qué:** cada microservicio es dueño exclusivo de su esquema (5 bases MySQL, 1 MongoDB, 1 Redis). No existen claves foráneas físicas entre bases; las relaciones cruzan servicios únicamente mediante UUIDs lógicos (`idEvento`, `idCliente`, `idSector`, `idCompra`).

### 2.3 Patrón Adapter (Puertos y Adaptadores)
- **Dónde:**
  - `msvc-eventos`: puerto `GeocodingProvider` + adaptador `MockGeocodingAdapter` (seleccionable por propiedad `ticketya.geocoding.provider`).
  - `msvc-notificaciones`: puertos de entrega de correo y push con adaptadores `SmtpEmailProvider` / `MockEmailProvider` y `MockPushProvider`.
- **Por qué:** aísla el dominio de servicios externos (geocodificación, SMTP, push), permite sustituir el proveedor por configuración sin tocar lógica de negocio y facilita pruebas deterministas sin internet.

### 2.4 Resiliencia: Timeout, Retry y Fallback
- **Dónde:**
  - `msvc-eventos` (`GeocodingClient`): envuelve al proveedor con timeout por intento (`2000 ms`), hasta 2 reintentos y **fallback silencioso** (`Optional.empty()`) para permitir la carga manual de coordenadas si el servicio externo falla.
  - `msvc-ventas` (`EmisionEntradasService`): ante fallo de `msvc-entradas`, ejecuta 3 reintentos inmediatos y delega en un job `@Scheduled` cada 30 s para garantizar la emisión eventual.
  - `msvc-entradas` (consulta de entradas): enriquecimiento *best-effort* de nombre/lugar del evento desde `msvc-eventos`; si falla, la entrada y su QR siguen siendo accesibles.
  - `msvc-ventas` (reportes): política *fail-fast* (`503 Service Unavailable`) si `eventos` o `entradas` no responden, evitando mostrar recaudación u ocupación parcial engañosa.

### 2.5 Cache-Aside con TTL y política de no-cacheo
- **Dónde:**
  - `GeocodingClient` (`msvc-eventos`): caché en memoria con TTL de 60 min indexada por `direccion|ciudad` normalizada.
  - `StockCache` (`msvc-ventas`, Redis): holds de reserva (TTL 10 min) e `Idempotency-Key` (TTL 24 h).
- **Qué NO se cachea (decisión crítica):** la **disponibilidad de cupos y asientos** (`cupo_sector`, `asiento_reservado`) **nunca se cachea**, porque cualquier dato obsoleto induciría reservas fallidas o riesgo de doble venta bajo concurrencia.

### 2.6 Idempotency-Key e idempotencia natural
- **Dónde:**
  - `POST /compras` (`msvc-ventas`): exige header `Idempotency-Key` (Redis 24 h).
  - `POST /entradas/emision` (`msvc-entradas`) y consumidor `preferencias.compras` (`msvc-preferencias`): idempotentes por `idCompra`.
  - Consumidores de `msvc-notificaciones`: idempotentes por `messageId` + `clave_mensaje` + canal.
- **Por qué:** permite reintentar operaciones ante timeouts de red o redelivery de RabbitMQ sin duplicar compras, entradas ni notificaciones.

### 2.7 Pessimistic Locking + Unique Constraint (Defensa en profundidad)
- **Dónde:** `msvc-ventas` (`cupo_sector` con `PESSIMISTIC_WRITE` + `UK` en `asiento_reservado`) y `msvc-entradas` (`SELECT ... FOR UPDATE` por QR al validar ingreso).
- **Por qué:** garantiza ausencia de doble venta y de doble ingreso incluso con múltiples instancias concurrentes del microservicio. Verificado con pruebas de concurrencia sobre MySQL real (`docs/testing/concurrencia.md`).

### 2.8 Asientos virtuales (representación dispersa)
- **Dónde:** `msvc-eventos` (`sector.filas` × `sector.asientos_por_fila`) y `msvc-ventas` (`asiento_reservado`).
- **Por qué:** en lugar de pre-generar decenas de miles de filas físicas por estadio/evento, el mapa se calcula desde el layout y solo se persisten los asientos reservados o vendidos. Reduce drásticamente volumen de datos y costo de publicación de eventos.

### 2.9 Publish/Subscribe orientado a eventos + Worker con `SKIP LOCKED`
- **Dónde:** `eventos.exchange` (Topic Exchange en RabbitMQ) y job de envío de `msvc-notificaciones` (`FOR UPDATE SKIP LOCKED`).
- **Por qué:** desacopla efectos secundarios de negocio y permite escalar horizontalmente `msvc-notificaciones` sin que dos réplicas envíen la misma notificación.

### 2.10 Observabilidad: Correlation ID y bitácora de operaciones críticas
- **Dónde:** `CorrelationIdFilter` (Gateway y msvcs) + `MDC` + interceptor Feign (`docs/observabilidad.md`).
- **Por qué:** permite rastrear una operación de punta a punta a través de todos los servicios en `docker compose logs` sin infraestructura adicional.

---

## 3. Simplificaciones justificadas (Consigna §6.D)

| Aspecto | Simplificación adoptada | Justificación |
|---|---|---|
| **Service Discovery (Eureka/Consul)** | No se incorpora; el Gateway y Feign resuelven servicios por DNS interno de Docker Compose (`ms-eventos:8082`, etc.) parametrizado por variables de entorno. | En un despliegue acotado con Docker Compose, DNS de red interna provee descubrimiento suficiente sin sumar un contenedor y punto de fallo extra. |
| **Backend For Frontend (BFF)** | No hay BFF dedicado; la SPA React consume el API Gateway directamente. | Existe un único tipo de cliente (web SPA); el Gateway ya cubre enrutamiento, seguridad y CORS. |
| **Pasarela de pagos y notificaciones Push** | Implementadas mediante adaptadores mock (`APROBAR`/`RECHAZAR` explícito en pagos; `MockPushProvider` en push). | Evita dependencias de terceros y permite reproducir flujos de éxito y rechazo de forma determinista en pruebas y demos. |
| **Personal de acceso por evento** | Cualquier usuario con rol `PERSONAL_ACCESO` puede validar QRs de cualquier evento. | Reduce tablas/pantallas de asignación fuera del alcance de la consigna; la validación crítica (QR válido, del evento correcto, no usado y evento no cancelado) sí se cumple de forma atómica. |
| **Recomendaciones** | Algoritmo híbrido explicable (preferencias explícitas + co-compra sobre historial + fallback por demanda/fecha) con pesos configurables. | Cumple la consigna con resultados interpretables (`motivo` visible en UI) sin necesidad de un pipeline de ML externo. |
| **Log centralizado** | Formato estructurado común con `X-Correlation-Id` agregado vía `docker compose logs` (sin stack ELK/Loki). | Permite trazabilidad completa en el entorno de entrega sin sobrecargar memoria en máquinas de desarrollo. |
