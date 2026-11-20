package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Respuesta de GET /eventos/{id}/fila/posicion. */
@Schema(name = "FilaPosicionDetalle", description = "Posición propia y estado en la fila")
public record FilaPosicionResponseDTO(
        @Schema(description = "UUID del evento") String idEvento,
        @Schema(description = "Número de turno asignado (inmutable)") long posicion,
        @Schema(description = "Última posición habilitada") long posicionHabilitadaHasta,
        @Schema(description = "Posiciones que faltan habilitar") long delante,
        @Schema(description = "EN_ESPERA | HABILITADO | CON_TOKEN | TURNO_USADO",
                allowableValues = {"EN_ESPERA", "HABILITADO", "CON_TOKEN", "TURNO_USADO"}) String estado) {
}
