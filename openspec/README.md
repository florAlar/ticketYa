# OpenSpec

Especificaciones de comportamiento generadas con [OpenSpec](https://github.com/Fission-AI/OpenSpec) al inicio del proyecto (2026-11-09), en formato Given/When/Then.

| Spec | Tema |
| --- | --- |
| [`compra-sin-doble-venta`](specs/compra-sin-doble-venta/spec.md) | Reserva temporal, compra idempotente y no doble venta |
| [`entradas-qr-devoluciones`](specs/entradas-qr-devoluciones/spec.md) | Emisión de entradas con QR y devoluciones |
| [`eventos-publicacion`](specs/eventos-publicacion/spec.md) | Alta, publicación y estados de eventos |
| [`roles-y-acceso`](specs/roles-y-acceso/spec.md) | Roles y control de acceso |

## Estado

Son el **punto de partida histórico** del flujo spec-driven y **no se actualizaron** con las decisiones posteriores. Por ejemplo, describen la venta solo por cupo de sector, y desde 2026-11-10 los sectores numerados se venden por asiento concreto (asientos virtuales).

La fuente vigente de reglas, endpoints y diseño es [`Backend/specs/`](../Backend/specs/README.md), que además describe el flujo de trabajo del equipo. Ante cualquier diferencia, prevalece `Backend/specs/`.
