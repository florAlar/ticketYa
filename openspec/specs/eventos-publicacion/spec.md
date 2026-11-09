# eventos-publicacion Specification

## Purpose

Definir el ciclo de vida de eventos, lugares y sectores, y las condiciones para que un evento pase a estar disponible para compra. El sistema requiere borrador flexible, publicación validada y apertura de cupo comercial al publicar.

## Requirements

### Requirement: Ciclo de vida del evento

El sistema DEBE modelar los eventos con estados: BORRADOR, PUBLICADO, AGOTADO, SUSPENDIDO, CANCELADO y FINALIZADO. La creación en borrador solo requiere nombre; la publicación exige fecha de inicio futura, lugar real, al menos un sector con precio y capacidad mayor o igual a 1, sin cupo ilimitado.

#### Scenario: Publicar con requisitos completos

- **GIVEN** un evento en BORRADOR con fecha futura, lugar real y un sector con precio y capacidad
- **WHEN** el dueño ORGANIZADOR o un ADMIN solicita la publicación
- **THEN** el evento pasa a PUBLICADO y se abre el stock de venta por sector

#### Scenario: Publicar sin sector con precio

- **GIVEN** un evento en BORRADOR sin ningún sector con precio y capacidad
- **WHEN** se solicita la publicación
- **THEN** el sistema rechaza la publicación

### Requirement: Catálogo público de eventos publicados

El sistema DEBE mostrar en el catálogo público los eventos PUBLICADO y permitir buscar por tipo, organizador, participante, ciudad y rango de fechas, con orden y paginación.

#### Scenario: Búsqueda de eventos publicados

- **GIVEN** eventos en estado PUBLICADO y otros en BORRADOR
- **WHEN** un visitante consulta el catálogo con filtros de tipo y ciudad
- **THEN** el sistema devuelve solo eventos PUBLICADO que cumplen los filtros

### Requirement: Suspensión y cancelación

El sistema DEBE permitir suspender los propios eventos al ORGANIZADOR dueño y cualquier evento a ADMIN. La cancelación corresponde a ADMIN e invalida los QR de entradas vendidas.

#### Scenario: Suspender evento propio

- **GIVEN** un ORGANIZADOR dueño de un evento PUBLICADO
- **WHEN** solicita suspenderlo
- **THEN** el evento pasa a SUSPENDIDO

#### Scenario: Cancelación invalida QR

- **GIVEN** un evento PUBLICADO con entradas VENDIDA
- **WHEN** un ADMIN cancela el evento
- **THEN** el evento pasa a CANCELADO y los QR dejan de ser utilizables
