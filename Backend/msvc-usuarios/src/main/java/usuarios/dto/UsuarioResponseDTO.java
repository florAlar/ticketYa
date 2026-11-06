package usuarios.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioResponseDTO {

    private String id;
    private String email;
    private String nombre;
    private String apellido;
    private String ciudad;
    private boolean activo;
    private String rol;
    private List<String> roles;
}
