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
import entradas.clients.VentasClient;
import entradas.dto.AsientoRefDTO;
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
        /** Reintegro por cancelación; no entra a cola admin (solo PENDIENTE). */
        public static final String DEV_EN_CURSO = "EN_CURSO";

        public static final String MOTIVO_REINTEGRO_CANCELACION =
                "Reintegro por cancelación del evento";

        /** Evento no admite ingreso ni exhibición de QR. */
        public static final String EVT_CANCELADO = "CANCELADO";

    private final EntradaRepository entradaRepository;
        private final DevolucionRepository devolucionRepository;
        private final EntradaMapper entradaMapper;
        private final EventosClient eventosClient;
        private final VentasClient ventasClient;
                private final long plazoDevolucionHoras;

                public EntradaServiceImpl(
                        EntradaRepository entradaRepository,
                        DevolucionRepository devolucionRepository,
                        EntradaMapper entradaMapper,
                        EventosClient eventosClient,
                        VentasClient ventasClient,
                        @Value("${ticketya.entradas.plazo-devolucion-horas:72}") long plazoDevolucionHoras) {
                    this.entradaRepository = entradaRepository;
                    this.devolucionRepository = devolucionRepository;
                    this.entradaMapper = entradaMapper;
                    this.eventosClient = eventosClient;
                    this.ventasClient = ventasClient;
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
            List<AsientoRefDTO> asientos = item.asientos();
            boolean numerado = asientos != null && !asientos.isEmpty();
            if (numerado && n != asientos.size()) {
                throw new IllegalArgumentException(
                        "cantidad (" + n + ") no coincide con los asientos (" + asientos.size()
                                + ") del sector " + item.idSector());
            }
            for (int i = 0; i < n; i++) {
                Entrada e = new Entrada();
                e.setIdEntrada(UUID.randomUUID().toString());
                e.setIdCompra(request.idCompra());
                e.setIdCliente(request.idCliente());
                e.setIdEvento(request.idEvento());
                e.setIdSector(item.idSector());
                if (numerado) {
                    // una entrada por asiento vendido
                    e.setFila(asientos.get(i).fila());
                    e.setNumero(asientos.get(i).numero());
                }
                e.setCodigoQr(buildQrCode());
                                e.setEstado(EST_VENDIDA);
                                e.setFechaEmision(now);
                                e.setFechaValidacion(null);
                                e.setPrecio(
                                        item.precioUnitario() != null ? item.precioUnitario() : BigDecimal.ZERO);
                                creadas.add(e);
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
    public EntradaResponseDTO obtener(String idCliente, String idEntrada, boolean admin) {
        Entrada e = requireEntrada(idCliente, idEntrada, admin);
                EntradaResponseDTO dto = entradaMapper.toEntrada(e);
                EventoDetalleFeignDTO evento = fetchEventoCached(e.getIdEvento(), new HashMap<>());
        enriquecer(dto, evento);
        applyQrPolicy(e, dto, evento);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public QrResponseDTO qrMia(String idCliente, String idEntrada) {
        Entrada e = requireEntrada(idCliente, idEntrada, false);
        EventoDetalleFeignDTO evento = fetchEventoCached(e.getIdEvento(), new HashMap<>());
        if (!puedeExponerQr(e, evento)) {
            String motivo = !EST_VENDIDA.equals(e.getEstado())
                    ? "estado=" + e.getEstado()
                    : "evento no habilita QR"
                            + (evento != null && evento.getEstado() != null
                                    ? " (" + evento.getEstado() + ")"
                                    : "");
            throw new BusinessConflictException("Entrada no tiene QR activo (" + motivo + ")");
        }
        return entradaMapper.toQr(e);
    }

    @Override
    @Transactional
    public DevolucionResponseDTO solicitarDevolucion(
            String idCliente, String idEntrada, DevolucionCreateRequestDTO body) {
        Entrada entrada = requireEntrada(idCliente, idEntrada, false);
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

        BigDecimal monto = EntradaMapper.precioDe(entrada);
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
                // QR viejo deja de coincidir con pantallazos previos (UK nuevo valor INV-*)
                invalidarCodigoQr(entrada);
                entradaRepository.save(entrada);
                d.setEstado(DEV_APROBADA);
                // reintegro mock de dinero: solo estado APROBADA (pasarela real = E2)
                devolucionRepository.save(d);
                // Reabrir cupo en ventas; si Feign falla → excepción → rollback de approve
                liberarCupoVentasFailHard(entrada);
                return entradaMapper.toDevolucion(d);
            }

        /**
         * 1 unidad del sector de la entrada vuelve a disponible en msvc-ventas.
         * Fail-hard: sin cupo liberado no se aprueba (consistencia BC).
         */
        private void liberarCupoVentasFailHard(Entrada entrada) {
            if (entrada == null
                    || entrada.getIdEvento() == null
                    || entrada.getIdEvento().isBlank()
                    || entrada.getIdSector() == null
                    || entrada.getIdSector().isBlank()) {
                throw new BusinessConflictException(
                        "No se puede liberar cupo: entrada sin idEvento/idSector");
            }
            try {
                // entrada con asiento (sector numerado): ventas libera ese asiento concreto
                VentasClient.LiberarCupoBody body = entrada.getFila() != null && entrada.getNumero() != null
                        ? new VentasClient.LiberarCupoBody(
                                entrada.getIdSector().trim(),
                                List.of(new AsientoRefDTO(entrada.getFila(), entrada.getNumero())))
                        : new VentasClient.LiberarCupoBody(entrada.getIdSector().trim(), 1);
                ventasClient.liberarCupo(entrada.getIdEvento().trim(), body);
            } catch (FeignException ex) {
                log.error(
                        "Feign ventas liberar cupo falló entrada={} evento={} sector={} status={}",
                        entrada.getIdEntrada(),
                        entrada.getIdEvento(),
                        entrada.getIdSector(),
                        ex.status(),
                        ex);
                throw new BusinessConflictException(
                        "No se pudo reabrir cupo en ventas al aprobar devolución (status="
                                + ex.status()
                                + "). Reintentá cuando ventas esté disponible.");
            } catch (RuntimeException ex) {
                log.error(
                        "Error liberando cupo ventas entrada={} evento={} sector={}",
                        entrada.getIdEntrada(),
                        entrada.getIdEvento(),
                        entrada.getIdSector(),
                        ex);
                throw new BusinessConflictException(
                        "No se pudo reabrir cupo en ventas al aprobar devolución: " + ex.getMessage());
            }
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
            LocalDateTime now = nowUtc();
            for (Entrada e : list) {
                e.setEstado(EST_A_REINTEGRAR);
                invalidarCodigoQr(e);
                // Trámite visible al cliente en GET /devoluciones (no es solicitud voluntaria)
                boolean yaTieneTramiteCancel =
                        devolucionRepository.findByEntradaIdEntrada(e.getIdEntrada()).stream()
                                .anyMatch(
                                        d ->
                                                d.getMotivo() != null
                                                        && d.getMotivo()
                                                                .startsWith(MOTIVO_REINTEGRO_CANCELACION));
                if (!yaTieneTramiteCancel) {
                    Devolucion d = new Devolucion();
                    d.setIdDevolucion(UUID.randomUUID().toString());
                    d.setEntrada(e);
                    d.setMotivo(MOTIVO_REINTEGRO_CANCELACION);
                                        d.setMonto(EntradaMapper.precioDe(e));
                                        d.setFechaSolicitud(now);
                    d.setEstado(DEV_EN_CURSO);
                    devolucionRepository.save(d);
                }
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

    private Entrada requireEntrada(String idCliente, String idEntrada, boolean admin) {
        Entrada e = entradaRepository
                .findById(idEntrada)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada no encontrada: " + idEntrada));
        if (admin) {
            return e;
        }
        if (idCliente == null || idCliente.isBlank() || !idCliente.equals(e.getIdCliente())) {
            throw new ResourceNotFoundException("Entrada no encontrada: " + idEntrada);
        }
        return e;
    }

    private static String buildQrCode() {
        return "QR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /** Reemplaza el código para que un pantallazo previo no coincida con el valor actual en DB. */
    private static void invalidarCodigoQr(Entrada e) {
        e.setCodigoQr(
                "INV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
    }

    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * QR solo si entrada VENDIDA y el evento no está CANCELADO.
     * Si Feign no trae evento, se confía en el estado de la entrada (best-effort).
     */
    static boolean puedeExponerQr(Entrada e, EventoDetalleFeignDTO evento) {
        if (e == null || !EST_VENDIDA.equals(e.getEstado())) {
            return false;
        }
        if (evento != null && evento.getEstado() != null) {
            String est = evento.getEstado().trim();
            if (!est.isEmpty() && EVT_CANCELADO.equalsIgnoreCase(est)) {
                return false;
            }
        }
        return true;
    }

    private void applyQrPolicy(Entrada e, EntradaResponseDTO dto, EventoDetalleFeignDTO evento) {
        if (!puedeExponerQr(e, evento)) {
            dto.setCodigoQR(null);
        }
    }

    /** Mapea + enriquece con cache por idEvento (una llamada Feign por evento distinto). */
    private List<EntradaResponseDTO> mapEnriquecidas(List<Entrada> entradas) {
        Map<String, EventoDetalleFeignDTO> cache = new HashMap<>();
        List<EntradaResponseDTO> out = new ArrayList<>(entradas.size());
        for (Entrada e : entradas) {
            EntradaResponseDTO dto = entradaMapper.toEntrada(e);
                        EventoDetalleFeignDTO evento = fetchEventoCached(e.getIdEvento(), cache);
            enriquecer(dto, evento);
            applyQrPolicy(e, dto, evento);
            out.add(dto);
        }
        return out;
    }

    private void enriquecer(EntradaResponseDTO dto, EventoDetalleFeignDTO evento) {
        if (evento == null) {
            return;
        }
        dto.setEventoNombre(evento.getNombre());
        dto.setEventoEstado(evento.getEstado());
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
