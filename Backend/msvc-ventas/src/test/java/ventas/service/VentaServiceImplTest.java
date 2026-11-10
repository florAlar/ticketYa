package ventas.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ventas.clients.EventosClient;
import ventas.clients.EventosClient.EventoDetalleFeignDTO;
import ventas.clients.EventosClient.SectorFeignDTO;
import ventas.dto.AbrirCuposRequestDTO;
import ventas.dto.CompraCreateRequestDTO;
import ventas.dto.CompraResponseDTO;
import ventas.dto.DisponibilidadBatchResponseDTO;
import ventas.dto.DisponibilidadResponseDTO;
import ventas.dto.LiberarCupoRequestDTO;
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
import ventas.repository.AsientoReservadoRepository;
import ventas.repository.PagoRepository;
import ventas.repository.ReservaRepository;

class VentaServiceImplTest {

    private CupoSectorRepository cupoSectorRepository;
    private ReservaRepository reservaRepository;
    private DetalleReservaRepository detalleReservaRepository;
    private AsientoReservadoRepository asientoReservadoRepository;
    private CompraRepository compraRepository;
    private PagoRepository pagoRepository;
    private EventosClient eventosClient;
    private StockCache stockCache;
    private VentaMapper ventaMapper;
    private EmisionEntradasService emisionEntradasService;
    private VentaServiceImpl service;

    private static final String ID_EVENTO = "e0000000-0000-4000-8000-000000000001";
    private static final String ID_SECTOR = "c0000000-0000-4000-8000-000000000011";
    private static final String ID_CLIENTE = "b0000000-0000-4000-8000-000000000001";
    private static final String ID_RESERVA = "r0000000-0000-4000-8000-000000000001";
    private static final String ID_COMPRA = "x0000000-0000-4000-8000-000000000001";
    private static final String ID_PAGO = "p0000000-0000-4000-8000-000000000001";

    @BeforeEach
    void setUp() {
        cupoSectorRepository = mock(CupoSectorRepository.class);
        reservaRepository = mock(ReservaRepository.class);
        detalleReservaRepository = mock(DetalleReservaRepository.class);
        asientoReservadoRepository = mock(AsientoReservadoRepository.class);
        compraRepository = mock(CompraRepository.class);
        pagoRepository = mock(PagoRepository.class);
        eventosClient = mock(EventosClient.class);
        stockCache = mock(StockCache.class);
        ventaMapper = mock(VentaMapper.class);
        emisionEntradasService = mock(EmisionEntradasService.class);

        service = new VentaServiceImpl(
                cupoSectorRepository, reservaRepository, detalleReservaRepository,
                asientoReservadoRepository,
                compraRepository, pagoRepository, eventosClient, stockCache,
                ventaMapper, emisionEntradasService,
                10L, 6, 24L);

        reset(cupoSectorRepository, reservaRepository, detalleReservaRepository,
                asientoReservadoRepository,
                compraRepository, pagoRepository, eventosClient, stockCache,
                ventaMapper, emisionEntradasService);

        when(cupoSectorRepository.findByIdEvento(anyString())).thenReturn(new ArrayList<>());
        when(cupoSectorRepository.findByIdEventoIn(anyList())).thenReturn(new ArrayList<>());
        when(stockCache.getIdempotentCompraId(anyString())).thenReturn(Optional.empty());
        when(ventaMapper.toDisponibilidad(anyString(), anyList())).thenReturn(new DisponibilidadResponseDTO());
        when(ventaMapper.toReserva(any(Reserva.class), anyList())).thenReturn(new ReservaResponseDTO());
        when(ventaMapper.toCompra(any(Compra.class))).thenReturn(new CompraResponseDTO());
        when(ventaMapper.toPago(any(Pago.class))).thenReturn(new PagoResponseDTO());
    }

    private ReservaCreateRequestDTO.Item crearItem(String idSector, int cantidad) {
        return new ReservaCreateRequestDTO.Item(idSector, Integer.valueOf(cantidad));
    }

    // ---------- disponibilidad ----------

    @Test
    void disponibilidad_sinCupos_throws() {
        when(cupoSectorRepository.findByIdEvento(ID_EVENTO)).thenReturn(new ArrayList<>());

        assertThrows(ResourceNotFoundException.class,
                () -> service.disponibilidad(ID_EVENTO));
    }

    @Test
    void disponibilidad_conCupos_devuelveDto() {
        CupoSector c = new CupoSector();
        c.setIdSector(ID_SECTOR);
        c.setCantidadDisponible(50);
        c.setCantidadReservada(0);
        c.setCantidadVendida(0);
        c.setCapacidadTotal(100);
        when(cupoSectorRepository.findByIdEvento(ID_EVENTO)).thenReturn(List.of(c));
        DisponibilidadResponseDTO dto = new DisponibilidadResponseDTO();
        dto.setIdEvento(ID_EVENTO);
        dto.setDisponibleTotal(50);
        when(ventaMapper.toDisponibilidad(ID_EVENTO, List.of(c))).thenReturn(dto);

        DisponibilidadResponseDTO res = service.disponibilidad(ID_EVENTO);

        assertNotNull(res);
        assertEquals(50, res.getDisponibleTotal());
    }

