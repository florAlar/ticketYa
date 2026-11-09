package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "ParticipanteRequest", description = "Alta o edición de artista/banda/elenco")
public class ParticipanteRequestDTO {

    @Schema(description = "Nombre", example = "Indio Solari")
    private String nombre;

    /** ARTISTA | BANDA | ELENCO | … */
    @Schema(description = "Tipo: ARTISTA | BANDA | ELENCO", example = "ARTISTA")
    private String tipo;

    @Schema(description = "Descripción", example = "Cantante y compositor")
    private String descripcion;
}
