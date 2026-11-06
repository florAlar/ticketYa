package ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompraResponseDTO {

    private String id;
    private String idReserva;
    private String idCliente;
    private LocalDateTime fechaCompra;
    private BigDecimal total;
    private String moneda;
    private String estado;
}
