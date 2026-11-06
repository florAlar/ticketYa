package eventos.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LugarDetalleDTO {

    private String id;
    private String nombre;
    private String direccion;
    private String ciudad;
    private List<SectorEventoDTO> sectores = new ArrayList<>();
}
