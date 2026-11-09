package usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(name = "RolesUpdate", description = "Conjunto de roles a asignar (reemplaza los anteriores)")
public record RolesUpdateDTO(
        @Schema(description = "Nombres de rol: CLIENTE, ORGANIZADOR, ADMIN, PERSONAL_ACCESO (ACCESO se normaliza)",
                example = "[\"CLIENTE\",\"ORGANIZADOR\"]")
        @NotEmpty List<String> roles) {
}
