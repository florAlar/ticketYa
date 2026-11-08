package ventas.service;

import feign.FeignException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ventas.clients.EventosClient;
import ventas.clients.EventosClient.EventoDetalleFeignDTO;
import ventas.clients.EventosClient.SectorFeignDTO;
import ventas.dto.AbrirCuposRequestDTO;
import ventas.dto.LiberarCupoRequestDTO;
import ventas.dto.CompraCreateRequestDTO;
import ventas.dto.CompraResponseDTO;
import ventas.dto.DisponibilidadBatchResponseDTO;
import ventas.dto.DisponibilidadResponseDTO;
import ventas.dto.PagoCreateRequestDTO;
import ventas.dto.PagoResponseDTO;
import ventas.dto.ReservaCreateRequestDTO;
import ventas.dto.ReservaResponseDTO;
import ventas.entity.Compra;
import ventas.entity.CupoSector;
import ventas.entity.DetalleReserva;
import ventas.entity.Pago;
import ventas.entity.Reserva;
import ventas.exception.BusinessConflictException;
import ventas.exception.ResourceNotFoundException;
import ventas.mapper.VentaMapper;
import ventas.redis.StockCache;
import ventas.repository.CompraRepository;
import ventas.repository.CupoSectorRepository;
import ventas.repository.DetalleReservaRepository;
import ventas.repository.PagoRepository;
import ventas.repository.ReservaRepository;

@Service
public class VentaServiceImpl implements VentaService {

    private static final Logger log = LoggerFactory.getLogger(VentaServiceImpl.class);

    public static final String EST_RES_PENDIENTE = "PENDIENTE";
    public static final String EST_RES_CONFIRMADA = "CONFIRMADA";
    public static final String EST_RES_CANCELADA = "CANCELADA";
    public static final String EST_RES_EXPIRADA = "EXPIRADA";
    public static final String EST_RES_COMPRADA = "COMPRADA";

    public static final String EST_COMPRA_PENDIENTE = "PENDIENTE_PAGO";
    public static final String EST_COMPRA_APROBADA = "APROBADA";
    public static final String EST_COMPRA_RECHAZADA = "RECHAZADA";

    public static final String EST_PAGO_APROBADO = "APROBADO";
    public static final String EST_PAGO_RECHAZADO = "RECHAZADO";

    private static final List<String> RESERVAS_ACTIVAS = List.of(EST_RES_PENDIENTE, EST_RES_CONFIRMADA);
    private static final List<String> COMPRAS_QUE_CUENTAN =
            List.of(EST_COMPRA_APROBADA, EST_COMPRA_PENDIENTE);

    private final CupoSectorRepository cupoSectorRepository;
    private final ReservaRepository reservaRepository;
    private final DetalleReservaRepository detalleReservaRepository;
    private final CompraRepository compraRepository;
    private final PagoRepository pagoRepository;
    private final EventosClient eventosClient;
    private final StockCache stockCache;
    private final VentaMapper ventaMapper;
    private final EmisionEntradasService emisionEntradasService;

    private final Duration reservaTtl;
    private final int maxEntradasPorClienteEvento;
    private final Duration idempotencyTtl;

    public VentaServiceImpl(
            CupoSectorRepository cupoSectorRepository,
            ReservaRepository reservaRepository,
            DetalleReservaRepository detalleReservaRepository,
            CompraRepository compraRepository,
            PagoRepository pagoRepository,
            EventosClient eventosClient,
            StockCache stockCache,
            VentaMapper ventaMapper,
            EmisionEntradasService emisionEntradasService,
            @Value("${ticketya.ventas.reserva-ttl-minutes:10}") long reservaTtlMinutes,
            @Value("${ticketya.ventas.max-entradas-cliente-evento:6}") int maxEntradasPorClienteEvento,
            @Value("${ticketya.ventas.idempotency-ttl-hours:24}") long idempotencyTtlHours) {
        this.cupoSectorRepository = cupoSectorRepository;
        this.reservaRepository = reservaRepository;
        this.detalleReservaRepository = detalleReservaRepository;
        this.compraRepository = compraRepository;
        this.pagoRepository = pagoRepository;
        this.eventosClient = eventosClient;
        this.stockCache = stockCache;
        this.ventaMapper = ventaMapper;
        this.emisionEntradasService = emisionEntradasService;
        this.reservaTtl = Duration.ofMinutes(reservaTtlMinutes);
        this.maxEntradasPorClienteEvento = maxEntradasPorClienteEvento;
        this.idempotencyTtl = Duration.ofHours(idempotencyTtlHours);
    }

