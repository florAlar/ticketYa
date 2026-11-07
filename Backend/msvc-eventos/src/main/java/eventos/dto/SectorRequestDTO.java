package eventos.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectorRequestDTO {
    private String nombre;
    private Integer capacidad;
    private String tipo;
    private Boolean esNumerado;
}
