package notificaciones.messaging;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Copia del contrato publicado por msvc-eventos en eventos.exchange.
 * Cambios acá deben coordinarse con ese servicio (ver specs/comunicacion.md).
 *
 * <p>{@code ciudad} y {@code artistas} solo vienen informados en {@code EventoPublicado}; sirven
 * para cruzar el evento con los artistas/ciudades favoritos del cliente.
 */
public record EventoCambioDTO(
        String messageId,
        String eventoId,
        String tipo,          // EventoCancelado | EventoSuspendido | EventoModificado | EventoPublicado
        String nombre,
        LocalDateTime fecha,  // fecha del evento (fechaInicio)
        String cambios,
        Instant occurredAt,
        String ciudad,
        List<String> artistas) {

    /** Compatibilidad con payloads previos que no traían ciudad/artistas. */
    public EventoCambioDTO(
            String messageId,
            String eventoId,
            String tipo,
            String nombre,
            LocalDateTime fecha,
            String cambios,
            Instant occurredAt) {
        this(messageId, eventoId, tipo, nombre, fecha, cambios, occurredAt, null, List.of());
    }
}
