package entradas.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Contrato de wire en {@code devolucion.aprobada}; lo consume msvc-notificaciones.
 * Ver specs/comunicacion.md.
 */
public record DevolucionAprobadaMessaging(
        String messageId,
        String devolucionId,
        String entradaId,
        String clienteId,
        String eventoId,
        BigDecimal monto,
        String moneda,
        Instant occurredAt) {
}
