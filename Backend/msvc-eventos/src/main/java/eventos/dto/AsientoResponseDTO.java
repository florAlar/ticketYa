package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Asiento", description = "Asiento de un sector numerado")
public class AsientoResponseDTO {

    @Schema(description = "UUID del asiento", example = "a1000000-0000-4000-8000-000000000001")
    private String id;

    @Schema(description = "UUID del sector", example = "a0000000-0000-4000-8000-000000000001")
    private String idSector;

    @Schema(description = "Fila", example = "A")
    private String fila;

    @Schema(description = "Número", example = "12")
    private Integer numero;

    @Schema(description = "Código legible", example = "A-12")
    private String codigo;
}
