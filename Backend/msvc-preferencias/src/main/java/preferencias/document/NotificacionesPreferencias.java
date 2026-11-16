package preferencias.document;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Preferencias de canal de notificación del cliente (E2-NOT-02).
 * Vive dentro del documento {@code preferencias_clientes} en MongoDB.
 */
@Getter
@Setter
public class NotificacionesPreferencias {

    private Boolean push = Boolean.TRUE;

    private Boolean email = Boolean.TRUE;

    /** Tipos habilitados. Lista vacía = todos. */
    private List<String> tipos = new ArrayList<>();
}