    @Override
        @Transactional(readOnly = true)
        public DisponibilidadResponseDTO disponibilidad(String idEvento) {
            List<CupoSector> cupos = cupoSectorRepository.findByIdEvento(idEvento);
            if (cupos.isEmpty()) {
                throw new ResourceNotFoundException("Sin cupos comerciales para evento: " + idEvento);
            }
            return ventaMapper.toDisponibilidad(idEvento, cupos);
        }

        @Override
        @Transactional(readOnly = true)
        public DisponibilidadBatchResponseDTO disponibilidadBatch(List<String> idEventos) {
            DisponibilidadBatchResponseDTO out = new DisponibilidadBatchResponseDTO();
            if (idEventos == null || idEventos.isEmpty()) {
                return out;
            }
            // orden estable + dedupe; tope anti-abuso
            Set<String> ordered = new LinkedHashSet<>();
            for (String raw : idEventos) {
                if (raw == null) {
                    continue;
                }
                String id = raw.trim();
                if (!id.isEmpty()) {
                    ordered.add(id);
                }
                if (ordered.size() >= 50) {
                    break;
                }
            }
            if (ordered.isEmpty()) {
                return out;
            }
            List<CupoSector> all = cupoSectorRepository.findByIdEventoIn(ordered);
            Map<String, List<CupoSector>> byEvento =
                    all.stream().collect(Collectors.groupingBy(CupoSector::getIdEvento));
            for (String id : ordered) {
                List<CupoSector> cupos = byEvento.getOrDefault(id, List.of());
                out.getItems().add(ventaMapper.toDisponibilidad(id, cupos));
            }
            return out;
        }

        @Override
        @Transactional
        public DisponibilidadResponseDTO abrirCupos(String idEvento, AbrirCuposRequestDTO request) {
            if (idEvento == null || idEvento.isBlank()) {
                throw new IllegalArgumentException("idEvento es obligatorio");
            }
            if (request == null || request.getSectores() == null || request.getSectores().isEmpty()) {
                throw new IllegalArgumentException("sectores es obligatorio (al menos uno con cupo ≥ 1)");
            }

            String eventoId = idEvento.trim();
            for (AbrirCuposRequestDTO.Item item : request.getSectores()) {
                if (item == null || item.getIdSector() == null || item.getIdSector().isBlank()) {
                    throw new IllegalArgumentException("sectores[].idSector es obligatorio");
                }
                Integer cap = item.getCapacidadTotal();
                if (cap == null || cap < 1) {
                    throw new IllegalArgumentException(
                            "sectores[].capacidadTotal debe ser ≥ 1 (sector=" + item.getIdSector() + ")");
                }
                String idSector = item.getIdSector().trim();
                Optional<CupoSector> existing = cupoSectorRepository.findByIdEventoAndIdSector(eventoId, idSector);
                if (existing.isPresent()) {
                    CupoSector cupo = existing.get();
                    // No resetear stock ya movido; solo subir techo si el org amplió capacidad
                    int oldTotal = cupo.getCapacidadTotal() != null ? cupo.getCapacidadTotal() : 0;
                    if (cap > oldTotal) {
                        int delta = cap - oldTotal;
                        cupo.setCapacidadTotal(cap);
                        cupo.setCantidadDisponible(
                                (cupo.getCantidadDisponible() != null ? cupo.getCantidadDisponible() : 0) + delta);
                        cupoSectorRepository.save(cupo);
                    }
                    continue;
                }
                CupoSector nuevo = new CupoSector();
                nuevo.setIdCupo(UUID.randomUUID().toString());
                nuevo.setIdEvento(eventoId);
                nuevo.setIdSector(idSector);
                nuevo.setCapacidadTotal(cap);
                nuevo.setCantidadDisponible(cap);
                nuevo.setCantidadReservada(0);
                nuevo.setCantidadVendida(0);
                cupoSectorRepository.save(nuevo);
            }
            return disponibilidad(eventoId);
                    }

