package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Lugar", description = "Lugar con sus sectores")
public class LugarResponseDTO {

    @Schema(description = "UUID del lugar", example = "f0000000-0000-4000-8000-000000000001")
    private String id;

    @Schema(description = "Nombre", example = "Estadio Obras")
    private String nombre;

    @Schema(description = "Dirección", example = "Av. del Libertador 7395")
    private String direccion;

    @Schema(description = "Ciudad", example = "Buenos Aires")
    private String ciudad;

    @Schema(description = "Latitud en grados decimales (-90 a 90). Opcional; va junto con la longitud", example = "-34.545300")
    private BigDecimal latitud;

    @Schema(description = "Longitud en grados decimales (-180 a 180). Opcional; va junto con la latitud", example = "-58.449700")
    private BigDecimal longitud;

    @Schema(description = "Capacidad total", example = "4500")
    private Integer capacidadTotal;

    @Schema(description = "Sectores del lugar")
    private List<SectorEventoDTO> sectores = new ArrayList<>();
}
