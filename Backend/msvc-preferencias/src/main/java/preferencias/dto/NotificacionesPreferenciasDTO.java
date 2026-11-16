package preferencias.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificacionesPreferenciasDTO {

    /** null = no modificar en un PUT parcial. */
    private Boolean push;

    /** null = no modificar en un PUT parcial. */
    private Boolean email;

    /** Lista vacía = todos. null = no modificar en un PUT parcial. */
    private List<String> tipos;
}
