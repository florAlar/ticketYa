package notificaciones.messaging;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Copia del contrato publicado por msvc-eventos en eventos.exchange.
 * Cambios acá deben coordinarse con ese servicio.
 */
public record EventoCambioDTO(
        String messageId,
        String eventoId,
        String tipo,          // EventoCancelado | EventoSuspendido
        String nombre,
        LocalDateTime fecha,  // fecha del evento (fechaInicio)
        String cambios,
        Instant occurredAt) {
}
