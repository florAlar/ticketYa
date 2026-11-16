package notificaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Alta administrativa de una notificación (ADMIN). */
@Getter
@Setter
public class NotificacionRequestDTO {

    @NotBlank
    @Size(max = 36)
    private String idUsuario;

    @Size(max = 36)
    private String idEventoOrigen;

    @NotBlank
    @Size(max = 64)
    private String tipo;

    @NotBlank
    @Size(max = 32)
    private String canal;

    @NotBlank
    @Size(max = 255)
    private String asunto;

    @NotBlank
    private String contenido;

    /** Opcional: si no viene se usa la fecha actual. */
    private LocalDateTime fechaCreacion;
}
