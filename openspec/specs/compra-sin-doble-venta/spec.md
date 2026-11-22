# compra-sin-doble-venta Specification

## Purpose

Definir el flujo de compra con reserva temporal, idempotencia y garantía de no doble venta. El sistema requiere reserva breve, límite por cliente y evento, pago simulado y liberación de cupo ante rechazo o vencimiento.

## Requirements

### Requirement: Reserva temporal y compra idempotente

El sistema DEBE crear una reserva temporal al seleccionar evento, sector y cantidad, permitir la compra desde una reserva válida con Idempotency-Key obligatoria, y limitar a 6 entradas por cliente y evento. Reintentar con la misma clave no debe duplicar la compra.

#### Scenario: Compra desde reserva confirmada

- **GIVEN** una reserva válida no vencida y una Idempotency-Key nueva
- **WHEN** el cliente confirma la compra
- **THEN** el sistema crea la compra y consume el cupo reservado

#### Scenario: Reintento con la misma clave

- **GIVEN** una compra ya creada con una Idempotency-Key
- **WHEN** el cliente reintenta con la misma clave
- **THEN** el sistema devuelve la compra existente sin duplicarla

### Requirement: No doble venta por cupo de sector

El sistema DEBE vender por cupo de sector y nunca vender más lugares que la capacidad total, incluso ante solicitudes simultáneas o reintentos de pago. La venta opera por cupo por sector, no por asiento individual elegido.

#### Scenario: Solicitudes simultáneas por el último cupo

- **GIVEN** un sector con un solo cupo disponible
- **WHEN** dos clientes solicitan comprar ese cupo al mismo tiempo
- **THEN** exactamente una de las solicitudes resulta exitosa

### Requirement: Pago simulado con liberación de cupo

El sistema DEBE procesar el pago mock como APROBAR o RECHAZAR. Si aprueba, emite entradas con QR único; si rechaza o vence la reserva, libera los lugares reservados.

#### Scenario: Pago aprobado emite entradas

- **GIVEN** una compra creada desde reserva válida
- **WHEN** el pago mock aprueba
- **THEN** el sistema emite las entradas con QR único

#### Scenario: Pago rechazado libera cupo

- **GIVEN** una compra creada desde reserva válida
- **WHEN** el pago mock rechaza
- **THEN** el sistema no emite entradas y libera el cupo del sector
