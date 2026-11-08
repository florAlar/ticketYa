# TicketYa — Consigna del Trabajo Práctico

## 1. Objetivo del sistema

Desarrollar una plataforma de venta de entradas para eventos (recitales, festivales, teatro, deportes, conferencias, etc.), orientada a organizadores y clientes.

El sistema debe permitir publicar eventos, buscar y comprar entradas, gestionar reservas, pagos, devoluciones y controlar el acceso mediante códigos QR.

**Desafío principal:** soportar eventos de alta demanda, garantizando disponibilidad, orden de acceso y evitando la doble venta de entradas, incluso ante solicitudes simultáneas.

### Tipos de usuarios
- **Administrador:** gestión completa, mantenimiento, tareas operativas y administración de eventos.
- **Organizador:** creación, configuración, publicación y consulta de reportes de eventos.
- **Cliente:** búsqueda, compra y devolución de entradas.
- **Personal de acceso:** validación de entradas mediante lectura de códigos QR.

## 2. Funcionalidades del sistema

### Gestión de eventos y lugares
1. **Eventos:** ID, nombre, descripción, tipo, organizador, participantes, fecha, hora, lugar y estado (borrador, publicado, agotado, cancelado o finalizado).
2. **Lugares:** ID, nombre, dirección, coordenadas geográficas y sectores. Los sectores pueden ser:
    - Numerados: filas y asientos identificados.
    - No numerados: zonas con capacidad máxima.
    - Cada evento define sus propios precios por sector.
3. **Publicación:** los organizadores crean eventos en borrador, configuran sectores y precios y los publican. Los administradores pueden suspender o cancelar cualquier evento.

### Compra y gestión de entradas
4. **Compra con reserva temporal:**
    - El cliente selecciona evento, sector y, si corresponde, asientos.
    - El sistema reserva temporalmente los lugares (por ejemplo, 10 minutos).
    - Si el pago se aprueba, emite entradas con QR único.
    - Si el pago se rechaza o vence la reserva, libera los lugares.
    - Registra el detalle de compra y medio de pago.
    - Limita la cantidad de entradas por cliente y evento.
5. **No doble venta:** una entrada o lugar nunca puede venderse dos veces, incluso ante compras simultáneas o reintentos de pago por problemas de conexión.

### Funcionalidades adicionales
6. **Fila virtual:** para eventos de alta demanda, los clientes ingresan a una cola que informa su posición y habilita la compra por orden de llegada y por tandas.
7. **Devoluciones y cancelaciones:**
    - El cliente puede solicitar devolución hasta 72 horas antes del evento.
    - El administrador aprueba o rechaza la solicitud.
    - Si se aprueba, se libera el lugar y se solicita el reintegro a la pasarela de pagos.
    - Si se cancela un evento, todas las entradas vendidas pasan a reintegro y se notifica a los compradores.
8. **Control de acceso QR:** el personal escanea la entrada. El sistema verifica que exista, corresponda al evento y no haya sido utilizada. Luego la marca como utilizada. Un QR no puede ingresar dos veces.
9. **Búsqueda:** por tipo de evento, organizador, participante, ciudad y rango de fechas. Permite ordenar por fecha, precio o relevancia.
10. **Preferencias y recomendaciones:** registra tipos de eventos, artistas favoritos y ciudad del cliente. Sugiere eventos según su historial y compras de otros usuarios.
11. **Notificaciones (email o push):**
    - Confirmación de compra.
    - Cancelación o modificación de eventos.
    - Publicación de eventos de artistas favoritos en la ciudad del cliente.
12. **Reportes para organizadores:** entradas vendidas y devueltas por sector, porcentaje de ocupación y recaudación.

## 3. Requisitos de arquitectura y tecnologías

### Arquitectura
- Arquitectura de **microservicios**, cada uno como proyecto independiente en Spring Boot.
- **API Gateway** como único punto de entrada para clientes externos.
- Comunicación mediante APIs REST, principalmente síncrona. Se permite comunicación asíncrona cuando esté justificada (notificaciones y fila virtual, por ejemplo).
- Cada microservicio debe tener preferentemente su propia base de datos o esquema independiente.

