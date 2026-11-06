package entradas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DevolucionResponseDTO {
    private String id;
    private String idEntrada;
    private String idCliente;
    private String motivo;
    private BigDecimal monto;
    private LocalDateTime fechaSolicitud;
    private String estado;
}
