package usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Usuario", description = "Usuario con su rol principal y todos sus roles")
public class UsuarioResponseDTO {

    @Schema(description = "UUID del usuario", example = "b0000000-0000-4000-8000-000000000001")
    private String id;

    @Schema(description = "Email único", example = "demo@ticketya.com")
    private String email;

    @Schema(description = "Nombre", example = "Usuario")
    private String nombre;

    @Schema(description = "Apellido", example = "Demo")
    private String apellido;

    @Schema(description = "Ciudad", example = "Buenos Aires")
    private String ciudad;

    @Schema(description = "Cuenta activa", example = "true")
    private boolean activo;

    @Schema(description = "Rol principal", example = "CLIENTE")
    private String rol;

    @Schema(description = "Roles asignados", example = "[\"CLIENTE\"]")
    private List<String> roles;
}
