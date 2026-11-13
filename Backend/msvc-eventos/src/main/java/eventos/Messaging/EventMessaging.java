package eventos.Messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventMessaging {
    private String eventoId;
    private String tipo;          // CANCELADO | SUSPENDIDO
    private String nombre;
    private LocalDateTime fecha;
    private String cambios;       // por ahora null
}