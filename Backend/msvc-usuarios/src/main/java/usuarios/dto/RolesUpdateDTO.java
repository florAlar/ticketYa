package usuarios.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RolesUpdateDTO(@NotEmpty List<String> roles) {
}
