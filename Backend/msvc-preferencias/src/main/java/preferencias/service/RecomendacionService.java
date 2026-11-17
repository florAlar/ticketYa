package preferencias.service;

import java.util.List;
import preferencias.dto.EventoRecomendadoDTO;

public interface RecomendacionService {

    /** Eventos similares a uno dado (co-compra + contenido). Público. */
    List<EventoRecomendadoDTO> similares(String idEvento, int limit);

    /** Recomendaciones personalizadas según preferencias e historial del cliente. */
    List<EventoRecomendadoDTO> paraCliente(String idCliente, int limit);
}
