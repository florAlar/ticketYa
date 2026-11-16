package notificaciones.mapper;

import notificaciones.dto.NotificacionResponseDTO;
import notificaciones.entity.Notificacion;

/** Mapeo entidad → DTO de la API de notificaciones. */
public final class NotificacionMapper {

    private NotificacionMapper() {}

    public static NotificacionResponseDTO toResponse(Notificacion n) {
        if (n == null) {
            return null;
        }
        NotificacionResponseDTO dto = new NotificacionResponseDTO();
        dto.setId(n.getIdNotificacion());
        dto.setIdUsuario(n.getIdUsuario());
        dto.setIdEventoOrigen(n.getIdEventoOrigen());
        dto.setTipo(n.getTipo());
        dto.setCanal(n.getCanal());
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
