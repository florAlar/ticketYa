package preferencias.document;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RangoPrecio {

    private BigDecimal min;
    private BigDecimal max;
    private String moneda;
}