                @Override
                @Transactional
                public DisponibilidadResponseDTO liberarCupoDevolucion(String idEvento, LiberarCupoRequestDTO request) {
                    if (idEvento == null || idEvento.isBlank()) {
                        throw new IllegalArgumentException("idEvento es obligatorio");
                    }
                    if (request == null || request.getIdSector() == null || request.getIdSector().isBlank()) {
                        throw new IllegalArgumentException("idSector es obligatorio");
                    }
                    int q = request.getCantidad() == null ? 1 : request.getCantidad();
                    if (q < 1) {
                        throw new IllegalArgumentException("cantidad debe ser ≥ 1");
                    }
                    String eventoId = idEvento.trim();
                    String idSector = request.getIdSector().trim();

                    CupoSector cupo = cupoSectorRepository
                            .findByEventoAndSectorForUpdate(eventoId, idSector)
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Sin cupo comercial evento=" + eventoId + " sector=" + idSector));

                    int vendida = cupo.getCantidadVendida() != null ? cupo.getCantidadVendida() : 0;
                    int disponible = cupo.getCantidadDisponible() != null ? cupo.getCantidadDisponible() : 0;
                    int capacidad = cupo.getCapacidadTotal() != null ? cupo.getCapacidadTotal() : 0;
                    int reservada = cupo.getCantidadReservada() != null ? cupo.getCantidadReservada() : 0;

                    if (vendida < q) {
                        throw new BusinessConflictException(
                                "No hay vendidas suficientes para liberar (vendida=" + vendida + ", pedir=" + q + ")");
                    }
                    // Invariante: disponible + reservada + vendida ≈ capacidad (con holgura post-ajustes)
                    int nuevaVendida = vendida - q;
                    int nuevaDisponible = disponible + q;
                    if (capacidad > 0 && nuevaDisponible + reservada + nuevaVendida > capacidad) {
                        // Capar disponible al techo residual (no inventar stock sobre capacidad)
                        nuevaDisponible = Math.max(0, capacidad - reservada - nuevaVendida);
                    }

                    cupo.setCantidadVendida(nuevaVendida);
                    cupo.setCantidadDisponible(nuevaDisponible);
                    cupoSectorRepository.save(cupo);
                    log.info(
                            "Cupo liberado por devolución evento={} sector={} q={} vendida {}→{} disp {}→{}",
                            eventoId,
                            idSector,
                            q,
                            vendida,
                            nuevaVendida,
                            disponible,
                            nuevaDisponible);
                    return disponibilidad(eventoId);
                }

                    @Override
                    @Transactional
                    public ReservaResponseDTO crearReserva(
                            String idCliente, ReservaCreateRequestDTO request, String idempotencyKey) {
        // idempotency de reserva: reusa si la key apunta a id ya creado (via redis compra-style)
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = stockCache.getIdempotentCompraId("reserva:" + idempotencyKey);
            if (existing.isPresent()) {
                return obtenerReserva(idCliente, existing.get());
            }
        }

        EventoDetalleFeignDTO evento = fetchEvento(request.idEvento());
        if (evento.getEstado() == null || !"PUBLICADO".equalsIgnoreCase(evento.getEstado())) {
            throw new BusinessConflictException(
                    "Solo se puede reservar eventos PUBLICADO (estado=" + evento.getEstado() + ")");
        }

        Map<String, BigDecimal> precios = preciosPorSector(evento);
        Map<String, Integer> qtyBySector = mergeItems(request.items());

        int nuevas = qtyBySector.values().stream().mapToInt(Integer::intValue).sum();
        assertLimiteEntradas(idCliente, request.idEvento(), nuevas);

        LocalDateTime now = nowUtc();
        Reserva reserva = new Reserva();
        reserva.setIdReserva(UUID.randomUUID().toString());
        reserva.setIdCliente(idCliente);
        reserva.setIdEvento(request.idEvento());
        reserva.setFechaCreacion(now);
        reserva.setFechaExpiracion(now.plus(reservaTtl));
        reserva.setEstado(EST_RES_PENDIENTE);

        BigDecimal total = BigDecimal.ZERO;
        List<DetalleReserva> detalles = new java.util.ArrayList<>();

