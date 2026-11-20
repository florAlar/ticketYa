package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Body de POST /eventos/{id}/fila/habilitacion (FIL-02). */
@Schema(name = "FilaHabilitacionRequest", description = "Última posición a habilitar")
public record FilaHabilitacionRequestDTO(
        @NotNull @Min(1)
        @Schema(description = "Última posición habilitada; nunca menor a la actual", example = "100")
        Integer hastaPosicion) {
}
