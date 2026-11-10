package entradas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Asiento virtual de un sector numerado: se identifica por fila y número. */
@Schema(name = "AsientoRef", description = "Asiento de un sector numerado (fila + número)")
public record AsientoRefDTO(
        @Schema(description = "Fila (A..Z, AA..)", example = "C") @NotBlank String fila,
        @Schema(description = "Número dentro de la fila (desde 1)", example = "14") @NotNull @Min(1) Integer numero) {
}