### Persistencia
- Al menos un microservicio debe utilizar una base NoSQL (por ejemplo, MongoDB para catálogo o preferencias), justificando la elección.
- Se valorará el uso de tecnologías adecuadas a cada necesidad, como Redis para reservas temporales o fila virtual.

### Requisitos no funcionales
- **Concurrencia e idempotencia:** impedir la doble venta incluso con múltiples instancias del mismo microservicio. Explicar y probar el mecanismo elegido (bloqueos, control de versiones, restricciones de unicidad, claves de idempotencia, etc.).
- **Consistencia entre servicios:** definir cómo mantener la consistencia en compras que involucran disponibilidad, pagos y emisión de entradas. Establecer compensaciones para deshacer operaciones si algún paso falla.
- **Resiliencia:** implementar reintentos, timeouts y fallback ante fallos de servicios externos.
- **Cache:** definir qué información se cachea, dónde, TTL, invalidación y qué datos no deben cachearse (por ejemplo, disponibilidad de asientos).
- **Trazabilidad:** registrar cada solicitud HTTP con endpoint, usuario, código de respuesta y tiempo de procesamiento. Las operaciones críticas (compra, devolución, cancelación e ingreso) deben quedar en un log centralizado con identificador para seguir la operación entre microservicios.
- **Seguridad:** autenticación y autorización mediante JWT y roles: cliente, organizador, administrador y personal de acceso.
- **Despliegue:** cada microservicio, base de datos y gateway debe poder ejecutarse en su propio contenedor Docker. El sistema completo debe levantarse con un único `docker-compose.yml`.
- **Rendimiento:** realizar al menos una prueba de carga del flujo de compra (por ejemplo, con JMeter) e informar resultados.

## 4. Modalidad de entrega

El trabajo se desarrolla en grupo, sobre el mismo repositorio Git, y se divide en dos entregas incrementales.

| Aspecto | Entrega 1 — Core | Entrega 2 — Sistema completo |
|---|---|---|
| Funcionalidad | Eventos, lugares, publicación, compra, reserva temporal, QR, no doble venta, devoluciones, cancelaciones y búsquedas. | Fila virtual, control de acceso QR, preferencias, recomendaciones, notificaciones y reportes. |
| Arquitectura | Microservicios Spring Boot, REST síncrono y API Gateway. | Se amplía la arquitectura según las funcionalidades restantes. |
| Bases de datos | Una base relacional por microservicio. | Incorporar al menos una base NoSQL (MongoDB). |
| Concurrencia | Mecanismo probado contra doble venta. | Mantener y ampliar las garantías de consistencia. |
| Pagos | Pasarela de pagos simulada (mock). | Completar integración y manejo de fallos según corresponda. |
| Seguridad | Sin autenticación formal. Identificación por parámetro o encabezado. | JWT y autorización por roles. |
| Despliegue | Ejecución local de microservicios. Las bases pueden ejecutarse en Docker. | Cada microservicio en su contenedor y orquestación completa con Docker Compose. |
| Testing | Pruebas unitarias y prueba de no doble venta. | Pruebas de integración, seguridad y carga. |

## 5. Tareas y entregables — Entrega 1

La primera entrega debe implementar el núcleo funcional completo de publicación, búsqueda y compra sin doble venta.

### A. Features y User Stories
- Transformar los requerimientos en features y user stories de todo el sistema.
- Incluir criterios de aceptación para las historias.

### B. Arquitectura de microservicios
- Definir microservicios, responsabilidades y APIs públicas.
- Diagrama de módulos de la arquitectura.
- Diagrama de secuencia de compra de una entrada numerada, contemplando pago rechazado y vencimiento de reserva.

### C. Modelo de datos
- Diagrama entidad-relación por microservicio.
- Indicar claves y relaciones entre servicios.
- Identificar entidades solapadas entre bases de datos.

### D. Diseño de API REST
- Listado de endpoints con método, ruta, parámetros y códigos de respuesta.
- Ejemplos JSON de request y response.
- Documentación OpenAPI / Swagger con descripciones, ejemplos y códigos de error.

