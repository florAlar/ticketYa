package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** Lugar liviano para catálogo. */
@Getter
@Setter
@Schema(name = "LugarResumen", description = "Lugar resumido para el catálogo")
public class LugarResumenDTO {

    @Schema(description = "Nombre", example = "Estadio Obras")
    private String nombre;

    @Schema(description = "Ciudad", example = "Buenos Aires")
    private String ciudad;
}
