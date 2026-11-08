package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "PrecioSectorRequest", description = "Precio de un sector para el evento")
public class PrecioSectorRequestDTO {

    @Schema(description = "Precio unitario", example = "25000.00")
    private BigDecimal precio;

    @Schema(description = "Moneda ISO-4217", example = "ARS")
    private String moneda;

    @Schema(description = "Precio activo", example = "true")
    private Boolean activo;
}
