package eventos.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Reemplaza el set de participantes del evento.
 * Preferir {@code participantes} (id + rol + orden). Alternativa simple: {@code ids}.
 */
@Getter
@Setter
public class EventoParticipantesRequestDTO {

    /** Lista simple de ids (orden = índice; rol null). */
    private List<String> ids = new ArrayList<>();

    private List<Item> participantes = new ArrayList<>();

    @Getter
    @Setter
    public static class Item {
        private String id;
        private String rol;
        private Integer orden;
    }
}
