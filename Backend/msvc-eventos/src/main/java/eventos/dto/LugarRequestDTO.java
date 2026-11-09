package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "LugarRequest", description = "Alta o edición de un lugar")
public class LugarRequestDTO {

    @Schema(description = "Nombre del lugar", example = "Estadio Obras")
    private String nombre;

    @Schema(description = "Dirección", example = "Av. del Libertador 7395")
    private String direccion;

    @Schema(description = "Ciudad", example = "Buenos Aires")
    private String ciudad;

    @Schema(description = "Capacidad total", example = "4500")
    private Integer capacidadTotal;
}
