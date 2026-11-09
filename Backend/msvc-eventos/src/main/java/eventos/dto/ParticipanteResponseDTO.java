package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Participante", description = "Artista/banda/elenco; rol y orden solo en contexto de evento")
public class ParticipanteResponseDTO {

    @Schema(description = "UUID del participante", example = "d0000000-0000-4000-8000-000000000001")
    private String id;

    @Schema(description = "Nombre", example = "Indio Solari")
    private String nombre;

    @Schema(description = "Tipo", example = "ARTISTA")
    private String tipo;

    @Schema(description = "Descripción", example = "Cantante y compositor")
    private String descripcion;

    /** Solo en contexto de evento: PRINCIPAL | TELONERO | … */
    @Schema(description = "Rol en el evento (solo en contexto de evento)", example = "PRINCIPAL")
    private String rol;

    /** Solo en contexto de evento. */
    @Schema(description = "Orden de aparición (solo en contexto de evento)", example = "1")
    private Integer orden;
}
