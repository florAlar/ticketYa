package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "SectorRequest", description = "Alta o edición de un sector del lugar")
public class SectorRequestDTO {

    @Schema(description = "Nombre del sector", example = "Platea alta")
    private String nombre;

    @Schema(description = "Capacidad", example = "300")
    private Integer capacidad;

    @Schema(description = "Tipo de sector", example = "PLATEA")
    private String tipo;

    @Schema(description = "Sector numerado", example = "true")
    private Boolean esNumerado;
}
