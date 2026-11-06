package usuarios.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioUpdateDTO {

    @Size(min = 2, max = 100)
    private String nombre;

    @Size(max = 100)
    private String apellido;

    @Size(max = 120)
    private String ciudad;
}
