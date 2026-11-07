package entradas.service;

import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import entradas.clients.EventosClient;
import entradas.clients.EventosClient.EventoDetalleFeignDTO;
import entradas.clients.EventosClient.LugarFeignDTO;
import entradas.clients.EventosClient.SectorFeignDTO;
import entradas.dto.DevolucionCreateRequestDTO;
import entradas.dto.DevolucionResponseDTO;
import entradas.dto.EmitirEntradasRequestDTO;
import entradas.dto.EntradaResponseDTO;
import entradas.dto.QrResponseDTO;
import entradas.entity.Devolucion;
import entradas.entity.Entrada;
import entradas.exception.BusinessConflictException;
import entradas.exception.ResourceNotFoundException;
import entradas.mapper.EntradaMapper;
import entradas.repository.DevolucionRepository;
import entradas.repository.EntradaRepository;

@Service
public class EntradaServiceImpl implements EntradaService {

    private static final Logger log = LoggerFactory.getLogger(EntradaServiceImpl.class);

    public static final String EST_VENDIDA = "VENDIDA";
    public static final String EST_USADA = "USADA";
    public static final String EST_DEVUELTA = "DEVUELTA";
    public static final String EST_A_REINTEGRAR = "A_REINTEGRAR";

    public static final String DEV_PENDIENTE = "PENDIENTE";
    public static final String DEV_APROBADA = "APROBADA";
    public static final String DEV_RECHAZADA = "RECHAZADA";

    private final EntradaRepository entradaRepository;
    private final DevolucionRepository devolucionRepository;
    private final EntradaMapper entradaMapper;
    private final EventosClient eventosClient;
    private final long plazoDevolucionHoras;

    /** precio unitario por idEntrada (solo memoria de emisión reciente; seeds = null). */
    private final Map<String, BigDecimal> preciosMem = new HashMap<>();

    public EntradaServiceImpl(
            EntradaRepository entradaRepository,
            DevolucionRepository devolucionRepository,
            EntradaMapper entradaMapper,
            EventosClient eventosClient,
            @Value("${ticketya.entradas.plazo-devolucion-horas:72}") long plazoDevolucionHoras) {
        this.entradaRepository = entradaRepository;
        this.devolucionRepository = devolucionRepository;
        this.entradaMapper = entradaMapper;
        this.eventosClient = eventosClient;
        this.plazoDevolucionHoras = plazoDevolucionHoras;
    }

