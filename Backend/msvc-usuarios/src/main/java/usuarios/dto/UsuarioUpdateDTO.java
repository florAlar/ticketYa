package usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "UsuarioUpdate", description = "Campos editables del perfil propio")
public class UsuarioUpdateDTO {

    @Schema(description = "Nombre", example = "Ana")
    @Size(min = 2, max = 100)
    private String nombre;

    @Schema(description = "Apellido", example = "Pérez")
    @Size(max = 100)
    private String apellido;

    @Schema(description = "Ciudad", example = "Rosario")
    @Size(max = 120)
    private String ciudad;
}
