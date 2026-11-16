package notificaciones.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Página de notificaciones propias + contador para el badge de la campanita del front. */
@Getter
@Setter
public class NotificacionPageResponseDTO {

    private List<NotificacionResponseDTO> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private long noLeidas;
}
