package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Respuesta de POST /eventos/{id}/fila/habilitacion (FIL-02). */
@Schema(name = "FilaHabilitacionResponse", description = "Tanda resultante y modo de apertura")
public record FilaHabilitacionResponseDTO(
        @Schema(description = "UUID del evento") String idEvento,
        @Schema(description = "Última posición habilitada") long posicionHabilitadaHasta,
        @Schema(description = "AUTO | MANUAL", allowableValues = {"AUTO", "MANUAL"}) String modo) {
}