        for (Map.Entry<String, Integer> e : qtyBySector.entrySet()) {
            String idSector = e.getKey();
            int cantidad = e.getValue();
            BigDecimal precio = precios.get(idSector);
            if (precio == null) {
                throw new IllegalArgumentException("Sin precio activo para sector " + idSector);
            }

            CupoSector cupo = lockCupo(request.idEvento(), idSector);
            if (cupo.getCantidadDisponible() < cantidad) {
                throw new BusinessConflictException(
                        "Cupo insuficiente sector " + idSector + " (disp=" + cupo.getCantidadDisponible()
                                + ", ped=" + cantidad + ")");
            }
            cupo.setCantidadDisponible(cupo.getCantidadDisponible() - cantidad);
            cupo.setCantidadReservada(cupo.getCantidadReservada() + cantidad);
            cupoSectorRepository.save(cupo);

            DetalleReserva det = new DetalleReserva();
            det.setIdDetalle(UUID.randomUUID().toString());
            det.setReserva(reserva);
            det.setIdSector(idSector);
            det.setCantidad(cantidad);
            det.setPrecioUnitario(precio);
            detalles.add(det);

            total = total.add(precio.multiply(BigDecimal.valueOf(cantidad)));
        }

        reserva.setTotal(total);
        reservaRepository.save(reserva);
        detalleReservaRepository.saveAll(detalles);

