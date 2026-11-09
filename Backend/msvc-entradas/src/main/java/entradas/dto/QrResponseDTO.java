package entradas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** Código QR de una entrada propia. */
@Getter
@Setter
@Schema(name = "Qr", description = "Código QR de una entrada")
public class QrResponseDTO {

    @Schema(description = "UUID de la entrada", example = "66666666-6666-4666-8666-666666666666")
    private String id;

    @Schema(description = "Código QR legible", example = "TKT-8f3a1c-0001")
    private String codigoQR;
}
