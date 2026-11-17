package preferencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import preferencias.document.RangoPrecio;

@Getter
@Setter
@Schema(name = "Preferencia", description = "Preferencias del cliente")
public class PreferenciaResponseDTO {

    private String idCliente;

    private List<String> categorias;

    private List<String> ciudades;

    private RangoPrecio rangoPrecio;

    private List<String> artistas;

    private NotificacionesPreferenciasDTO notificaciones;

    private Instant fechaActualizacion;
}
