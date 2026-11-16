package entradas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "ValidacionRegistro", description = "Intento de validación de ingreso registrado (aceptado o rechazado)")
public class ValidacionRegistroDTO {

    @Schema(description = "UUID del registro", example = "77777777-7777-4777-8777-777777777777")
    private String idValidacion;

    @Schema(description = "UUID de la entrada", example = "66666666-6666-4666-8666-666666666666")
    private String idEntrada;

    @Schema(description = "UUID del personal de acceso que escaneó", example = "b0000000-0000-4000-8000-000000000004")
    private String idPersonalAcceso;

    @Schema(description = "VALIDA | YA_USADA | OTRO_EVENTO | NO_VALIDA | EVENTO_CANCELADO", example = "VALIDA")
    private String resultado;

    @Schema(description = "Momento del intento (UTC)")
    private LocalDateTime fechaValidacion;

    @Schema(description = "Dispositivo/puerta informado", example = "puerta-3")
    private String dispositivo;

    @Schema(description = "Fila del asiento (sector numerado)", example = "C")
    private String fila;

    @Schema(description = "Número de asiento (sector numerado)", example = "14")
    private Integer asiento;
}
