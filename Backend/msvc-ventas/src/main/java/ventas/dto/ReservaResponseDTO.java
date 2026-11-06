package ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservaResponseDTO {

    private String id;
    private String idCliente;
    private String idEvento;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaExpiracion;
    private String estado;
    private BigDecimal total;
    private List<DetalleReservaDTO> detalles = new ArrayList<>();
}
