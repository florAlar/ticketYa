package ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import ventas.clients.EventosClient;
import ventas.clients.EventosClient.EventoDetalleFeignDTO;
import ventas.clients.EventosClient.SectorFeignDTO;
import ventas.dto.AsientoRefDTO;
import ventas.dto.DetalleReservaDTO;
import ventas.dto.DisponibilidadResponseDTO;
import ventas.dto.LiberarCupoRequestDTO;
import ventas.dto.PagoCreateRequestDTO;
import ventas.dto.PagoResponseDTO;
import ventas.dto.ReservaCreateRequestDTO;
import ventas.dto.ReservaResponseDTO;
import ventas.entity.AsientoReservado;
import ventas.entity.Compra;
import ventas.entity.CupoSector;
import ventas.entity.DetalleReserva;
import ventas.entity.Pago;
import ventas.entity.Reserva;
import ventas.exception.BusinessConflictException;
import ventas.mapper.VentaMapper;
import ventas.redis.StockCache;
import ventas.repository.AsientoReservadoRepository;
import ventas.repository.CompraRepository;
import ventas.repository.CupoSectorRepository;
import ventas.repository.DetalleReservaRepository;
import ventas.repository.PagoRepository;
import ventas.repository.ReservaRepository;

/** Reserva, venta, liberación y disponibilidad de sectores numerados (asientos virtuales). */
class VentaServiceAsientosTest {

    private static final String ID_EVENTO = "e0000000-0000-4000-8000-000000000001";
    private static final String ID_SECTOR = "c0000000-0000-4000-8000-000000000012";
    private static final String ID_CLIENTE = "b0000000-0000-4000-8000-000000000001";
    private static final String ID_RESERVA = "r0000000-0000-4000-8000-000000000001";
    private static final String ID_COMPRA = "x0000000-0000-4000-8000-000000000001";

    private CupoSectorRepository cupoRepo;
    private ReservaRepository reservaRepo;
    private DetalleReservaRepository detalleRepo;
    private AsientoReservadoRepository asientoRepo;
    private CompraRepository compraRepo;
    private PagoRepository pagoRepo;
    private EventosClient eventosClient;
    private StockCache stockCache;
    private VentaMapper mapper;
    private EmisionEntradasService emision;
    private VentaServiceImpl service;
    private CupoSector cupo;

