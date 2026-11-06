package ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PagoResponseDTO {

    private String id;
    private String idCompra;
    private String proveedor;
    private String idTransaccionExterna;
    private BigDecimal monto;
    private String estado;
    private LocalDateTime fechaPago;
}
