package eventos.Messaging;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Evento de Spring (no de Rabbit) que se publica dentro de la transacción y se envía al broker
 * después del commit en {@link EventChangePublisher}.
 */
public record EventChangeEvent(
        String eventoId,
        String tipo,
        String nombre,
        LocalDateTime fecha,
        String cambios,
        String ciudad,
        List<String> artistas) {

    public EventChangeEvent(
            String eventoId, String tipo, String nombre, LocalDateTime fecha, String cambios) {
        this(eventoId, tipo, nombre, fecha, cambios, null, List.of());
    }
}
