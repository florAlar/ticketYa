package entradas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ValidacionRequest", description = "Código leído del QR en la puerta")
public record ValidacionRequestDTO(
        @Schema(description = "Código QR de la entrada", example = "QR-8F3A1C5D2B7E9A40")
        @NotBlank(message = "codigoQR requerido")
        @Size(max = 128, message = "codigoQR demasiado largo")
        String codigoQR,
        @Schema(description = "Identificador libre del dispositivo/puerta que escanea (opcional)",
                example = "puerta-3")
        @Size(max = 128, message = "dispositivo demasiado largo")
        String dispositivo) {}
