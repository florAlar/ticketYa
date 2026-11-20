package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Respuesta pública de GET /eventos/{id}/fila/estado (sin identidades). */
@Schema(name = "FilaEstado", description = "Estado general de la fila, sin identidades")
public record FilaEstadoResponseDTO(
        @Schema(description = "UUID del evento") String idEvento,
        @Schema(description = "Clientes que aún no usaron su turno") long totalEnFila,
        @Schema(description = "Última posición habilitada") long posicionHabilitadaHasta,
        @Schema(description = "true si el evento es de alta demanda y está PUBLICADO") boolean abierta) {
}
