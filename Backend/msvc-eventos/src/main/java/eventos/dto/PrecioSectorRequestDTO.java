package eventos.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrecioSectorRequestDTO {
    private BigDecimal precio;
    private String moneda;
    private Boolean activo;
}
