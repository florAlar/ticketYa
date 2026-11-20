package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Respuesta de POST /eventos/{id}/fila/validacion (FIL-03). */
@Schema(name = "FilaValidacion", description = "Token temporal para reservar en un evento de alta demanda")
public record FilaValidacionResponseDTO(
        @Schema(description = "UUID del evento") String idEvento,
        @Schema(description = "Token a enviar como tokenFila en POST /reservas") String token,
        @Schema(description = "Instante de caducidad del token (ISO-8601, UTC)") Instant expiraEn,
        @Schema(description = "Turno asignado") long posicion,
        @Schema(description = "Posiciones que faltan: 0 porque ya está habilitado") long delante) {
}
