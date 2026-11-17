package preferencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import preferencias.document.RangoPrecio;

@Getter
@Setter
@Schema(name = "PreferenciaRequest", description = "Preferencias editables del cliente")
public class PreferenciaRequestDTO {

    private List<String> categorias;

    private List<String> ciudades;

    private RangoPrecio rangoPrecio;

    private List<String> artistas;

    private NotificacionesPreferenciasDTO notificaciones;
}