    @Override
    @Transactional
    public List<EntradaResponseDTO> emitir(EmitirEntradasRequestDTO request) {
        // idempotente: si ya hay entradas para la compra, devolverlas
        List<Entrada> existentes = entradaRepository.findByIdCompra(request.idCompra());
        if (!existentes.isEmpty()) {
            return mapEnriquecidas(existentes);
        }

        LocalDateTime now = nowUtc();
        List<Entrada> creadas = new ArrayList<>();
        for (EmitirEntradasRequestDTO.Item item : request.items()) {
            int n = item.cantidad() == null ? 0 : item.cantidad();
            for (int i = 0; i < n; i++) {
                Entrada e = new Entrada();
                e.setIdEntrada(UUID.randomUUID().toString());
                e.setIdCompra(request.idCompra());
                e.setIdCliente(request.idCliente());
                e.setIdEvento(request.idEvento());
                e.setIdSector(item.idSector());
                e.setCodigoQr(buildQrCode());
                e.setEstado(EST_VENDIDA);
                e.setFechaEmision(now);
                e.setFechaValidacion(null);
                creadas.add(e);
                if (item.precioUnitario() != null) {
                    preciosMem.put(e.getIdEntrada(), item.precioUnitario());
                }
            }
        }
        if (creadas.isEmpty()) {
            throw new IllegalArgumentException("items vacíos o cantidad 0");
        }
        entradaRepository.saveAll(creadas);
        return mapEnriquecidas(creadas);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntradaResponseDTO> listarMias(String idCliente) {
        return mapEnriquecidas(entradaRepository.findByIdClienteOrderByFechaEmisionDesc(idCliente));
    }

    @Override
    @Transactional(readOnly = true)
    public EntradaResponseDTO obtenerMia(String idCliente, String idEntrada) {
        Entrada e = requireMia(idCliente, idEntrada);
        EntradaResponseDTO dto = entradaMapper.toEntrada(e, preciosMem.get(idEntrada));
        enriquecer(dto, fetchEventoCached(e.getIdEvento(), new HashMap<>()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public QrResponseDTO qrMia(String idCliente, String idEntrada) {
        Entrada e = requireMia(idCliente, idEntrada);
        if (EST_DEVUELTA.equals(e.getEstado()) || EST_A_REINTEGRAR.equals(e.getEstado())) {
            throw new BusinessConflictException("Entrada no tiene QR activo (estado=" + e.getEstado() + ")");
        }
        return entradaMapper.toQr(e);
    }

    @Override
    @Transactional
    public DevolucionResponseDTO solicitarDevolucion(
            String idCliente, String idEntrada, DevolucionCreateRequestDTO body) {
        Entrada entrada = requireMia(idCliente, idEntrada);
        if (!EST_VENDIDA.equals(entrada.getEstado())) {
            throw new BusinessConflictException("Solo entradas VENDIDA admiten devolución");
        }
        if (entrada.getFechaValidacion() != null || EST_USADA.equals(entrada.getEstado())) {
            throw new BusinessConflictException("Entrada ya usada; no se puede devolver");
        }
        LocalDateTime limite = entrada.getFechaEmision().plusHours(plazoDevolucionHoras);
        if (nowUtc().isAfter(limite)) {
            throw new BusinessConflictException(
                    "Plazo de devolución vencido (" + plazoDevolucionHoras + " h desde emisión)");
        }
        boolean hayPendiente = devolucionRepository.findByEntradaIdEntrada(idEntrada).stream()
                .anyMatch(d -> DEV_PENDIENTE.equals(d.getEstado()));
        if (hayPendiente) {
            throw new BusinessConflictException("Ya hay una devolución PENDIENTE para esta entrada");
        }

        BigDecimal monto = preciosMem.getOrDefault(idEntrada, BigDecimal.ZERO);
        Devolucion d = new Devolucion();
        d.setIdDevolucion(UUID.randomUUID().toString());
        d.setEntrada(entrada);
        d.setMotivo(body != null && body.motivo() != null ? body.motivo().trim() : null);
        d.setMonto(monto);
        d.setFechaSolicitud(nowUtc());
        d.setEstado(DEV_PENDIENTE);
        devolucionRepository.save(d);
        return entradaMapper.toDevolucion(d);
    }

    @Override
    @Transactional(readOnly = true)
    public DevolucionResponseDTO obtenerDevolucion(String idCliente, String idDevolucion, boolean admin) {
        Devolucion d = devolucionRepository
                .findById(idDevolucion)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada: " + idDevolucion));
        if (!admin && !idCliente.equals(d.getEntrada().getIdCliente())) {
            throw new ResourceNotFoundException("Devolución no encontrada: " + idDevolucion);
        }
        return entradaMapper.toDevolucion(d);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DevolucionResponseDTO> listarDevolucionesPendientes() {
        return devolucionRepository.findByEstadoOrderByFechaSolicitudAsc(DEV_PENDIENTE).stream()
                .map(entradaMapper::toDevolucion)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DevolucionResponseDTO> listarDevolucionesCliente(String idCliente) {
        return devolucionRepository.findByEntradaIdClienteOrderByFechaSolicitudDesc(idCliente).stream()
                .map(entradaMapper::toDevolucion)
                .toList();
    }

    @Override
    @Transactional
    public DevolucionResponseDTO aprobarDevolucion(String idDevolucion) {
        Devolucion d = requireDevPendiente(idDevolucion);
        Entrada entrada = d.getEntrada();
        entrada.setEstado(EST_DEVUELTA);
        entradaRepository.save(entrada);
        d.setEstado(DEV_APROBADA);
        // reintegro mock: solo estado; cupo ventas no se toca en E1 (contrato)
        devolucionRepository.save(d);
        return entradaMapper.toDevolucion(d);
    }

    @Override
    @Transactional
    public DevolucionResponseDTO rechazarDevolucion(String idDevolucion) {
        Devolucion d = requireDevPendiente(idDevolucion);
        d.setEstado(DEV_RECHAZADA);
        devolucionRepository.save(d);
        return entradaMapper.toDevolucion(d);
    }

    @Override
    @Transactional
    public int reintegroPorCancelacionEvento(String idEvento) {
        List<Entrada> list = entradaRepository.findByIdEventoAndEstado(idEvento, EST_VENDIDA);
        for (Entrada e : list) {
            e.setEstado(EST_A_REINTEGRAR);
        }
        entradaRepository.saveAll(list);
        return list.size();
    }

    private Devolucion requireDevPendiente(String id) {
        Devolucion d = devolucionRepository
                .findByIdWithEntrada(id)
                .or(() -> devolucionRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada: " + id));
        if (!DEV_PENDIENTE.equals(d.getEstado())) {
            throw new BusinessConflictException("Devolución no está PENDIENTE (estado=" + d.getEstado() + ")");
        }
        return d;
    }

    private Entrada requireMia(String idCliente, String idEntrada) {
        Entrada e = entradaRepository
                .findById(idEntrada)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada no encontrada: " + idEntrada));
        if (!idCliente.equals(e.getIdCliente())) {
            throw new ResourceNotFoundException("Entrada no encontrada: " + idEntrada);
        }
        return e;
    }

    private static String buildQrCode() {
        return "QR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /** Mapea + enriquece con cache por idEvento (una llamada Feign por evento distinto). */
    private List<EntradaResponseDTO> mapEnriquecidas(List<Entrada> entradas) {
        Map<String, EventoDetalleFeignDTO> cache = new HashMap<>();
        List<EntradaResponseDTO> out = new ArrayList<>(entradas.size());
        for (Entrada e : entradas) {
            EntradaResponseDTO dto = entradaMapper.toEntrada(e, preciosMem.get(e.getIdEntrada()));
            enriquecer(dto, fetchEventoCached(e.getIdEvento(), cache));
            out.add(dto);
        }
        return out;
    }

    private void enriquecer(EntradaResponseDTO dto, EventoDetalleFeignDTO evento) {
        if (evento == null) {
            return;
        }
        dto.setEventoNombre(evento.getNombre());
        LugarFeignDTO lugar = evento.getLugar();
        if (lugar != null) {
            dto.setLugarNombre(lugar.getNombre());
            dto.setLugarCiudad(lugar.getCiudad());
        }
        String idSector = dto.getIdSector();
        if (idSector != null && evento.getSectores() != null) {
            for (SectorFeignDTO s : evento.getSectores()) {
                if (s != null && idSector.equals(s.getId())) {
                    dto.setSectorNombre(s.getNombre());
                    break;
                }
            }
        }
    }

    /**
     * Best-effort: si eventos cae o el id no existe, el DTO queda sin nombre/lugar (null).
     * Cache local al request evita N Feign en listados del mismo evento.
     */
    private EventoDetalleFeignDTO fetchEventoCached(String idEvento, Map<String, EventoDetalleFeignDTO> cache) {
        if (idEvento == null || idEvento.isBlank()) {
            return null;
        }
        if (cache.containsKey(idEvento)) {
            return cache.get(idEvento);
        }
        EventoDetalleFeignDTO ev = null;
        try {
            ev = eventosClient.getEvento(idEvento);
        } catch (FeignException.NotFound e) {
            log.debug("Evento no encontrado al enriquecer entrada: {}", idEvento);
        } catch (FeignException e) {
            log.warn("No se pudo enriquecer entrada con evento {}: {}", idEvento, e.status());
        } catch (Exception e) {
            log.warn("No se pudo enriquecer entrada con evento {}: {}", idEvento, e.toString());
        }
        cache.put(idEvento, ev);
        return ev;
    }
}
