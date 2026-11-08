package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Sector del lugar con precio del evento (si hay precio_sector activo). */
@Getter
@Setter
@Schema(name = "SectorEvento", description = "Sector del lugar con el precio vigente del evento")
public class SectorEventoDTO {

    @Schema(description = "UUID del sector", example = "a0000000-0000-4000-8000-000000000001")
    private String id;

    @Schema(description = "Nombre del sector", example = "Campo")
    private String nombre;

    @Schema(description = "Tipo de sector", example = "CAMPO")
    private String tipo;

    @Schema(description = "Capacidad", example = "500")
    private Integer capacidad;

    @Schema(description = "Sector numerado", example = "false")
    private Boolean esNumerado;

    @Schema(description = "Precio vigente", example = "25000.00")
    private BigDecimal precio;

    @Schema(description = "Moneda", example = "ARS")
    private String moneda;
}
