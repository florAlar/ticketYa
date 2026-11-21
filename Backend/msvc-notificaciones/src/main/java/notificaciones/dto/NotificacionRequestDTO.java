package notificaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Alta administrativa de una notificación (ADMIN). */
@Getter
@Setter
@Schema(name = "NotificacionRequestDTO", description = "Alta administrativa de una notificación (ADMIN)")
public class NotificacionRequestDTO {

    @NotBlank
    @Size(max = 36)
    @Schema(description = "UUID del destinatario", example = "b0000000-0000-4000-8000-000000000001")
    private String idUsuario;

    @Size(max = 36)
    @Schema(description = "UUID del evento que origina el aviso (opcional)", nullable = true)
    private String idEventoOrigen;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Tipo de notificación", example = "AVISO_ADMINISTRATIVO")
    private String tipo;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "Canal de entrega", allowableValues = {"PUSH", "EMAIL"}, example = "PUSH")
    private String canal;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Asunto del aviso", example = "Mantenimiento programado")
    private String asunto;

    @NotBlank
    @Schema(description = "Contenido del aviso",
            example = "La plataforma estará en mantenimiento el domingo de 03:00 a 04:00.")
    private String contenido;

    /** Opcional: si no viene se usa la fecha actual. */
    @Schema(description = "Fecha de creación (opcional; por defecto, ahora)", nullable = true)
    private LocalDateTime fechaCreacion;
}
