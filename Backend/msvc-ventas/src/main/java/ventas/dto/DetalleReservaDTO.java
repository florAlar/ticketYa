package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "DetalleReserva", description = "Línea de reserva: sector, cantidad y precio unitario")
public class DetalleReservaDTO {

    @Schema(description = "UUID del detalle", example = "22222222-2222-4222-8222-222222222222")
    private String idDetalle;

    @Schema(description = "UUID del sector", example = "c0000000-0000-4000-8000-000000000011")
    private String idSector;

    @Schema(description = "Cantidad reservada", example = "2")
    private Integer cantidad;

    @Schema(description = "Precio unitario", example = "95000.00")
    private BigDecimal precioUnitario;

    @Schema(description = "Asientos reservados (solo sectores numerados; vacío en sectores generales)")
    private List<AsientoDTO> asientos = new ArrayList<>();
}
