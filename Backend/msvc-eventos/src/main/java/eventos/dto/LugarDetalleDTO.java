package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "LugarDetalle", description = "Lugar embebido en el detalle del evento")
public class LugarDetalleDTO {

    @Schema(description = "UUID del lugar", example = "f0000000-0000-4000-8000-000000000001")
    private String id;

    @Schema(description = "Nombre", example = "Estadio Obras")
    private String nombre;

    @Schema(description = "Dirección", example = "Av. del Libertador 7395")
    private String direccion;

    @Schema(description = "Ciudad", example = "Buenos Aires")
    private String ciudad;

    @Schema(description = "Sectores del lugar")
    private List<SectorEventoDTO> sectores = new ArrayList<>();
}
