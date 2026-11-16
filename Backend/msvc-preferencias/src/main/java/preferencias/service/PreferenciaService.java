package preferencias.service;

import java.util.List;
import preferencias.dto.MatchingClientesRequestDTO;
import preferencias.dto.NotificacionesPreferenciasDTO;
import preferencias.dto.PreferenciaRequestDTO;
import preferencias.dto.PreferenciaResponseDTO;

public interface PreferenciaService {

    PreferenciaResponseDTO obtener(String idCliente);

    PreferenciaResponseDTO actualizar(String idCliente, PreferenciaRequestDTO request);

    NotificacionesPreferenciasDTO obtenerNotificaciones(String idCliente);

    NotificacionesPreferenciasDTO actualizarNotificaciones(
            String idCliente, NotificacionesPreferenciasDTO request);

    /** Clientes que tienen alguno de los artistas y la ciudad indicados entre sus favoritos. */
    List<String> clientesInteresados(MatchingClientesRequestDTO request);
}