    // ---------- disponibilidadBatch ----------

    @Test
    void disponibilidadBatch_null_devuelveVacia() {
        DisponibilidadBatchResponseDTO res = service.disponibilidadBatch(null);

        assertNotNull(res);
        assertTrue(res.getItems().isEmpty());
    }

    @Test
    void disponibilidadBatch_vacio_devuelveVacia() {
        DisponibilidadBatchResponseDTO res = service.disponibilidadBatch(new ArrayList<>());

        assertNotNull(res);
        assertTrue(res.getItems().isEmpty());
    }

    @Test
    void disponibilidadBatch_conIds_devuelveItems() {
        when(cupoSectorRepository.findByIdEventoIn(List.of(ID_EVENTO))).thenReturn(new ArrayList<>());
        DisponibilidadResponseDTO dto = new DisponibilidadResponseDTO();
        dto.setIdEvento(ID_EVENTO);
        when(ventaMapper.toDisponibilidad(eq(ID_EVENTO), anyList())).thenReturn(dto);

        DisponibilidadBatchResponseDTO res = service.disponibilidadBatch(List.of(ID_EVENTO));

        assertNotNull(res);
        assertEquals(1, res.getItems().size());
    }

    @Test
    void disponibilidadBatch_conIdsNulosYVacios_filtraYDedupe() {
        when(cupoSectorRepository.findByIdEventoIn(anyList())).thenReturn(new ArrayList<>());

        List<String> ids = new ArrayList<>();
        ids.add(null);
        ids.add("");
        ids.add("   ");

        DisponibilidadBatchResponseDTO res = service.disponibilidadBatch(ids);

        assertNotNull(res);
        assertTrue(res.getItems().isEmpty());
        verify(cupoSectorRepository, never()).findByIdEventoIn(anyList());
    }

    // ---------- abrirCupos ----------

