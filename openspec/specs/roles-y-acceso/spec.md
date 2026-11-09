# roles-y-acceso Specification

## Purpose

Definir la identidad y el control de acceso de TicketYa para que cada operación se ejecute solo cuando el rol del solicitante lo permite. El sistema requiere registro público limitado, autenticación JWT centralizada en el gateway y autorización por rol.

## Requirements

### Requirement: Registro público limitado a CLIENTE

El sistema DEBE permitir el alta pública únicamente con rol CLIENTE. Los roles ORGANIZADOR, ADMIN y PERSONAL_ACCESO no se obtienen por registro público.

#### Scenario: Alta pública crea CLIENTE

- **GIVEN** un visitante no autenticado
- **WHEN** registra una cuenta con sus datos personales
- **THEN** el sistema crea la cuenta con rol CLIENTE

#### Scenario: Alta como ADMIN no permitida por vía pública

- **GIVEN** un visitante no autenticado
- **WHEN** intenta registrarse con rol ADMIN
- **THEN** el sistema rechaza el alta

### Requirement: Autenticación JWT validada en el gateway

El sistema DEBE validar el JWT en el gateway como único punto de entrada externo y propagar a los microservicios la identidad y los roles ya resueltos. El frontend solo se comunica con el gateway.

#### Scenario: Petición con JWT válido

- **GIVEN** un usuario autenticado con JWT válido
- **WHEN** realiza una petición al gateway
- **THEN** el gateway propaga su identidad y roles al microservicio destino

#### Scenario: Petición sin JWT o con JWT inválido

- **GIVEN** una petición sin JWT o con JWT inválido
- **WHEN** llega al gateway y el recurso exige autenticación
- **THEN** el sistema rechaza la petición

### Requirement: Autorización por rol según la acción

El sistema DEBE autorizar cada acción según el rol: ver catálogo es público; crear o editar eventos y publicar corresponde al ORGANIZADOR dueño o ADMIN; suspender eventos corresponde al ORGANIZADOR dueño o ADMIN; cancelar un evento corresponde a ADMIN; aprobar o rechazar devoluciones corresponde a ADMIN; validar ingreso con QR corresponde a PERSONAL_ACCESO.

#### Scenario: ORGANIZADOR publica su propio evento

- **GIVEN** un ORGANIZADOR dueño de un evento en borrador
- **WHEN** solicita la publicación con los requisitos cumplidos
- **THEN** el sistema permite la publicación

#### Scenario: CLIENTE intenta cancelar un evento

- **GIVEN** un CLIENTE autenticado
- **WHEN** solicita la cancelación de un evento
- **THEN** el sistema rechaza la operación

#### Scenario: ADMIN cancela cualquier evento

- **GIVEN** un ADMIN autenticado
- **WHEN** solicita la cancelación de un evento
- **THEN** el sistema permite la cancelación
