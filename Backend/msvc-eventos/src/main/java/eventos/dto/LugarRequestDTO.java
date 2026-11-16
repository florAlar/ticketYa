package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
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

    @Schema(description = "Latitud en grados decimales (-90 a 90). Opcional; va junto con la longitud", example = "-34.545300")
    private BigDecimal latitud;

    @Schema(description = "Longitud en grados decimales (-180 a 180). Opcional; va junto con la latitud", example = "-58.449700")
    private BigDecimal longitud;

    @Schema(description = "true borra las coordenadas del lugar (no se combina con latitud/longitud)", example = "false")
    private Boolean quitarCoordenadas;

    @Schema(description = "Capacidad total", example = "4500")
    private Integer capacidadTotal;
}
