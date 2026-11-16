package ventas.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/** Contrato de wire en {@code compra.confirmada}; lo consume msvc-notificaciones. */
public record CompraConfirmadaMessaging(
        String messageId,
        String compraId,
        String clienteId,
        String eventoId,
        BigDecimal total,
        String moneda,
        Integer cantidadEntradas,
        Instant occurredAt) {
}
