package entradas.messaging;

import java.math.BigDecimal;

/**
 * Evento de Spring que dispara el aviso de devolución aprobada.
 * Se publica al broker después del commit (ver {@link DevolucionEventPublisher}).
 */
public record DevolucionAprobadaEvent(
        String devolucionId,
        String entradaId,
        String clienteId,
        String eventoId,
        BigDecimal monto) {
}
