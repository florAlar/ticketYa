package entradas.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final long plazoDevolucionHoras;

    /** precio unitario por idEntrada (solo memoria de emisión reciente; seeds = null). */
    private final Map<String, BigDecimal> preciosMem = new HashMap<>();

    public EntradaServiceImpl(
            EntradaRepository entradaRepository,
            DevolucionRepository devolucionRepository,
            EntradaMapper entradaMapper,
            @Value("${ticketya.entradas.plazo-devolucion-horas:72}") long plazoDevolucionHoras) {
        this.entradaRepository = entradaRepository;
        this.devolucionRepository = devolucionRepository;
        this.entradaMapper = entradaMapper;
        this.plazoDevolucionHoras = plazoDevolucionHoras;
    }

    @Override
    @Transactional
    public List<EntradaResponseDTO> emitir(EmitirEntradasRequestDTO request) {
        // idempotente: si ya hay entradas para la compra, devolverlas
        List<Entrada> existentes = entradaRepository.findByIdCompra(request.idCompra());
        if (!existentes.isEmpty()) {
            return existentes.stream().map(e -> entradaMapper.toEntrada(e, preciosMem.get(e.getIdEntrada()))).toList();
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
        return creadas.stream()
                .map(e -> entradaMapper.toEntrada(e, preciosMem.get(e.getIdEntrada())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntradaResponseDTO> listarMias(String idCliente) {
        return entradaRepository.findByIdClienteOrderByFechaEmisionDesc(idCliente).stream()
                .map(e -> entradaMapper.toEntrada(e, preciosMem.get(e.getIdEntrada())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EntradaResponseDTO obtenerMia(String idCliente, String idEntrada) {
        return entradaMapper.toEntrada(requireMia(idCliente, idEntrada), preciosMem.get(idEntrada));
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
}