    @Test
    void abrirCupos_idEventoNulo_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.abrirCupos(null, new AbrirCuposRequestDTO()));
    }

    @Test
    void abrirCupos_idEventoBlanco_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.abrirCupos("   ", new AbrirCuposRequestDTO()));
    }

    @Test
    void abrirCupos_requestNull_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.abrirCupos(ID_EVENTO, null));
    }

    @Test
    void abrirCupos_sectoresVacios_throws() {
        AbrirCuposRequestDTO req = new AbrirCuposRequestDTO();
        req.setSectores(new ArrayList<>());
        assertThrows(IllegalArgumentException.class,
                () -> service.abrirCupos(ID_EVENTO, req));
    }

    @Test
    void abrirCupos_itemNull_throws() {
        AbrirCuposRequestDTO req = new AbrirCuposRequestDTO();
        List<AbrirCuposRequestDTO.Item> items = new ArrayList<>();
        items.add(null);
        req.setSectores(items);
        assertThrows(IllegalArgumentException.class,
                () -> service.abrirCupos(ID_EVENTO, req));
    }

    @Test
    void abrirCupos_idSectorNulo_throws() {
        AbrirCuposRequestDTO req = new AbrirCuposRequestDTO();
        AbrirCuposRequestDTO.Item item = new AbrirCuposRequestDTO.Item();
        item.setIdSector(null);
        item.setCapacidadTotal(5);
        List<AbrirCuposRequestDTO.Item> items = new ArrayList<>();
        items.add(item);
        req.setSectores(items);
        assertThrows(IllegalArgumentException.class,
                () -> service.abrirCupos(ID_EVENTO, req));
    }

    @Test
    void abrirCupos_capacidadCero_throws() {
        AbrirCuposRequestDTO req = new AbrirCuposRequestDTO();
        AbrirCuposRequestDTO.Item item = new AbrirCuposRequestDTO.Item();
        item.setIdSector(ID_SECTOR);
        item.setCapacidadTotal(0);
        List<AbrirCuposRequestDTO.Item> items = new ArrayList<>();
        items.add(item);
        req.setSectores(items);
        assertThrows(IllegalArgumentException.class,
                () -> service.abrirCupos(ID_EVENTO, req));
    }

    @Test
    void abrirCupos_sectorExistente_actualizaTecho() {
        AbrirCuposRequestDTO req = new AbrirCuposRequestDTO();
        AbrirCuposRequestDTO.Item item = new AbrirCuposRequestDTO.Item();
        item.setIdSector(ID_SECTOR);
        item.setCapacidadTotal(200);
        List<AbrirCuposRequestDTO.Item> items = new ArrayList<>();
        items.add(item);
        req.setSectores(items);

        CupoSector existente = new CupoSector();
        existente.setCapacidadTotal(100);
        existente.setCantidadDisponible(30);
        existente.setCantidadReservada(10);
        existente.setCantidadVendida(60);

        when(cupoSectorRepository.findByIdEventoAndIdSector(ID_EVENTO, ID_SECTOR))
                .thenReturn(Optional.of(existente));
        when(cupoSectorRepository.findByIdEvento(ID_EVENTO)).thenReturn(List.of(existente));
        DisponibilidadResponseDTO dto = new DisponibilidadResponseDTO();
        dto.setIdEvento(ID_EVENTO);
        when(ventaMapper.toDisponibilidad(ID_EVENTO, List.of(existente))).thenReturn(dto);

        service.abrirCupos(ID_EVENTO, req);

        assertEquals(200, existente.getCapacidadTotal());
        assertEquals(130, existente.getCantidadDisponible());
        verify(cupoSectorRepository).save(existente);
    }

    @Test
    void abrirCupos_sectorNuevo_creaCupo() {
        AbrirCuposRequestDTO req = new AbrirCuposRequestDTO();
        AbrirCuposRequestDTO.Item item = new AbrirCuposRequestDTO.Item();
        item.setIdSector(ID_SECTOR);
        item.setCapacidadTotal(50);
        List<AbrirCuposRequestDTO.Item> items = new ArrayList<>();
        items.add(item);
        req.setSectores(items);

        when(cupoSectorRepository.findByIdEventoAndIdSector(ID_EVENTO, ID_SECTOR))
                .thenReturn(Optional.empty());
        CupoSector nuevo = new CupoSector();
        nuevo.setIdSector(ID_SECTOR);
        nuevo.setCapacidadTotal(50);
        nuevo.setCantidadDisponible(50);
        when(cupoSectorRepository.save(any(CupoSector.class))).thenReturn(nuevo);
        when(cupoSectorRepository.findByIdEvento(ID_EVENTO)).thenReturn(List.of(nuevo));
        DisponibilidadResponseDTO dto = new DisponibilidadResponseDTO();
        dto.setIdEvento(ID_EVENTO);
        when(ventaMapper.toDisponibilidad(ID_EVENTO, List.of(nuevo))).thenReturn(dto);

        service.abrirCupos(ID_EVENTO, req);

        verify(cupoSectorRepository).save(any(CupoSector.class));
    }

    @Test
    void abrirCupos_sectorExistente_capacidadMenor_noActualiza() {
        AbrirCuposRequestDTO req = new AbrirCuposRequestDTO();
        AbrirCuposRequestDTO.Item item = new AbrirCuposRequestDTO.Item();
        item.setIdSector(ID_SECTOR);
        item.setCapacidadTotal(50);
        List<AbrirCuposRequestDTO.Item> items = new ArrayList<>();
        items.add(item);
        req.setSectores(items);

        CupoSector existente = new CupoSector();
        existente.setCapacidadTotal(100);
        existente.setCantidadDisponible(50);
        existente.setCantidadReservada(10);
        existente.setCantidadVendida(40);

        when(cupoSectorRepository.findByIdEventoAndIdSector(ID_EVENTO, ID_SECTOR))
                .thenReturn(Optional.of(existente));
        when(cupoSectorRepository.findByIdEvento(ID_EVENTO)).thenReturn(List.of(existente));
        DisponibilidadResponseDTO dto = new DisponibilidadResponseDTO();
        dto.setIdEvento(ID_EVENTO);
        when(ventaMapper.toDisponibilidad(ID_EVENTO, List.of(existente))).thenReturn(dto);

        service.abrirCupos(ID_EVENTO, req);

        assertEquals(100, existente.getCapacidadTotal());
        assertEquals(50, existente.getCantidadDisponible());
        verify(cupoSectorRepository, never()).save(existente);
    }

    // ---------- liberarCupoDevolucion ----------

    @Test
    void liberarCupoDevolucion_idEventoNulo_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.liberarCupoDevolucion(null, new LiberarCupoRequestDTO()));
    }

    @Test
    void liberarCupoDevolucion_idSectorNulo_throws() {
        LiberarCupoRequestDTO req = new LiberarCupoRequestDTO();
        req.setIdSector(null);
        assertThrows(IllegalArgumentException.class,
                () -> service.liberarCupoDevolucion(ID_EVENTO, req));
    }

    @Test
    void liberarCupoDevolucion_cantidadCero_throws() {
        LiberarCupoRequestDTO req = new LiberarCupoRequestDTO();
        req.setIdSector(ID_SECTOR);
        req.setCantidad(0);
        assertThrows(IllegalArgumentException.class,
                () -> service.liberarCupoDevolucion(ID_EVENTO, req));
    }

    @Test
    void liberarCupoDevolucion_cupoNoExiste_throws() {
        LiberarCupoRequestDTO req = new LiberarCupoRequestDTO();
        req.setIdSector(ID_SECTOR);
        req.setCantidad(1);
        when(cupoSectorRepository.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.liberarCupoDevolucion(ID_EVENTO, req));
    }

    @Test
    void liberarCupoDevolucion_vendidaInsuficiente_throws() {
        LiberarCupoRequestDTO req = new LiberarCupoRequestDTO();
        req.setIdSector(ID_SECTOR);
        req.setCantidad(10);
        CupoSector cupo = new CupoSector();
        cupo.setCantidadVendida(5);
        cupo.setCantidadDisponible(50);
        cupo.setCapacidadTotal(100);
        cupo.setCantidadReservada(0);
        when(cupoSectorRepository.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR))
                .thenReturn(Optional.of(cupo));
        assertThrows(BusinessConflictException.class,
                () -> service.liberarCupoDevolucion(ID_EVENTO, req));
    }

    @Test
    void liberarCupoDevolucion_ok_devuelveDisponibilidad() {
        LiberarCupoRequestDTO req = new LiberarCupoRequestDTO();
        req.setIdSector(ID_SECTOR);
        req.setCantidad(5);
        CupoSector cupo = new CupoSector();
        cupo.setCantidadVendida(10);
        cupo.setCantidadDisponible(50);
        cupo.setCapacidadTotal(100);
        cupo.setCantidadReservada(0);
        when(cupoSectorRepository.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR))
                .thenReturn(Optional.of(cupo));
        when(cupoSectorRepository.findByIdEvento(ID_EVENTO)).thenReturn(List.of(cupo));
        DisponibilidadResponseDTO dto = new DisponibilidadResponseDTO();
        dto.setIdEvento(ID_EVENTO);
        when(ventaMapper.toDisponibilidad(ID_EVENTO, List.of(cupo))).thenReturn(dto);

        service.liberarCupoDevolucion(ID_EVENTO, req);

        assertEquals(5, cupo.getCantidadVendida());
        assertEquals(55, cupo.getCantidadDisponible());
        verify(cupoSectorRepository).save(cupo);
    }

    // ---------- crearReserva ----------

    @Test
    void crearReserva_idempotencyKeyCacheada_reusa() {
        ReservaCreateRequestDTO req = new ReservaCreateRequestDTO(
                ID_EVENTO, new ArrayList<>());
        when(stockCache.getIdempotentCompraId("reserva:key-1"))
                .thenReturn(Optional.of(ID_RESERVA));
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente(ID_CLIENTE);
        reserva.setEstado("PENDIENTE");
        when(reservaRepository.findById(ID_RESERVA)).thenReturn(Optional.of(reserva));
        when(detalleReservaRepository.findByReservaIdReserva(ID_RESERVA)).thenReturn(new ArrayList<>());
        when(ventaMapper.toReserva(reserva, new ArrayList<>())).thenReturn(new ReservaResponseDTO());

        service.crearReserva(ID_CLIENTE, req, "key-1");

        verify(ventaMapper).toReserva(reserva, new ArrayList<>());
    }

    @Test
    void crearReserva_eventoNoPublicado_throws() {
        ReservaCreateRequestDTO req = new ReservaCreateRequestDTO(
                ID_EVENTO, new ArrayList<>());
        EventoDetalleFeignDTO evento = new EventoDetalleFeignDTO();
        evento.setEstado("BORRADOR");
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(evento);

        assertThrows(BusinessConflictException.class,
                () -> service.crearReserva(ID_CLIENTE, req, null));
    }

    @Test
    void crearReserva_sinPrecioParaSector_throws() {
        ReservaCreateRequestDTO.Item it = crearItem(ID_SECTOR, 1);
        List<ReservaCreateRequestDTO.Item> items = new ArrayList<>();
        items.add(it);
        ReservaCreateRequestDTO req = new ReservaCreateRequestDTO(ID_EVENTO, items);

        EventoDetalleFeignDTO evento = new EventoDetalleFeignDTO();
        evento.setEstado("PUBLICADO");
        List<SectorFeignDTO> sectores = new ArrayList<>();
        SectorFeignDTO s = new SectorFeignDTO();
        s.setId("otro-sector");
        s.setPrecio(BigDecimal.TEN);
        sectores.add(s);
        evento.setSectores(sectores);
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(evento);

        assertThrows(IllegalArgumentException.class,
                () -> service.crearReserva(ID_CLIENTE, req, null));
    }

    @Test
    void crearReserva_cupoInsuficiente_throws() {
        ReservaCreateRequestDTO req = new ReservaCreateRequestDTO(
                ID_EVENTO, List.of(crearItem(ID_SECTOR, 10)));

        EventoDetalleFeignDTO evento = new EventoDetalleFeignDTO();
        evento.setEstado("PUBLICADO");
        List<SectorFeignDTO> sectores = new ArrayList<>();
        SectorFeignDTO s = new SectorFeignDTO();
        s.setId(ID_SECTOR);
        s.setPrecio(BigDecimal.valueOf(95000));
        sectores.add(s);
        evento.setSectores(sectores);
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(evento);

        CupoSector cupo = new CupoSector();
        cupo.setCantidadDisponible(3);
        cupo.setCantidadReservada(0);
        cupo.setCantidadVendida(0);
        cupo.setCapacidadTotal(100);
        when(cupoSectorRepository.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR))
                .thenReturn(Optional.of(cupo));

        assertThrows(BusinessConflictException.class,
                () -> service.crearReserva(ID_CLIENTE, req, null));
    }

    @Test
    void crearReserva_limiteEntradas_throws() {
        ReservaCreateRequestDTO req = new ReservaCreateRequestDTO(
                ID_EVENTO, List.of(crearItem(ID_SECTOR, 1)));

        EventoDetalleFeignDTO evento = new EventoDetalleFeignDTO();
        evento.setEstado("PUBLICADO");
        List<SectorFeignDTO> sectores = new ArrayList<>();
        SectorFeignDTO s = new SectorFeignDTO();
        s.setId(ID_SECTOR);
        s.setPrecio(BigDecimal.valueOf(95000));
        sectores.add(s);
        evento.setSectores(sectores);
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(evento);

        CupoSector cupo = new CupoSector();
        cupo.setCantidadDisponible(100);
        cupo.setCantidadReservada(0);
        cupo.setCantidadVendida(0);
        cupo.setCapacidadTotal(100);
        when(cupoSectorRepository.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR))
                .thenReturn(Optional.of(cupo));
        when(compraRepository.sumCantidadByClienteEventoAndEstados(ID_CLIENTE, ID_EVENTO,
                List.of("APROBADA", "PENDIENTE_PAGO"))).thenReturn(6L);

        assertThrows(BusinessConflictException.class,
                () -> service.crearReserva(ID_CLIENTE, req, null));
    }

    @Test
    void crearReserva_ok_guardaYMapea() {
        ReservaCreateRequestDTO req = new ReservaCreateRequestDTO(
                ID_EVENTO, List.of(crearItem(ID_SECTOR, 2)));

        EventoDetalleFeignDTO evento = new EventoDetalleFeignDTO();
        evento.setEstado("PUBLICADO");
        List<SectorFeignDTO> sectores = new ArrayList<>();
        SectorFeignDTO s = new SectorFeignDTO();
        s.setId(ID_SECTOR);
        s.setPrecio(BigDecimal.valueOf(95000));
        sectores.add(s);
        evento.setSectores(sectores);
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(evento);

        CupoSector cupo = new CupoSector();
        cupo.setCantidadDisponible(100);
        cupo.setCantidadReservada(0);
        cupo.setCantidadVendida(0);
        cupo.setCapacidadTotal(100);
        when(cupoSectorRepository.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR))
                .thenReturn(Optional.of(cupo));
        when(compraRepository.sumCantidadByClienteEventoAndEstados(anyString(), anyString(),
                anyList())).thenReturn(0L);
        when(cupoSectorRepository.save(any(CupoSector.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detalleReservaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(stockCache).putReservaHold(anyString(), anyString(), any(Duration.class));
        when(ventaMapper.toReserva(any(Reserva.class), anyList()))
                .thenReturn(new ReservaResponseDTO());

        ReservaResponseDTO res = service.crearReserva(ID_CLIENTE, req, null);

        assertNotNull(res);
        verify(stockCache).putReservaHold(anyString(), eq(ID_CLIENTE), eq(Duration.ofMinutes(10)));
    }

    // ---------- obtenerReserva ----------

    @Test
    void obtenerReserva_ok_devuelveDto() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente(ID_CLIENTE);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("PENDIENTE");
        when(reservaRepository.findById(ID_RESERVA)).thenReturn(Optional.of(reserva));
        when(detalleReservaRepository.findByReservaIdReserva(ID_RESERVA)).thenReturn(new ArrayList<>());
        when(ventaMapper.toReserva(reserva, new ArrayList<>())).thenReturn(new ReservaResponseDTO());

        ReservaResponseDTO res = service.obtenerReserva(ID_CLIENTE, ID_RESERVA);

        assertNotNull(res);
    }

    @Test
    void obtenerReserva_noExiste_throws() {
        when(reservaRepository.findById(ID_RESERVA)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerReserva(ID_CLIENTE, ID_RESERVA));
    }

    @Test
    void obtenerReserva_noEsDelCliente_throws() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente("otro-cliente");
        when(reservaRepository.findById(ID_RESERVA)).thenReturn(Optional.of(reserva));
        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerReserva(ID_CLIENTE, ID_RESERVA));
    }

    // ---------- confirmarReserva ----------

    @Test
    void confirmarReserva_ok() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente(ID_CLIENTE);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("PENDIENTE");
        reserva.setFechaExpiracion(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        when(reservaRepository.findById(ID_RESERVA)).thenReturn(Optional.of(reserva));
        when(detalleReservaRepository.findByReservaIdReserva(ID_RESERVA)).thenReturn(new ArrayList<>());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ventaMapper.toReserva(any(Reserva.class), anyList())).thenReturn(new ReservaResponseDTO());

        service.confirmarReserva(ID_CLIENTE, ID_RESERVA);

        assertEquals("CONFIRMADA", reserva.getEstado());
        verify(reservaRepository).save(reserva);
    }

    @Test
    void confirmarReserva_yaConfirmada_devuelve() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente(ID_CLIENTE);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("CONFIRMADA");
        when(reservaRepository.findById(ID_RESERVA)).thenReturn(Optional.of(reserva));
        when(ventaMapper.toReserva(reserva, new ArrayList<>())).thenReturn(new ReservaResponseDTO());

        service.confirmarReserva(ID_CLIENTE, ID_RESERVA);

        verify(reservaRepository, never()).save(any());
    }

    @Test
    void confirmarReserva_estadoNoPermitido_throws() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente(ID_CLIENTE);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("CANCELADA");
        when(reservaRepository.findById(ID_RESERVA)).thenReturn(Optional.of(reserva));
        assertThrows(BusinessConflictException.class,
                () -> service.confirmarReserva(ID_CLIENTE, ID_RESERVA));
    }

    // ---------- cancelarReserva ----------

    @Test
    void cancelarReserva_ok() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente(ID_CLIENTE);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("PENDIENTE");
        when(reservaRepository.findById(ID_RESERVA)).thenReturn(Optional.of(reserva));
        when(compraRepository.findByReservaIdReserva(ID_RESERVA)).thenReturn(Optional.empty());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detalleReservaRepository.findByReservaIdReserva(ID_RESERVA)).thenReturn(new ArrayList<>());
        when(cupoSectorRepository.findByEventoAndSectorForUpdate(anyString(), anyString()))
                .thenReturn(Optional.of(new CupoSector()));
        when(cupoSectorRepository.findByIdEvento(ID_EVENTO)).thenReturn(new ArrayList<>());
        when(ventaMapper.toReserva(reserva, new ArrayList<>())).thenReturn(new ReservaResponseDTO());

        service.cancelarReserva(ID_CLIENTE, ID_RESERVA);

        assertEquals("CANCELADA", reserva.getEstado());
        verify(stockCache).releaseReservaHold(ID_RESERVA);
    }

    @Test
    void cancelarReserva_conCompraAprobada_throws() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente(ID_CLIENTE);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("PENDIENTE");
        Compra compra = new Compra();
        compra.setEstado("APROBADA");
        when(reservaRepository.findById(ID_RESERVA)).thenReturn(Optional.of(reserva));
        when(compraRepository.findByReservaIdReserva(ID_RESERVA)).thenReturn(Optional.of(compra));
        assertThrows(BusinessConflictException.class,
                () -> service.cancelarReserva(ID_CLIENTE, ID_RESERVA));
    }

    // ---------- renovarReserva ----------

    @Test
    void renovarReserva_ok() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente(ID_CLIENTE);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("PENDIENTE");
        reserva.setFechaExpiracion(LocalDateTime.now().plusDays(1));
        when(reservaRepository.findById(ID_RESERVA)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ventaMapper.toReserva(reserva, new ArrayList<>())).thenReturn(new ReservaResponseDTO());

        service.renovarReserva(ID_CLIENTE, ID_RESERVA);

        verify(stockCache).renewReservaHold(ID_RESERVA, Duration.ofMinutes(10));
    }

    // ---------- crearCompra ----------

    @Test
    void crearCompra_idempotencyKeyNulo_throws() {
        CompraCreateRequestDTO req = new CompraCreateRequestDTO(ID_RESERVA);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCompra(ID_CLIENTE, req, null));
    }

    @Test
    void crearCompra_idempotencyKeyCacheada_reusa() {
        CompraCreateRequestDTO req = new CompraCreateRequestDTO(ID_RESERVA);
        when(stockCache.getIdempotentCompraId("key-1")).thenReturn(Optional.of(ID_COMPRA));
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente(ID_CLIENTE);
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        CompraResponseDTO dto = new CompraResponseDTO();
        dto.setId(ID_COMPRA);
        when(ventaMapper.toCompra(compra)).thenReturn(dto);

        CompraResponseDTO res = service.crearCompra(ID_CLIENTE, req, "key-1");

        assertNotNull(res);
        assertEquals(ID_COMPRA, res.getId());
    }

    @Test
    void crearCompra_reservaConCompra_throws() {
        CompraCreateRequestDTO req = new CompraCreateRequestDTO(ID_RESERVA);
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente(ID_CLIENTE);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("CONFIRMADA");
        when(reservaRepository.findById(ID_RESERVA)).thenReturn(Optional.of(reserva));
        Compra compraExistente = new Compra();
        compraExistente.setEstado("PENDIENTE_PAGO");
        when(compraRepository.findByReservaIdReserva(ID_RESERVA)).thenReturn(Optional.of(compraExistente));

        assertThrows(BusinessConflictException.class,
                () -> service.crearCompra(ID_CLIENTE, req, "key-1"));
    }

    @Test
    void crearCompra_ok_guardaYMapea() {
        CompraCreateRequestDTO req = new CompraCreateRequestDTO(ID_RESERVA);
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente(ID_CLIENTE);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("CONFIRMADA");
        reserva.setTotal(BigDecimal.valueOf(190000));
        when(reservaRepository.findById(ID_RESERVA)).thenReturn(Optional.of(reserva));
        when(compraRepository.findByReservaIdReserva(ID_RESERVA)).thenReturn(Optional.empty());
        when(compraRepository.save(any(Compra.class))).thenAnswer(inv -> {
            Compra c = inv.getArgument(0);
            c.setIdCompra(ID_COMPRA);
            return c;
        });
        doNothing().when(stockCache).putIdempotentCompraId("key-1", ID_COMPRA, Duration.ofHours(24));
        when(ventaMapper.toCompra(any(Compra.class))).thenReturn(new CompraResponseDTO());

        CompraResponseDTO res = service.crearCompra(ID_CLIENTE, req, "key-1");

        assertNotNull(res);
        verify(stockCache).putIdempotentCompraId("key-1", ID_COMPRA, Duration.ofHours(24));
    }

    // ---------- obtenerCompra ----------

    @Test
    void obtenerCompra_ok_devuelveDto() {
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente(ID_CLIENTE);
        compra.setEstado("PENDIENTE_PAGO");
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        when(ventaMapper.toCompra(compra)).thenReturn(new CompraResponseDTO());

        CompraResponseDTO res = service.obtenerCompra(ID_CLIENTE, ID_COMPRA);

        assertNotNull(res);
    }

    @Test
    void obtenerCompra_noEsDelCliente_throws() {
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente("otro-cliente");
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerCompra(ID_CLIENTE, ID_COMPRA));
    }

    // ---------- listarCompras ----------

    @Test
    void listarCompras_devuelveLista() {
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente(ID_CLIENTE);
        when(compraRepository.findByIdClienteOrderByFechaCompraDesc(ID_CLIENTE))
                .thenReturn(List.of(compra));
        when(ventaMapper.toCompra(compra)).thenReturn(new CompraResponseDTO());

        List<CompraResponseDTO> res = service.listarCompras(ID_CLIENTE);

        assertEquals(1, res.size());
    }

    // ---------- iniciarPago ----------

    @Test
    void iniciarPago_compraNoPendiente_throws() {
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente(ID_CLIENTE);
        compra.setEstado("APROBADA");
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        assertThrows(BusinessConflictException.class,
                () -> service.iniciarPago(ID_CLIENTE, ID_COMPRA, new PagoCreateRequestDTO(null, null)));
    }

    @Test
    void iniciarPago_ok_aprueba() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setEstado("CONFIRMADA");
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente(ID_CLIENTE);
        compra.setEstado("PENDIENTE_PAGO");
        compra.setReserva(reserva);
        compra.setTotal(BigDecimal.valueOf(190000));
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emisionEntradasService).marcarPendienteYAgendar(any(Compra.class));
        when(ventaMapper.toPago(any(Pago.class))).thenReturn(new PagoResponseDTO());

        service.iniciarPago(ID_CLIENTE, ID_COMPRA, new PagoCreateRequestDTO("APROBAR", "MOCK_PASARELA"));

        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    void iniciarPago_resultadoInvalido_throws() {
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente(ID_CLIENTE);
        compra.setEstado("PENDIENTE_PAGO");
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        PagoCreateRequestDTO req = new PagoCreateRequestDTO("INVALIDO", null);
        assertThrows(IllegalArgumentException.class,
                () -> service.iniciarPago(ID_CLIENTE, ID_COMPRA, req));
    }

    // ---------- listarPagos ----------

    @Test
    void listarPagos_devuelveLista() {
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente(ID_CLIENTE);
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        Pago pago = new Pago();
        pago.setIdPago(ID_PAGO);
        pago.setCompra(compra);
        when(pagoRepository.findByCompraIdCompraOrderByFechaPagoDesc(ID_COMPRA)).thenReturn(List.of(pago));
        when(ventaMapper.toPago(pago)).thenReturn(new PagoResponseDTO());

        List<PagoResponseDTO> res = service.listarPagos(ID_CLIENTE, ID_COMPRA);

        assertEquals(1, res.size());
    }

    // ---------- reintentarPago ----------

    @Test
    void reintentarPago_pagoNoEncontrado_throws() {
        when(pagoRepository.findById(ID_PAGO)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.reintentarPago(ID_CLIENTE, ID_PAGO, new PagoCreateRequestDTO(null, null)));
    }

    @Test
    void reintentarPago_noEsDelCliente_throws() {
        Pago pago = new Pago();
        pago.setIdPago(ID_PAGO);
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente("otro-cliente");
        pago.setCompra(compra);
        when(pagoRepository.findById(ID_PAGO)).thenReturn(Optional.of(pago));
        assertThrows(ResourceNotFoundException.class,
                () -> service.reintentarPago(ID_CLIENTE, ID_PAGO, new PagoCreateRequestDTO(null, null)));
    }

    @Test
    void reintentarPago_pagoNoRechazado_throws() {
        Pago pago = new Pago();
        pago.setIdPago(ID_PAGO);
        pago.setEstado("APROBADO");
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente(ID_CLIENTE);
        compra.setEstado("PENDIENTE_PAGO");
        pago.setCompra(compra);
        when(pagoRepository.findById(ID_PAGO)).thenReturn(Optional.of(pago));
        assertThrows(BusinessConflictException.class,
                () -> service.reintentarPago(ID_CLIENTE, ID_PAGO, new PagoCreateRequestDTO(null, null)));
    }

    @Test
    void reintentarPago_compraNoReintentable_throws() {
        Pago pago = new Pago();
        pago.setIdPago(ID_PAGO);
        pago.setEstado("RECHAZADO");
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente(ID_CLIENTE);
        compra.setEstado("APROBADA");
        pago.setCompra(compra);
        when(pagoRepository.findById(ID_PAGO)).thenReturn(Optional.of(pago));
        assertThrows(BusinessConflictException.class,
                () -> service.reintentarPago(ID_CLIENTE, ID_PAGO, new PagoCreateRequestDTO(null, null)));
    }

    @Test
    void reintentarPago_compraRechazadaReservaNoActiva_throws() {
        Pago pago = new Pago();
        pago.setIdPago(ID_PAGO);
        pago.setEstado("RECHAZADO");
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente(ID_CLIENTE);
        compra.setEstado("RECHAZADA");
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setEstado("EXPIRADA");
        compra.setReserva(reserva);
        pago.setCompra(compra);
        when(pagoRepository.findById(ID_PAGO)).thenReturn(Optional.of(pago));
        assertThrows(BusinessConflictException.class,
                () -> service.reintentarPago(ID_CLIENTE, ID_PAGO, new PagoCreateRequestDTO(null, null)));
    }

    @Test
    void reintentarPago_ok() {
        Pago pago = new Pago();
        pago.setIdPago(ID_PAGO);
        pago.setEstado("RECHAZADO");
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setIdCliente(ID_CLIENTE);
        compra.setEstado("RECHAZADA");
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setEstado("PENDIENTE");
        compra.setReserva(reserva);
        pago.setCompra(compra);
        when(pagoRepository.findById(ID_PAGO)).thenReturn(Optional.of(pago));
        when(compraRepository.save(any(Compra.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emisionEntradasService).marcarPendienteYAgendar(any(Compra.class));
        when(ventaMapper.toPago(any(Pago.class))).thenReturn(new PagoResponseDTO());

        service.reintentarPago(ID_CLIENTE, ID_PAGO, new PagoCreateRequestDTO("APROBAR", null));

        assertEquals("APROBADA", compra.getEstado());
        verify(pagoRepository).save(any(Pago.class));
    }

    // ---------- expirarReservasVencidas ----------

    @Test
    void expirarReservasVencidas_sinVencidas_devuelveCero() {
        when(reservaRepository.findExpiradas(anyList(), any(LocalDateTime.class))).thenReturn(new ArrayList<>());

        int n = service.expirarReservasVencidas();

        assertEquals(0, n);
    }

    @Test
    void expirarReservasVencidas_conVencida_expiraYLibera() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdCliente(ID_CLIENTE);
        reserva.setIdEvento(ID_EVENTO);
        reserva.setEstado("PENDIENTE");
        when(reservaRepository.findExpiradas(anyList(), any(LocalDateTime.class))).thenReturn(List.of(reserva));
        when(compraRepository.findByReservaIdReserva(ID_RESERVA)).thenReturn(Optional.empty());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detalleReservaRepository.findByReservaIdReserva(ID_RESERVA)).thenReturn(new ArrayList<>());
        when(cupoSectorRepository.findByEventoAndSectorForUpdate(anyString(), anyString()))
                .thenReturn(Optional.of(new CupoSector()));
        when(cupoSectorRepository.findByIdEvento(ID_EVENTO)).thenReturn(new ArrayList<>());
        when(ventaMapper.toDisponibilidad(anyString(), anyList())).thenReturn(new DisponibilidadResponseDTO());

        int n = service.expirarReservasVencidas();

        assertEquals(1, n);
        assertEquals("EXPIRADA", reserva.getEstado());
        verify(stockCache).releaseReservaHold(ID_RESERVA);
    }
}
