# entradas-qr-devoluciones Specification

## Purpose

Definir la emisión y el uso de entradas con QR, las devoluciones pedidas por el cliente y el efecto de la cancelación de eventos. El sistema requiere QR único reutilizable solo mientras la entrada está VENDIDA y el evento no está cancelado.

## Requirements

### Requirement: Emisión de entradas con QR único

El sistema DEBE emitir, tras un pago aprobado, una entrada con QR único por cada lugar comprado. La emisión por compra debe ser idempotente: reintentar no duplica entradas. Si la emisión falla tras el pago, la compra queda pendiente de emisión y se reintenta automáticamente sin duplicar.

#### Scenario: Compra aprobada con emisión exitosa

- **GIVEN** una compra con pago aprobado
- **WHEN** el sistema emite las entradas
- **THEN** cada entrada posee un QR único utilizable

#### Scenario: Reintento de emisión no duplica

- **GIVEN** una compra con emisión ya realizada
- **WHEN** la emisión se reintenta para la misma compra
- **THEN** el sistema no crea entradas duplicadas

### Requirement: QR utilizable solo en entrada VENDIDA

El sistema DEBE exponer el código QR usable solo si la entrada está VENDIDA y el evento no está cancelado. Tras una devolución aprobada o un reintegro por cancelación, el QR queda invalido. Un QR no debe ingresarse dos veces.

#### Scenario: Consulta de QR de entrada vendida

- **GIVEN** una entrada VENDIDA cuyo evento no está cancelado
- **WHEN** el dueño consulta su QR
- **THEN** el sistema expone el código QR usable

#### Scenario: Consulta de QR tras devolución

- **GIVEN** una entrada DEVUELTA
- **WHEN** el dueño consulta su QR
- **THEN** el sistema no expone el código QR como usable

### Requirement: Devolución con aprobación del ADMIN

El sistema DEBE permitir al cliente dueño solicitar devolución solo de entradas VENDIDA no usada dentro de las 72 horas desde la emisión. ADMIN aprueba o rechaza; si aprueba, la entrada pasa a DEVUELTA, el QR se invalida, se libera un lugar del sector y se realiza reintegro mock.

#### Scenario: Devolución aprobada dentro de plazo

- **GIVEN** una entrada VENDIDA no usada dentro del plazo de 72 horas
- **WHEN** el cliente solicita la devolución y ADMIN la aprueba
- **THEN** la entrada pasa a DEVUELTA, el QR se invalida y se libera el cupo

#### Scenario: Devolución rechazada conserva la entrada

- **GIVEN** una entrada VENDIDA
- **WHEN** ADMIN rechaza la devolución solicitada
- **THEN** la entrada permanece VENDIDA sin cambios
