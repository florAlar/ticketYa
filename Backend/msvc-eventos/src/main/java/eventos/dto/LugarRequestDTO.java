package eventos.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LugarRequestDTO {
    private String nombre;
    private String direccion;
    private String ciudad;
    private Integer capacidadTotal;
}
