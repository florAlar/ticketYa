package notificaciones.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificacionResponseDTO {

    private String id;
    private String idUsuario;
    private String idEventoOrigen;
    private String tipo;
    private String canal;
    /** Canales por los que se entregó el mensaje (PUSH, EMAIL). */
    private List<String> canales;
    private String asunto;
    private String contenido;
    private String destinatario;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaEnvio;
    private Boolean leida;
    private LocalDateTime fechaLectura;
}
