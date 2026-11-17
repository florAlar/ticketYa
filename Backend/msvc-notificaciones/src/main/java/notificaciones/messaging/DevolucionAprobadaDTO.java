package notificaciones.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Contrato publicado por msvc-entradas en {@code devolucion.aprobada} al aprobarse una devolución.
 * Se consume en la cola {@code notificaciones.devoluciones}.
 */
public record DevolucionAprobadaDTO(
        String messageId,
        String devolucionId,
        String entradaId,
        String clienteId,
        String eventoId,
        BigDecimal monto,
        String moneda,
        Instant occurredAt) {
}
