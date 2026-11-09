package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "EventoEstadoRequest", description = "Nuevo estado del evento")
public class EventoEstadoRequestDTO {

    @Schema(description = "Estado destino: PUBLICADO, SUSPENDIDO, CANCELADO, AGOTADO, FINALIZADO",
            example = "SUSPENDIDO")
    private String estado;
}
