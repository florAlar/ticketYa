package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "EventoParticipantesRequest", description = "Set completo de participantes a asociar al evento")
public class EventoParticipantesRequestDTO {

    /** Lista simple de ids (orden = índice; rol null). */
    @Schema(description = "Ids de participantes (orden = índice, rol null)",
            example = "[\"d0000000-0000-4000-8000-000000000001\"]")
    private List<String> ids = new ArrayList<>();

    @Schema(description = "Participantes con rol y orden (preferido)")
    private List<Item> participantes = new ArrayList<>();

    @Getter
    @Setter
    @Schema(name = "EventoParticipanteItem", description = "Participante asociado con rol y orden")
    public static class Item {

        @Schema(description = "UUID del participante", example = "d0000000-0000-4000-8000-000000000001")
        private String id;

        @Schema(description = "Rol en el evento: PRINCIPAL | TELONERO | …", example = "PRINCIPAL")
        private String rol;

        @Schema(description = "Orden de aparición", example = "1")
        private Integer orden;
    }
}
