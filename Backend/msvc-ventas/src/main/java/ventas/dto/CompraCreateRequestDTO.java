package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CompraCreateRequest", description = "Compra a crear desde una reserva válida")
public record CompraCreateRequestDTO(
        @Schema(description = "UUID de la reserva a convertir en compra",
                example = "11111111-1111-4111-8111-111111111111")
        @NotBlank String idReserva) {
}
