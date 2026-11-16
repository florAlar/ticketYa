package notificaciones.dto;

import java.time.LocalDateTime;
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
    private String asunto;
    private String contenido;
    private String destinatario;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaEnvio;
    private Boolean leida;
    private LocalDateTime fechaLectura;
}
