package eventos.service;

import eventos.dto.AsientoResponseDTO;
import eventos.dto.EventoDetalleResponseDTO;
import eventos.dto.EventoFechaRequestDTO;
import eventos.dto.EventoRequestDTO;
import eventos.dto.EventoResponseDTO;
import eventos.dto.LugarRequestDTO;
import eventos.dto.LugarResponseDTO;
import eventos.dto.PageResponseDTO;
import eventos.dto.PrecioSectorRequestDTO;
import eventos.dto.SectorEventoDTO;
import eventos.dto.SectorRequestDTO;
import java.time.LocalDate;
import java.util.List;

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

    List<EventoResponseDTO> listarDeOrganizador(String idOrganizador);

    EventoDetalleResponseDTO crear(EventoRequestDTO request, String idOrganizador);

    EventoDetalleResponseDTO actualizar(String id, EventoRequestDTO request, String userId, String role);

    void eliminarBorrador(String id, String userId, String role);

    EventoDetalleResponseDTO publicar(String id, String userId, String role);

    EventoDetalleResponseDTO patchEstado(String id, String estado, String userId, String role);

    EventoDetalleResponseDTO patchFecha(String id, EventoFechaRequestDTO body, String userId, String role);

    List<SectorEventoDTO> listarSectoresEvento(String idEvento);

    SectorEventoDTO upsertPrecioSector(
            String idEvento, String sectorId, PrecioSectorRequestDTO body, String userId, String role);

    List<LugarResponseDTO> listarLugares(String ciudad);

    LugarResponseDTO obtenerLugar(String id);

    LugarResponseDTO crearLugar(LugarRequestDTO body);

    LugarResponseDTO actualizarLugar(String id, LugarRequestDTO body);

    SectorEventoDTO crearSector(String idLugar, SectorRequestDTO body);

    SectorEventoDTO actualizarSector(String idSector, SectorRequestDTO body);

    List<AsientoResponseDTO> listarAsientos(String idLugar, String idSector);
}
