package eventos.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Sector del lugar con precio del evento (si hay precio_sector activo). */
@Getter
@Setter
public class SectorEventoDTO {

    private String id;
    private String nombre;
    private String tipo;
    private Integer capacidad;
    private Boolean esNumerado;
    private BigDecimal precio;
    private String moneda;
}
