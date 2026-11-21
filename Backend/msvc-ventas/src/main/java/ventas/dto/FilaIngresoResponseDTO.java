package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Respuesta de POST /eventos/{id}/fila/ingreso y GET .../posicion. */
@Schema(name = "FilaPosicion", description = "Posición del cliente en la fila virtual")
public record FilaIngresoResponseDTO(
        @Schema(description = "UUID del evento") String idEvento,
        @Schema(description = "Número de turno asignado (inmutable)") long posicion,
        @Schema(description = "Última posición habilitada por la tanda") long posicionHabilitadaHasta,
        @Schema(description = "Posiciones que faltan habilitar para este turno") long delante,
        @Schema(description = "EN_ESPERA | HABILITADO | CON_TOKEN | TURNO_USADO",
                allowableValues = {"EN_ESPERA", "HABILITADO", "CON_TOKEN", "TURNO_USADO"}) String estado,
        @Schema(description = "true si el cliente ya estaba en la fila y conserva su posición")
        boolean reingreso) {
}
