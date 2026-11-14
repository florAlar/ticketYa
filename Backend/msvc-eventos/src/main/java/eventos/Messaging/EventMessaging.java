package eventos.Messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventMessaging {

    private String messageId;
    private String eventoId;
    private String tipo;          // EventoCancelado | EventoSuspendido
    private String nombre;
    private LocalDateTime fecha;
    private String cambios;       // Detalle del cambio
    private Instant occurredAt;
}
