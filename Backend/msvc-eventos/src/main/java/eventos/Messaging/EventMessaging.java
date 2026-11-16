package eventos.Messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventMessaging {

    private String messageId;
    private String eventoId;
    private String tipo;          // EventoCancelado | EventoSuspendido | EventoModificado | EventoPublicado
    private String nombre;
    private LocalDateTime fecha;
    private String cambios;       // Detalle del cambio
    private Instant occurredAt;
    private String ciudad;        // solo EventoPublicado
    private List<String> artistas; // solo EventoPublicado

    /** Compatibilidad con mensajes previos sin ciudad/artistas. */
    public EventMessaging(
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
