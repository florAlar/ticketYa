package eventos.service;

import eventos.dto.EventoDetalleResponseDTO;
import eventos.dto.EventoResponseDTO;
import eventos.dto.PageResponseDTO;
import java.time.LocalDate;

public interface EventoService {

    PageResponseDTO<EventoResponseDTO> buscar(
            String tipo,
            String ciudad,
            String organizador,
            String estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String orden,
            int page,
            int size);

    EventoDetalleResponseDTO obtenerPorId(String id);
}