    @BeforeEach
    void setUp() {
        cupoRepo = mock(CupoSectorRepository.class);
        reservaRepo = mock(ReservaRepository.class);
        detalleRepo = mock(DetalleReservaRepository.class);
        asientoRepo = mock(AsientoReservadoRepository.class);
        compraRepo = mock(CompraRepository.class);
        pagoRepo = mock(PagoRepository.class);
        eventosClient = mock(EventosClient.class);
        stockCache = mock(StockCache.class);
        mapper = mock(VentaMapper.class);
        emision = mock(EmisionEntradasService.class);
        service = new VentaServiceImpl(
                cupoRepo, reservaRepo, detalleRepo, asientoRepo, compraRepo, pagoRepo,
                eventosClient, stockCache, mapper, emision, 10L, 6, 24L);

        cupo = new CupoSector();
        cupo.setIdEvento(ID_EVENTO);
        cupo.setIdSector(ID_SECTOR);
        cupo.setCapacidadTotal(100);
        cupo.setCantidadDisponible(100);
        cupo.setCantidadReservada(0);
        cupo.setCantidadVendida(0);
        when(cupoRepo.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR)).thenReturn(Optional.of(cupo));
        when(stockCache.getIdempotentCompraId(anyString())).thenReturn(Optional.empty());
        when(reservaRepo.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toReserva(any(Reserva.class), anyList())).thenAnswer(inv -> {
            ReservaResponseDTO dto = new ReservaResponseDTO();
            for (DetalleReserva d : inv.<List<DetalleReserva>>getArgument(1)) {
                DetalleReservaDTO x = new DetalleReservaDTO();
                x.setIdSector(d.getIdSector());
                x.setCantidad(d.getCantidad());
                dto.getDetalles().add(x);
            }
            return dto;
        });
    }

    // ---------- helpers ----------

    private void eventoConSector(boolean numerado, Integer filas, Integer porFila) {
        EventoDetalleFeignDTO evento = new EventoDetalleFeignDTO();
        evento.setId(ID_EVENTO);
        evento.setEstado("PUBLICADO");
        SectorFeignDTO s = new SectorFeignDTO();
        s.setId(ID_SECTOR);
        s.setNombre("Platea");
        s.setPrecio(BigDecimal.valueOf(180000));
        s.setEsNumerado(numerado);
        s.setFilas(filas);
        s.setAsientosPorFila(porFila);
        evento.setSectores(new ArrayList<>(List.of(s)));
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(evento);
    }

    private ReservaCreateRequestDTO pedido(Integer cantidad, AsientoRefDTO... asientos) {
        return new ReservaCreateRequestDTO(
                ID_EVENTO,
                List.of(new ReservaCreateRequestDTO.Item(
                        ID_SECTOR, cantidad, asientos.length == 0 ? null : List.of(asientos))));
    }

    private static AsientoRefDTO a(String fila, int numero) {
        return new AsientoRefDTO(fila, numero);
    }

    private static AsientoReservado fila(String idSector, String fila, int numero, String estado) {
        AsientoReservado r = new AsientoReservado();
        r.setIdSector(idSector);
        r.setIdEvento(ID_EVENTO);
        r.setFila(fila);
        r.setNumero(numero);
        r.setEstado(estado);
        return r;
    }

    private List<AsientoReservado> capturarSaveAll() {
        List<AsientoReservado> guardados = new ArrayList<>();
        doAnswer(inv -> {
            Iterable<AsientoReservado> it = inv.getArgument(0);
            it.forEach(guardados::add);
            return guardados;
        }).when(asientoRepo).saveAll(org.mockito.ArgumentMatchers.<Iterable<AsientoReservado>>any());
        return guardados;
    }

    // ---------- crearReserva: sector numerado ----------

    @Test
    void crearReserva_numerado_tomaAsientosYActualizaCupo() {
        eventoConSector(true, 10, 20);
        List<AsientoReservado> guardados = capturarSaveAll();

        ReservaResponseDTO res = service.crearReserva(ID_CLIENTE, pedido(null, a("a", 1), a("A", 2)), null);

        assertEquals(2, guardados.size());
        assertEquals("A", guardados.get(0).getFila()); // fila normalizada a mayúscula
        assertEquals(1, guardados.get(0).getNumero());
        assertEquals(VentaServiceImpl.EST_ASIENTO_RESERVADO, guardados.get(0).getEstado());
        assertEquals(ID_SECTOR, guardados.get(0).getIdSector());
        assertEquals(98, cupo.getCantidadDisponible());
        assertEquals(2, cupo.getCantidadReservada());
        assertEquals(1, res.getDetalles().size());
        assertEquals(2, res.getDetalles().get(0).getCantidad());
        assertEquals(2, res.getDetalles().get(0).getAsientos().size());
        verify(asientoRepo).flush();
    }

    @Test
    void crearReserva_numeradoSinAsientos_throws() {
        eventoConSector(true, 10, 20);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearReserva(ID_CLIENTE, pedido(2), null));
    }

    @Test
    void crearReserva_numeradoCantidadNoCoincide_throws() {
        eventoConSector(true, 10, 20);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearReserva(ID_CLIENTE, pedido(3, a("A", 1), a("A", 2)), null));
    }

    @Test
    void crearReserva_numeradoCantidadCoincide_ok() {
        eventoConSector(true, 10, 20);
        capturarSaveAll();
        service.crearReserva(ID_CLIENTE, pedido(2, a("A", 1), a("A", 2)), null);
        assertEquals(2, cupo.getCantidadReservada());
    }

    @Test
    void crearReserva_asientoFueraDeLayout_throws() {
        eventoConSector(true, 10, 20);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearReserva(ID_CLIENTE, pedido(null, a("K", 1)), null)); // solo hay filas A..J
        assertThrows(IllegalArgumentException.class,
                () -> service.crearReserva(ID_CLIENTE, pedido(null, a("A", 21)), null)); // 20 por fila
        assertThrows(IllegalArgumentException.class,
                () -> service.crearReserva(ID_CLIENTE, pedido(null, a("A1", 1)), null)); // rótulo inválido
    }

    @Test
    void crearReserva_asientoRepetidoEnPedido_throws() {
        eventoConSector(true, 10, 20);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearReserva(ID_CLIENTE, pedido(null, a("A", 1), a("a", 1)), null));
    }

    @Test
    void crearReserva_numeradoSinLayout_throws() {
        eventoConSector(true, null, null);
        assertThrows(BusinessConflictException.class,
                () -> service.crearReserva(ID_CLIENTE, pedido(null, a("A", 1)), null));
    }

    @Test
    void crearReserva_asientoYaTomado_conflictoYNoGuarda() {
        eventoConSector(true, 10, 20);
        when(asientoRepo.existsByIdEventoAndIdSectorAndFilaAndNumero(ID_EVENTO, ID_SECTOR, "A", 2))
                .thenReturn(true);

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> service.crearReserva(ID_CLIENTE, pedido(null, a("A", 1), a("A", 2)), null));

        assertTrue(ex.getMessage().contains("A-2"));
        verify(asientoRepo, never()).saveAll(org.mockito.ArgumentMatchers.<Iterable<AsientoReservado>>any());
    }

    @Test
    void crearReserva_carreraEnClaveUnica_conflicto() {
        eventoConSector(true, 10, 20);
        doThrow(new DataIntegrityViolationException("uk_asiento_evento_sector_fila_numero"))
                .when(asientoRepo).flush();

        assertThrows(BusinessConflictException.class,
                () -> service.crearReserva(ID_CLIENTE, pedido(null, a("A", 1)), null));
    }

    // ---------- crearReserva: sector general ----------

    @Test
    void crearReserva_generalConAsientos_throws() {
        eventoConSector(false, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearReserva(ID_CLIENTE, pedido(1, a("A", 1)), null));
    }

    @Test
    void crearReserva_generalSinCantidad_throws() {
        eventoConSector(false, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearReserva(ID_CLIENTE, pedido(null), null));
    }

    @Test
    void crearReserva_general_noTocaAsientos() {
        eventoConSector(false, null, null);

        service.crearReserva(ID_CLIENTE, pedido(3), null);

        assertEquals(3, cupo.getCantidadReservada());
        verify(asientoRepo, never()).existsByIdEventoAndIdSectorAndFilaAndNumero(
                anyString(), anyString(), anyString(), any());
        verify(asientoRepo, never()).saveAll(org.mockito.ArgumentMatchers.<Iterable<AsientoReservado>>any());
    }

    // ---------- venta, cancelación y expiración ----------

    @Test
    void pagoAprobado_marcaAsientosVendidos() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("CONFIRMADA");
        reserva.setFechaExpiracion(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente(ID_CLIENTE);
        compra.setEstado("PENDIENTE_PAGO");
        compra.setReserva(reserva);
        compra.setTotal(BigDecimal.valueOf(360000));
        when(compraRepo.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        when(detalleRepo.findByReservaIdReserva(ID_RESERVA)).thenReturn(new ArrayList<>());
        when(pagoRepo.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emision).marcarPendienteYAgendar(any(Compra.class));
        when(mapper.toPago(any(Pago.class))).thenReturn(new PagoResponseDTO());

        service.iniciarPago(ID_CLIENTE, ID_COMPRA, new PagoCreateRequestDTO("APROBAR", "MOCK_PASARELA"));

        verify(asientoRepo).marcarVendidos(ID_RESERVA);
        verify(asientoRepo, never()).eliminarPorReserva(anyString());
    }

    @Test
    void pagoRechazado_liberaAsientos() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("CONFIRMADA");
        reserva.setFechaExpiracion(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente(ID_CLIENTE);
        compra.setEstado("PENDIENTE_PAGO");
        compra.setReserva(reserva);
        compra.setTotal(BigDecimal.valueOf(360000));
        when(compraRepo.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        when(detalleRepo.findByReservaIdReserva(ID_RESERVA)).thenReturn(new ArrayList<>());
        when(pagoRepo.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toPago(any(Pago.class))).thenReturn(new PagoResponseDTO());

        service.iniciarPago(ID_CLIENTE, ID_COMPRA, new PagoCreateRequestDTO("RECHAZAR", "MOCK_PASARELA"));

        verify(asientoRepo).eliminarPorReserva(ID_RESERVA);
        verify(asientoRepo, never()).marcarVendidos(anyString());
    }

    @Test
    void cancelarReserva_liberaAsientos() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente(ID_CLIENTE);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("PENDIENTE");
        when(reservaRepo.findById(ID_RESERVA)).thenReturn(Optional.of(reserva));
        when(compraRepo.findByReservaIdReserva(ID_RESERVA)).thenReturn(Optional.empty());
        when(detalleRepo.findByReservaIdReserva(ID_RESERVA)).thenReturn(new ArrayList<>());

        service.cancelarReserva(ID_CLIENTE, ID_RESERVA);

        verify(asientoRepo).eliminarPorReserva(ID_RESERVA);
    }

    @Test
    void reservaVencida_liberaAsientos() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("PENDIENTE");
        when(reservaRepo.findExpiradas(anyList(), any(LocalDateTime.class))).thenReturn(List.of(reserva));
        when(compraRepo.findByReservaIdReserva(ID_RESERVA)).thenReturn(Optional.empty());
        when(detalleRepo.findByReservaIdReserva(ID_RESERVA)).thenReturn(new ArrayList<>());

        int n = service.expirarReservasVencidas();

        assertEquals(1, n);
        assertEquals("EXPIRADA", reserva.getEstado());
        verify(asientoRepo).eliminarPorReserva(ID_RESERVA);
    }

    @Test
    void obtenerReserva_incluyeAsientosPorSector() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente(ID_CLIENTE);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("PENDIENTE");
        reserva.setFechaExpiracion(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        DetalleReserva det = new DetalleReserva();
        det.setIdSector(ID_SECTOR);
        det.setCantidad(2);
        when(reservaRepo.findById(ID_RESERVA)).thenReturn(Optional.of(reserva));
        when(detalleRepo.findByReservaIdReserva(ID_RESERVA)).thenReturn(List.of(det));
        when(asientoRepo.findByReservaIdReserva(ID_RESERVA)).thenReturn(List.of(
                fila(ID_SECTOR, "A", 1, "RESERVADO"), fila(ID_SECTOR, "A", 2, "RESERVADO")));

        ReservaResponseDTO res = service.obtenerReserva(ID_CLIENTE, ID_RESERVA);

        assertEquals(2, res.getDetalles().get(0).getAsientos().size());
        assertEquals("RESERVADO", res.getDetalles().get(0).getAsientos().get(0).getEstado());
    }

    // ---------- disponibilidad ----------

    @Test
    void disponibilidad_adjuntaAsientosOcupadosOrdenados() {
        when(cupoRepo.findByIdEvento(ID_EVENTO)).thenReturn(List.of(cupo));
        DisponibilidadResponseDTO dto = new DisponibilidadResponseDTO();
        DisponibilidadResponseDTO.CupoSectorDTO sector = new DisponibilidadResponseDTO.CupoSectorDTO();
        sector.setIdSector(ID_SECTOR);
        dto.getSectores().add(sector);
        when(mapper.toDisponibilidad(ID_EVENTO, List.of(cupo))).thenReturn(dto);
        when(asientoRepo.findByIdEvento(ID_EVENTO)).thenReturn(List.of(
                fila(ID_SECTOR, "B", 2, "RESERVADO"),
                fila(ID_SECTOR, "A", 10, "VENDIDO"),
                fila(ID_SECTOR, "A", 2, "RESERVADO"),
                fila(ID_SECTOR, "AA", 1, "VENDIDO")));

        var res = service.disponibilidad(ID_EVENTO);

        var ocupados = res.getSectores().get(0).getAsientosOcupados();
        assertEquals(4, ocupados.size());
        assertEquals("A", ocupados.get(0).getFila());
        assertEquals(2, ocupados.get(0).getNumero());
        assertEquals(10, ocupados.get(1).getNumero());
        assertEquals("B", ocupados.get(2).getFila());
        assertEquals("AA", ocupados.get(3).getFila()); // AA va después de Z
    }

    @Test
    void disponibilidad_sinAsientosTomados_listaVacia() {
        when(cupoRepo.findByIdEvento(ID_EVENTO)).thenReturn(List.of(cupo));
        DisponibilidadResponseDTO dto = new DisponibilidadResponseDTO();
        DisponibilidadResponseDTO.CupoSectorDTO sector = new DisponibilidadResponseDTO.CupoSectorDTO();
        sector.setIdSector(ID_SECTOR);
        dto.getSectores().add(sector);
        when(mapper.toDisponibilidad(ID_EVENTO, List.of(cupo))).thenReturn(dto);

        var res = service.disponibilidad(ID_EVENTO);

        assertTrue(res.getSectores().get(0).getAsientosOcupados().isEmpty());
    }

    // ---------- liberarCupoDevolucion con asientos ----------

    private LiberarCupoRequestDTO devolucion(AsientoRefDTO... asientos) {
        LiberarCupoRequestDTO req = new LiberarCupoRequestDTO();
        req.setIdSector(ID_SECTOR);
        req.setAsientos(List.of(asientos));
        cupo.setCantidadVendida(5);
        cupo.setCantidadDisponible(90);
        cupo.setCantidadReservada(5);
        when(cupoRepo.findByIdEvento(ID_EVENTO)).thenReturn(List.of(cupo));
        when(mapper.toDisponibilidad(anyString(), anyList())).thenReturn(new DisponibilidadResponseDTO());
        return req;
    }

    @Test
    void liberarCupo_conAsientos_liberaCadaUnoYUsaSuCantidad() {
        AsientoReservado v1 = fila(ID_SECTOR, "A", 1, "VENDIDO");
        AsientoReservado v2 = fila(ID_SECTOR, "A", 2, "VENDIDO");
        when(asientoRepo.findByIdEventoAndIdSectorAndFilaAndNumero(ID_EVENTO, ID_SECTOR, "A", 1))
                .thenReturn(Optional.of(v1));
        when(asientoRepo.findByIdEventoAndIdSectorAndFilaAndNumero(ID_EVENTO, ID_SECTOR, "A", 2))
                .thenReturn(Optional.of(v2));

        service.liberarCupoDevolucion(ID_EVENTO, devolucion(a("a", 1), a("A", 2)));

        verify(asientoRepo).delete(v1);
        verify(asientoRepo).delete(v2);
        assertEquals(3, cupo.getCantidadVendida());
        assertEquals(92, cupo.getCantidadDisponible());
    }

    @Test
    void liberarCupo_asientoNoVendido_conflicto() {
        when(asientoRepo.findByIdEventoAndIdSectorAndFilaAndNumero(ID_EVENTO, ID_SECTOR, "A", 1))
                .thenReturn(Optional.empty());

        assertThrows(BusinessConflictException.class,
                () -> service.liberarCupoDevolucion(ID_EVENTO, devolucion(a("A", 1))));
        verify(asientoRepo, never()).delete(any(AsientoReservado.class));
    }

    @Test
    void liberarCupo_asientoSoloReservado_conflicto() {
        when(asientoRepo.findByIdEventoAndIdSectorAndFilaAndNumero(ID_EVENTO, ID_SECTOR, "A", 1))
                .thenReturn(Optional.of(fila(ID_SECTOR, "A", 1, "RESERVADO")));

        assertThrows(BusinessConflictException.class,
                () -> service.liberarCupoDevolucion(ID_EVENTO, devolucion(a("A", 1))));
        verify(asientoRepo, never()).delete(any(AsientoReservado.class));
    }
}
