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

    @Schema(description = "Filas del sector numerado (obligatorio si esNumerado; 1..200)", example = "20")
    private Integer filas;

    @Schema(description = "Asientos por fila del sector numerado (obligatorio si esNumerado; 1..500). "
            + "La capacidad del sector es filas * asientosPorFila", example = "15")
    private Integer asientosPorFila;
}
