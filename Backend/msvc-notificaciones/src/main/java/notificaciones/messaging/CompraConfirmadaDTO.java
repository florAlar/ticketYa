package notificaciones.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Contrato publicado por msvc-ventas en {@code compra.confirmada} cuando el pago queda APROBADO.
 * Se consume en la cola {@code notificaciones.compras}.
 */
public record CompraConfirmadaDTO(
        String messageId,
        String compraId,
        String clienteId,
        String eventoId,
        BigDecimal total,
        String moneda,
        Integer cantidadEntradas,
        Instant occurredAt) {
}
