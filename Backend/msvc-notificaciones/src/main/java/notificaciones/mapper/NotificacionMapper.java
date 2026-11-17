package notificaciones.mapper;

import java.util.List;
import notificaciones.dto.NotificacionResponseDTO;
import notificaciones.entity.Notificacion;

/** Mapeo entidad → DTO de la API de notificaciones. */
public final class NotificacionMapper {

    private NotificacionMapper() {}

    public static NotificacionResponseDTO toResponse(Notificacion n) {
        return toResponse(n, null);
    }

    /**
     * @param canales canales del mensaje completo (PUSH/EMAIL); si es null se usa el canal de la
     *                fila. La bandeja muestra una sola fila por mensaje, pero informa por qué
     *                canales se entregó.
     */
    public static NotificacionResponseDTO toResponse(Notificacion n, List<String> canales) {
        if (n == null) {
            return null;
        }
        NotificacionResponseDTO dto = new NotificacionResponseDTO();
        dto.setId(n.getIdNotificacion());
        dto.setIdUsuario(n.getIdUsuario());
        dto.setIdEventoOrigen(n.getIdEventoOrigen());
        dto.setTipo(n.getTipo());
        dto.setCanal(n.getCanal());
        dto.setCanales(canales == null || canales.isEmpty()
                ? (n.getCanal() == null ? List.of() : List.of(n.getCanal()))
                : canales);
        dto.setAsunto(n.getAsunto());
        dto.setContenido(n.getContenido());
        dto.setDestinatario(n.getDestinatario());
        dto.setEstado(n.getEstado());
        dto.setFechaCreacion(n.getFechaCreacion());
        dto.setFechaEnvio(n.getFechaEnvio());
        dto.setLeida(Boolean.TRUE.equals(n.getLeida()));
        dto.setFechaLectura(n.getFechaLectura());
        return dto;
    }
}
