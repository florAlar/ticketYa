package eventos.service;

import eventos.dto.EventoDetalleResponseDTO;
import eventos.dto.EventoResponseDTO;
import eventos.dto.PageResponseDTO;
import eventos.entity.Evento;
import eventos.entity.PrecioSector;
import eventos.exception.ResourceNotFoundException;
import eventos.mapper.EventoMapper;
import eventos.repository.EventoRepository;
import eventos.repository.PrecioSectorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoServiceImpl implements EventoService {

    private static final String ESTADO_CATALOGO_DEFAULT = "PUBLICADO";

    private final EventoRepository eventoRepository;
    private final PrecioSectorRepository precioSectorRepository;
    private final EventoMapper eventoMapper;

    public EventoServiceImpl(
            EventoRepository eventoRepository,
            PrecioSectorRepository precioSectorRepository,
            EventoMapper eventoMapper) {
        this.eventoRepository = eventoRepository;
        this.precioSectorRepository = precioSectorRepository;
        this.eventoMapper = eventoMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<EventoResponseDTO> buscar(
            String tipo,
            String ciudad,
            String organizador,
            String estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String orden,
            int page,
            int size) {

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);

        String estadoFiltro = blankToNull(estado);
        if (estadoFiltro == null) {
            estadoFiltro = ESTADO_CATALOGO_DEFAULT;
        }

        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null;

        // orden precio se aplica en memoria sobre la página (seed chico); fecha/relevancia en DB
        Sort sort = resolveSort(orden);
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        Page<Evento> result = eventoRepository.buscar(
                blankToNull(tipo),
                blankToNull(ciudad),
                blankToNull(organizador),
                estadoFiltro,
                desde,
                hasta,
                pageable);

        List<Evento> eventos = result.getContent();
        Map<String, BigDecimal> minPrecios = minPrecios(eventos.stream().map(Evento::getId).toList());

        List<EventoResponseDTO> content = eventos.stream()
                .map(e -> eventoMapper.toCatalogo(e, minPrecios.get(e.getId())))
                .collect(Collectors.toList());

        if ("precio".equalsIgnoreCase(blankToNull(orden))) {
            content.sort((a, b) -> {
                BigDecimal pa = a.getPrecioDesde() != null ? a.getPrecioDesde() : BigDecimal.ZERO;
                BigDecimal pb = b.getPrecioDesde() != null ? b.getPrecioDesde() : BigDecimal.ZERO;
                return pa.compareTo(pb);
            });
        }

        return new PageResponseDTO<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public EventoDetalleResponseDTO obtenerPorId(String id) {
        Evento evento = eventoRepository
                .findByIdWithLugar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado: " + id));

        BigDecimal precioDesde = precioSectorRepository.minPrecioByEventoId(id);
        List<PrecioSector> precios = precioSectorRepository.findActivosByEventoId(id);
        return eventoMapper.toDetalle(evento, precioDesde, precios);
    }

    private Map<String, BigDecimal> minPrecios(List<String> ids) {
        Map<String, BigDecimal> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        for (Object[] row : precioSectorRepository.minPrecioByEventoIds(ids)) {
            map.put((String) row[0], (BigDecimal) row[1]);
        }
        return map;
    }

    private static Sort resolveSort(String orden) {
        if (orden == null || orden.isBlank() || "fecha".equalsIgnoreCase(orden)) {
            return Sort.by(Sort.Direction.ASC, "fechaInicio");
        }
        if ("relevancia".equalsIgnoreCase(orden)) {
            return Sort.by(Sort.Direction.DESC, "fechaPublicacion")
                    .and(Sort.by(Sort.Direction.ASC, "fechaInicio"));
        }
        // precio: orden DB neutro; se reordena en memoria
        return Sort.by(Sort.Direction.ASC, "fechaInicio");
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
