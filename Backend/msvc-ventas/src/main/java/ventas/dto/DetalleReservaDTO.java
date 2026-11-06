package ventas.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetalleReservaDTO {

    private String idDetalle;
    private String idSector;
    private Integer cantidad;
    private BigDecimal precioUnitario;
}
