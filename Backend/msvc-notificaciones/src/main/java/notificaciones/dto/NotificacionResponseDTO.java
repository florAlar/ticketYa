package notificaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "NotificacionResponseDTO", description = "Notificación registrada y estado de entrega")
public class NotificacionResponseDTO {

    @Schema(description = "UUID de la notificación")
    private String id;

    @Schema(description = "UUID del destinatario")
    private String idUsuario;

    @Schema(description = "UUID del evento de origen", nullable = true)
    private String idEventoOrigen;

    @Schema(description = "Tipo de notificación", example = "COMPRA_CONFIRMADA")
    private String tipo;

    @Schema(description = "Canal de la fila representativa del mensaje", example = "PUSH")
    private String canal;

    /** Canales por los que se entregó el mensaje (PUSH, EMAIL). */
    @Schema(description = "Canales por los que se entregó el mensaje (PUSH, EMAIL)",
            example = "[\"PUSH\", \"EMAIL\"]")
    private List<String> canales;

    @Schema(description = "Asunto del aviso")
    private String asunto;

    @Schema(description = "Contenido del aviso")
    private String contenido;

    @Schema(description = "Email de destino (canal EMAIL)", nullable = true,
            example = "demo@ticketya.com")
    private String destinatario;

    @Schema(description = "Estado de envío", allowableValues = {"PENDIENTE", "ENVIADA", "FALLIDA"})
    private String estado;

    @Schema(description = "Fecha de creación")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Fecha de envío", nullable = true)
    private LocalDateTime fechaEnvio;

    @Schema(description = "Leída por el usuario en la bandeja")
    private Boolean leida;

    @Schema(description = "Fecha de lectura", nullable = true)
    private LocalDateTime fechaLectura;
}
