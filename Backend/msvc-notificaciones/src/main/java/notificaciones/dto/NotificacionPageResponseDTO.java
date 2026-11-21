package notificaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Página de notificaciones propias + contador para el badge de la campanita del front. */
@Getter
@Setter
@Schema(name = "NotificacionPageResponseDTO",
        description = "Página de notificaciones propias + contador de no leídas")
public class NotificacionPageResponseDTO {

    @Schema(description = "Notificaciones de la página")
    private List<NotificacionResponseDTO> content;

    @Schema(description = "Número de página (0-based)")
    private int page;

    @Schema(description = "Tamaño de página")
    private int size;

    @Schema(description = "Total de elementos")
    private long totalElements;

    @Schema(description = "Total de páginas")
    private int totalPages;

    @Schema(description = "Mensajes visibles sin leer (badge de la campanita)")
    private long noLeidas;
}