### E. Implementación
- Desarrollar en Spring Boot los microservicios y API Gateway necesarios para todas las funcionalidades de la primera entrega.
- Incluir README con instrucciones de ejecución y documentación técnica adicional.

### F. Testing
- Pruebas unitarias de lógica de negocio: compra, devoluciones y búsqueda.
- Prueba de concurrencia que simule muchas compras simultáneas del mismo asiento o último lugar de una zona y verifique que solo una resulte exitosa.

### Entregable 1
Repositorio Git con:
- Código fuente.
- Especificaciones versionadas, si se utiliza OpenSpec u otra metodología similar.
- Documentación de diseño.
- README con instrucciones de ejecución.
- Datos de prueba.
- Colección de requests (Postman o similar) para probar endpoints.

## 6. Tareas y entregables — Entrega 2

La segunda entrega completa las funcionalidades restantes y los requisitos no funcionales.

### A. Features y User Stories
- Completar criterios de aceptación de las historias restantes.
- Actualizar las historias que hayan cambiado.

### B. Arquitectura y modelo de datos
- Actualizar diagramas de módulos y entidad-relación incorporando nuevos servicios y base NoSQL.
- Definir estrategias de consistencia (eventual o fuerte) y manejo de fallos parciales mediante compensaciones.

### C. Decisiones tecnológicas
- Justificar tecnologías elegidas, especialmente la base NoSQL y los datos que almacena.
- Justificar patrones arquitectónicos utilizados: API Gateway, Circuit Breaker, Backend For Frontend, Saga, Service Discovery, etc.

### D. Implementación
- Desarrollar las funcionalidades restantes.
- La fila virtual y las recomendaciones pueden implementarse de forma simplificada, justificando las simplificaciones.
- Incorporar JWT con roles, base NoSQL, cache, resiliencia y bitácora/log centralizado.
- Empaquetar cada microservicio en un contenedor Docker independiente.
- Orquestar todo el sistema mediante Docker Compose.

### E. Diseño de API REST
- Actualizar documentación OpenAPI con nuevos endpoints y requisitos de seguridad.

### F. Testing
- Pruebas de integración entre microservicios (por ejemplo, Testcontainers o Docker Compose).
- Pruebas de autorización que verifiquen que cada endpoint sea accesible únicamente por los roles correspondientes.
- Prueba de carga del flujo de compra e informe con:
    - Cantidad de usuarios concurrentes.
    - Tiempos de respuesta.
    - Errores.

### Entregable 2
Repositorio Git actualizado con:
- Código fuente.
- `docker-compose.yml` para levantar todos los servicios, bases de datos y gateway.
- Especificaciones y documentación de diseño actualizadas.
- README con instrucciones de ejecución y descripción del diseño.
- Colección de requests actualizada.
- Informe de prueba de carga.
- Si existe interfaz gráfica, instrucciones para ejecutarla.

## 7. Metodología sugerida: Spec-Driven Development

Se recomienda trabajar mediante especificaciones escritas y revisadas por el grupo antes de implementar cada funcionalidad.

Flujo sugerido:

**Propuesta → Requisitos y escenarios → Diseño → Tareas → Implementación**

Se puede utilizar OpenSpec u otra herramienta similar. Las especificaciones deben quedar versionadas junto al código y servir como base para contratos OpenAPI y casos de prueba. Si se utilizan agentes de IA para programar, deben trabajar guiados por estas especificaciones.

## 8. Interfaz gráfica

El foco principal es el backend, pero se valora positivamente contar con una interfaz web (React, Angular, Vue o Thymeleaf) que consuma la API mediante el Gateway.

Como mínimo, sería deseable cubrir los flujos principales del cliente:
- Buscar eventos.
- Elegir sector y asientos.
- Comprar entradas.
- Consultar entradas adquiridas.

---

**Enfoque general:** la Entrega 1 construye el circuito central y garantiza que la compra funcione correctamente sin doble venta. La Entrega 2 amplía el sistema con funcionalidades complementarias, seguridad, escalabilidad, resiliencia, observabilidad y despliegue completo.