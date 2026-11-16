package entradas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "ValidacionResponse", description = "Resultado de validar una entrada en la puerta")
public class ValidacionResponseDTO {

    @Schema(description = "true solo si el ingreso fue aceptado (resultado VALIDA)", example = "true")
    private boolean ok;

    @Schema(description = "VALIDA | YA_USADA | OTRO_EVENTO | NO_VALIDA | EVENTO_CANCELADO", example = "VALIDA")
    private String resultado;

    @Schema(description = "Mensaje legible para el personal de acceso", example = "Ingreso registrado")
    private String mensaje;

    @Schema(description = "UUID del registro de validación", example = "77777777-7777-4777-8777-777777777777")
    private String idValidacion;

    @Schema(description = "Momento de la validación (UTC)")
    private LocalDateTime fechaValidacion;

    @Schema(description = "Entrada validada (con evento, sector y asiento)")
    private EntradaResponseDTO entrada;
}
