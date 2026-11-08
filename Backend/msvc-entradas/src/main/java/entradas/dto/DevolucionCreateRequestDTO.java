package entradas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Solicitud de devolución de una entrada propia. El motivo es opcional. */
@Schema(name = "DevolucionCreateRequest", description = "Solicitud de devolución de una entrada")
public record DevolucionCreateRequestDTO(
        @Schema(description = "Motivo de la devolución", example = "No puedo asistir al evento")
        String motivo) {
}
