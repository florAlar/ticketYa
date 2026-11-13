package eventos.Messaging;

import java.time.LocalDateTime;

public record EventChangeEvent(String eventoId, String tipo, String nombre,
                               LocalDateTime fecha, String cambios) {}