        stockCache.putReservaHold(reserva.getIdReserva(), idCliente, reservaTtl);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            stockCache.putIdempotentCompraId("reserva:" + idempotencyKey, reserva.getIdReserva(), idempotencyTtl);
        }

        return ventaMapper.toReserva(reserva, detalles);
    }

    @Override
    @Transactional
    public ReservaResponseDTO obtenerReserva(String idCliente, String idReserva) {
        Reserva reserva = requireReservaPropia(idCliente, idReserva);
        maybeExpire(reserva);
        return ventaMapper.toReserva(reserva, detalleReservaRepository.findByReservaIdReserva(idReserva));
    }

    @Override
    @Transactional
    public ReservaResponseDTO confirmarReserva(String idCliente, String idReserva) {
        Reserva reserva = requireReservaPropia(idCliente, idReserva);
        ensureActiva(reserva);
        if (EST_RES_CONFIRMADA.equals(reserva.getEstado())) {
            return ventaMapper.toReserva(reserva, detalleReservaRepository.findByReservaIdReserva(idReserva));
        }
        if (!EST_RES_PENDIENTE.equals(reserva.getEstado())) {
            throw new BusinessConflictException("No se puede confirmar reserva en estado " + reserva.getEstado());
        }
        reserva.setEstado(EST_RES_CONFIRMADA);
        reservaRepository.save(reserva);
        return ventaMapper.toReserva(reserva, detalleReservaRepository.findByReservaIdReserva(idReserva));
    }

    @Override
    @Transactional
    public ReservaResponseDTO cancelarReserva(String idCliente, String idReserva) {
        Reserva reserva = requireReservaPropia(idCliente, idReserva);
        if (EST_RES_CANCELADA.equals(reserva.getEstado())
                || EST_RES_EXPIRADA.equals(reserva.getEstado())
                || EST_RES_COMPRADA.equals(reserva.getEstado())) {
            return ventaMapper.toReserva(reserva, detalleReservaRepository.findByReservaIdReserva(idReserva));
        }
        if (compraRepository.findByReservaIdReserva(idReserva).filter(c -> EST_COMPRA_PENDIENTE.equals(c.getEstado())
                        || EST_COMPRA_APROBADA.equals(c.getEstado()))
                .isPresent()) {
            throw new BusinessConflictException("Reserva con compra en curso o aprobada; no cancelable");
        }
        liberarCupoDeReserva(reserva);
        reserva.setEstado(EST_RES_CANCELADA);
        reservaRepository.save(reserva);
        stockCache.releaseReservaHold(idReserva);
        return ventaMapper.toReserva(reserva, detalleReservaRepository.findByReservaIdReserva(idReserva));
    }

    @Override
    @Transactional
    public ReservaResponseDTO renovarReserva(String idCliente, String idReserva) {
        Reserva reserva = requireReservaPropia(idCliente, idReserva);
        ensureActiva(reserva);
        LocalDateTime now = nowUtc();
        reserva.setFechaExpiracion(now.plus(reservaTtl));
        reservaRepository.save(reserva);
        stockCache.renewReservaHold(idReserva, reservaTtl);
        return ventaMapper.toReserva(reserva, detalleReservaRepository.findByReservaIdReserva(idReserva));
    }

    @Override
    @Transactional
    public CompraResponseDTO crearCompra(
            String idCliente, CompraCreateRequestDTO request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Header Idempotency-Key requerido para POST /compras");
        }
        var cached = stockCache.getIdempotentCompraId(idempotencyKey);
        if (cached.isPresent()) {
            return obtenerCompra(idCliente, cached.get());
        }

        Reserva reserva = requireReservaPropia(idCliente, request.idReserva());
        ensureActiva(reserva);

        compraRepository
                .findByReservaIdReserva(reserva.getIdReserva())
                .ifPresent(c -> {
                    throw new BusinessConflictException("Ya existe compra para la reserva: " + c.getIdCompra());
                });

        Compra compra = new Compra();
        compra.setIdCompra(UUID.randomUUID().toString());
        compra.setReserva(reserva);
        compra.setIdCliente(idCliente);
        compra.setFechaCompra(nowUtc());
        compra.setTotal(reserva.getTotal());
        compra.setMoneda("ARS");
        compra.setEstado(EST_COMPRA_PENDIENTE);
        compra.setEmisionEstado(EmisionEntradasService.EMISION_NO_REQUIERE);
        compra.setEmisionIntentos(0);
        compraRepository.save(compra);

        stockCache.putIdempotentCompraId(idempotencyKey, compra.getIdCompra(), idempotencyTtl);
        return ventaMapper.toCompra(compra);
    }

    @Override
    @Transactional(readOnly = true)
    public CompraResponseDTO obtenerCompra(String idCliente, String idCompra) {
        Compra compra = requireCompraPropia(idCliente, idCompra);
        return ventaMapper.toCompra(compra);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraResponseDTO> listarCompras(String idCliente) {
        return compraRepository.findByIdClienteOrderByFechaCompraDesc(idCliente).stream()
                .map(ventaMapper::toCompra)
                .toList();
    }

    @Override
    @Transactional
    public PagoResponseDTO iniciarPago(String idCliente, String idCompra, PagoCreateRequestDTO request) {
        Compra compra = requireCompraPropia(idCliente, idCompra);
        if (!EST_COMPRA_PENDIENTE.equals(compra.getEstado())) {
            throw new BusinessConflictException("Compra no admite pago en estado " + compra.getEstado());
        }
        Reserva reserva = compra.getReserva();
        if (reserva != null) {
            ensureActiva(reserva);
        }
        return procesarPagoMock(compra, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponseDTO> listarPagos(String idCliente, String idCompra) {
        requireCompraPropia(idCliente, idCompra);
        return pagoRepository.findByCompraIdCompraOrderByFechaPagoDesc(idCompra).stream()
                .map(ventaMapper::toPago)
                .toList();
    }

    @Override
    @Transactional
    public PagoResponseDTO reintentarPago(String idCliente, String idPago, PagoCreateRequestDTO request) {
        Pago anterior = pagoRepository
                .findById(idPago)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado: " + idPago));
        Compra compra = anterior.getCompra();
        if (!idCliente.equals(compra.getIdCliente())) {
            throw new ResourceNotFoundException("Pago no encontrado: " + idPago);
        }
        if (!EST_PAGO_RECHAZADO.equals(anterior.getEstado())) {
            throw new BusinessConflictException("Solo se reintenta pago RECHAZADO");
        }
        if (!EST_COMPRA_PENDIENTE.equals(compra.getEstado())
                && !EST_COMPRA_RECHAZADA.equals(compra.getEstado())) {
            throw new BusinessConflictException("Compra no reintentable en estado " + compra.getEstado());
        }
        // reabrir compra a pendiente si quedó rechazada y la reserva sigue activa
        if (EST_COMPRA_RECHAZADA.equals(compra.getEstado())) {
            Reserva r = compra.getReserva();
            if (r == null || !RESERVAS_ACTIVAS.contains(r.getEstado())) {
                throw new BusinessConflictException("No hay reserva activa para reintentar el pago");
            }
            ensureActiva(r);
            compra.setEstado(EST_COMPRA_PENDIENTE);
            compraRepository.save(compra);
        }
        return procesarPagoMock(compra, request);
    }

    @Override
    @Transactional
    public int expirarReservasVencidas() {
        List<Reserva> vencidas = reservaRepository.findExpiradas(RESERVAS_ACTIVAS, nowUtc());
        int n = 0;
        for (Reserva r : vencidas) {
            expireOne(r);
            n++;
        }
        return n;
    }

    // --- helpers ---

    private PagoResponseDTO procesarPagoMock(Compra compra, PagoCreateRequestDTO request) {
        boolean aprobar = true;
        if (request != null && request.resultado() != null && !request.resultado().isBlank()) {
            String r = request.resultado().trim().toUpperCase();
            if (r.startsWith("RECHAZ")) {
                aprobar = false;
            } else if (!(r.startsWith("APROB") || "OK".equals(r) || "APPROVE".equals(r))) {
                throw new IllegalArgumentException("resultado debe ser APROBAR o RECHAZAR");
            }
        }
        String proveedor = (request != null && request.proveedor() != null && !request.proveedor().isBlank())
                ? request.proveedor().trim()
                : "MOCK_PASARELA";

        Pago pago = new Pago();
        pago.setIdPago(UUID.randomUUID().toString());
        pago.setCompra(compra);
        pago.setProveedor(proveedor);
        pago.setIdTransaccionExterna("mock-" + UUID.randomUUID());
        pago.setMonto(compra.getTotal());
        pago.setFechaPago(nowUtc());

        if (aprobar) {
            pago.setEstado(EST_PAGO_APROBADO);
            pagoRepository.save(pago);
            confirmarVenta(compra);
        } else {
            pago.setEstado(EST_PAGO_RECHAZADO);
            pagoRepository.save(pago);
            rechazarVenta(compra);
        }
        return ventaMapper.toPago(pago);
    }

    private void confirmarVenta(Compra compra) {
        Reserva reserva = compra.getReserva();
        if (reserva == null) {
            throw new BusinessConflictException("Compra sin reserva asociada");
        }
        ensureActiva(reserva);
        List<DetalleReserva> detalles =
                detalleReservaRepository.findByReservaIdReserva(reserva.getIdReserva());
        for (DetalleReserva d : detalles) {
            CupoSector cupo = lockCupo(reserva.getIdEvento(), d.getIdSector());
            int q = d.getCantidad();
            if (cupo.getCantidadReservada() < q) {
                throw new BusinessConflictException(
                        "Inconsistencia cupo reservado sector " + d.getIdSector());
            }
            cupo.setCantidadReservada(cupo.getCantidadReservada() - q);
            cupo.setCantidadVendida(cupo.getCantidadVendida() + q);
            cupoSectorRepository.save(cupo);
        }
        reserva.setEstado(EST_RES_COMPRADA);
        reservaRepository.save(reserva);
        compra.setEstado(EST_COMPRA_APROBADA);
        emisionEntradasService.marcarPendienteYAgendar(compra);
        compraRepository.save(compra);
        stockCache.releaseReservaHold(reserva.getIdReserva());
    }

    private void rechazarVenta(Compra compra) {
        compra.setEstado(EST_COMPRA_RECHAZADA);
        compraRepository.save(compra);
        Reserva reserva = compra.getReserva();
        if (reserva != null && RESERVAS_ACTIVAS.contains(reserva.getEstado())) {
            liberarCupoDeReserva(reserva);
            reserva.setEstado(EST_RES_CANCELADA);
            reservaRepository.save(reserva);
            stockCache.releaseReservaHold(reserva.getIdReserva());
        }
    }

    private void liberarCupoDeReserva(Reserva reserva) {
        List<DetalleReserva> detalles =
                detalleReservaRepository.findByReservaIdReserva(reserva.getIdReserva());
        for (DetalleReserva d : detalles) {
            CupoSector cupo = lockCupo(reserva.getIdEvento(), d.getIdSector());
            int q = d.getCantidad();
            int reservada = Math.max(0, cupo.getCantidadReservada() - q);
            int liberar = cupo.getCantidadReservada() - reservada;
            cupo.setCantidadReservada(reservada);
            cupo.setCantidadDisponible(cupo.getCantidadDisponible() + liberar);
            cupoSectorRepository.save(cupo);
        }
    }

    private void maybeExpire(Reserva reserva) {
        if (RESERVAS_ACTIVAS.contains(reserva.getEstado())
                && reserva.getFechaExpiracion() != null
                && reserva.getFechaExpiracion().isBefore(nowUtc())) {
            expireOne(reserva);
        }
    }

    private void expireOne(Reserva reserva) {
        if (!RESERVAS_ACTIVAS.contains(reserva.getEstado())) {
            return;
        }
        // Si ya hay compra APROBADA, la reserva no debía seguir "activa": alinear a COMPRADA.
        var compraOpt = compraRepository.findByReservaIdReserva(reserva.getIdReserva());
        if (compraOpt.isPresent() && EST_COMPRA_APROBADA.equals(compraOpt.get().getEstado())) {
            reserva.setEstado(EST_RES_COMPRADA);
            reservaRepository.save(reserva);
            stockCache.releaseReservaHold(reserva.getIdReserva());
            return;
        }
        compraOpt
                .filter(c -> EST_COMPRA_PENDIENTE.equals(c.getEstado()))
                .ifPresent(c -> {
                    c.setEstado(EST_COMPRA_RECHAZADA);
                    compraRepository.save(c);
                });
        liberarCupoDeReserva(reserva);
        reserva.setEstado(EST_RES_EXPIRADA);
        reservaRepository.save(reserva);
        stockCache.releaseReservaHold(reserva.getIdReserva());
        log.info("Reserva expirada {}", reserva.getIdReserva());
    }

    private void ensureActiva(Reserva reserva) {
        maybeExpire(reserva);
        if (!RESERVAS_ACTIVAS.contains(reserva.getEstado())) {
            throw new BusinessConflictException("Reserva no activa (estado=" + reserva.getEstado() + ")");
        }
        if (reserva.getFechaExpiracion() != null && reserva.getFechaExpiracion().isBefore(nowUtc())) {
            throw new BusinessConflictException("Reserva expirada");
        }
    }

    private Reserva requireReservaPropia(String idCliente, String idReserva) {
        Reserva reserva = reservaRepository
                .findById(idReserva)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + idReserva));
        if (!idCliente.equals(reserva.getIdCliente())) {
            throw new ResourceNotFoundException("Reserva no encontrada: " + idReserva);
        }
        return reserva;
    }

    private Compra requireCompraPropia(String idCliente, String idCompra) {
        Compra compra = compraRepository
                .findById(idCompra)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada: " + idCompra));
        if (!idCliente.equals(compra.getIdCliente())) {
            throw new ResourceNotFoundException("Compra no encontrada: " + idCompra);
        }
        return compra;
    }

    private CupoSector lockCupo(String idEvento, String idSector) {
        return cupoSectorRepository
                .findByEventoAndSectorForUpdate(idEvento, idSector)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cupo no encontrado evento=" + idEvento + " sector=" + idSector));
    }

    private void assertLimiteEntradas(String idCliente, String idEvento, int nuevas) {
        long yaCompradasOPendientes = Optional.ofNullable(
                        compraRepository.sumCantidadByClienteEventoAndEstados(
                                idCliente, idEvento, COMPRAS_QUE_CUENTAN))
                .orElse(0L);
        long enReservasActivas = 0;
        for (Reserva r :
                reservaRepository.findByIdClienteAndIdEventoAndEstadoIn(idCliente, idEvento, RESERVAS_ACTIVAS)) {
            enReservasActivas += detalleReservaRepository.findByReservaIdReserva(r.getIdReserva()).stream()
                    .mapToInt(DetalleReserva::getCantidad)
                    .sum();
        }
        if (yaCompradasOPendientes + enReservasActivas + nuevas > maxEntradasPorClienteEvento) {
            throw new BusinessConflictException(
                    "Límite de entradas por cliente/evento (" + maxEntradasPorClienteEvento + ")");
        }
    }

    private EventoDetalleFeignDTO fetchEvento(String idEvento) {
        try {
            return eventosClient.getEvento(idEvento);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Evento no encontrado: " + idEvento);
        } catch (FeignException e) {
            throw new IllegalArgumentException("No se pudo consultar msvc-eventos: HTTP " + e.status());
        }
    }

    private static Map<String, BigDecimal> preciosPorSector(EventoDetalleFeignDTO evento) {
        Map<String, BigDecimal> map = new HashMap<>();
        if (evento.getSectores() == null) {
            return map;
        }
        for (SectorFeignDTO s : evento.getSectores()) {
            if (s.getId() != null && s.getPrecio() != null) {
                map.put(s.getId(), s.getPrecio());
            }
        }
        return map;
    }

    private static Map<String, Integer> mergeItems(List<ReservaCreateRequestDTO.Item> items) {
        Map<String, Integer> map = new HashMap<>();
        for (ReservaCreateRequestDTO.Item item : items) {
            map.merge(item.idSector(), item.cantidad(), Integer::sum);
        }
        return map;
    }

    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
