package entradas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import entradas.clients.EventosClient;
import entradas.clients.EventosClient.EventoDetalleFeignDTO;
import entradas.clients.EventosClient.LugarFeignDTO;
import entradas.clients.EventosClient.SectorFeignDTO;
import entradas.clients.VentasClient;
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
import feign.FeignException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntradaServiceImplTest {

    @Mock
    private EntradaRepository entradaRepository;

    @Mock
    private DevolucionRepository devolucionRepository;

    @Mock
    private EntradaMapper entradaMapper;

    @Mock
    private EventosClient eventosClient;

    @Mock
    private VentasClient ventasClient;

    private EntradaServiceImpl service;

    private static final String ID_ENTRADA = "e0000000-0000-4000-8000-000000000001";
    private static final String ID_DEVOLUCION = "d0000000-0000-4000-8000-000000000001";
    private static final String ID_CLIENTE = "b0000000-0000-4000-8000-000000000001";
    private static final String ID_OTRO_CLIENTE = "b0000000-0000-4000-8000-000000000002";
    private static final String ID_EVENTO = "ev000000-0000-4000-8000-000000000001";
    private static final String ID_SECTOR = "s0000000-0000-4000-8000-000000000001";
    private static final String ID_COMPRA = "c0000000-0000-4000-8000-000000000001";

    @BeforeEach
    void setUp() {
        reset(entradaRepository, devolucionRepository, entradaMapper, eventosClient, ventasClient);
        service = new EntradaServiceImpl(
                entradaRepository, devolucionRepository, entradaMapper,
                eventosClient, ventasClient, 72L);
    }

    private Entrada entradaVendida() {
        Entrada e = new Entrada();
        e.setIdEntrada(ID_ENTRADA);
        e.setIdCompra(ID_COMPRA);
        e.setIdCliente(ID_CLIENTE);
        e.setIdEvento(ID_EVENTO);
        e.setIdSector(ID_SECTOR);
        e.setEstado("VENDIDA");
        e.setPrecio(BigDecimal.valueOf(95000));
        e.setFechaEmision(LocalDateTime.of(2026, 3, 1, 12, 0));
        e.setFechaValidacion(null);
        e.setCodigoQr("QR-SEED-001");
        return e;
    }

    private Entrada entradaUsada() {
        Entrada e = entradaVendida();
        e.setEstado("USADA");
        e.setFechaValidacion(LocalDateTime.of(2026, 3, 5, 10, 0));
        return e;
    }

    private EmitirEntradasRequestDTO.Item item() {
        return new EmitirEntradasRequestDTO.Item(
                ID_SECTOR, Integer.valueOf(2), BigDecimal.valueOf(95000));
    }

    // ---------- emitir ----------

    @Test
    void emitir_itemsVacios_throws() {
        EmitirEntradasRequestDTO req = new EmitirEntradasRequestDTO(
                ID_COMPRA, ID_CLIENTE, ID_EVENTO, new ArrayList<>());

        assertThrows(IllegalArgumentException.class,
                () -> service.emitir(req));
    }

    @Test
    void emitir_cantidadCero_throws() {
        EmitirEntradasRequestDTO.Item it = new EmitirEntradasRequestDTO.Item(
                ID_SECTOR, Integer.valueOf(0), BigDecimal.valueOf(95000));
        EmitirEntradasRequestDTO req = new EmitirEntradasRequestDTO(
                ID_COMPRA, ID_CLIENTE, ID_EVENTO, List.of(it));

        assertThrows(IllegalArgumentException.class,
                () -> service.emitir(req));
    }

    @Test
    void emitir_compraYaTieneEntradas_devuelveExistentes() {
        EntradaResponseDTO dto = new EntradaResponseDTO();
        dto.setId(ID_ENTRADA);
        when(entradaRepository.findByIdCompra(ID_COMPRA)).thenReturn(List.of(entradaVendida()));
        when(entradaMapper.toEntrada(any(Entrada.class))).thenReturn(dto);
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(eventoActivo());

        List<EntradaResponseDTO> res = service.emitir(
                new EmitirEntradasRequestDTO(ID_COMPRA, ID_CLIENTE, ID_EVENTO, List.of(item())));

        assertEquals(1, res.size());
        verify(entradaRepository, never()).saveAll(anyList());
    }

    @Test
    void emitir_ok_creaEntradas() {
        when(entradaRepository.findByIdCompra(ID_COMPRA)).thenReturn(new ArrayList<>());
        when(entradaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(entradaMapper.toEntrada(any(Entrada.class))).thenReturn(new EntradaResponseDTO());
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(eventoActivo());

        List<EntradaResponseDTO> res = service.emitir(
                new EmitirEntradasRequestDTO(ID_COMPRA, ID_CLIENTE, ID_EVENTO, List.of(item())));

        assertEquals(2, res.size());
        verify(entradaRepository).saveAll(anyList());
    }

    @Test
    void emitir_eventoNoEncontrado_entradaSigueCreada() {
        when(entradaRepository.findByIdCompra(ID_COMPRA)).thenReturn(new ArrayList<>());
        when(entradaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(entradaMapper.toEntrada(any(Entrada.class))).thenReturn(new EntradaResponseDTO());
        when(eventosClient.getEvento(ID_EVENTO))
                .thenThrow(new RuntimeException("not found"));

        List<EntradaResponseDTO> res = service.emitir(
                new EmitirEntradasRequestDTO(ID_COMPRA, ID_CLIENTE, ID_EVENTO, List.of(item())));

        assertEquals(2, res.size());
        verify(entradaRepository).saveAll(anyList());
    }

    // ---------- listarMias ----------

    @Test
    void listarMias_devuelveLista() {
        when(entradaRepository.findByIdClienteOrderByFechaEmisionDesc(ID_CLIENTE))
                .thenReturn(List.of(entradaVendida()));
        when(entradaMapper.toEntrada(any(Entrada.class))).thenReturn(new EntradaResponseDTO());
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(eventoActivo());

        List<EntradaResponseDTO> res = service.listarMias(ID_CLIENTE);

        assertEquals(1, res.size());
    }

    // ---------- obtener ----------

    @Test
    void obtener_dueño_devuelveDto() {
        EntradaResponseDTO dto = new EntradaResponseDTO();
        dto.setId(ID_ENTRADA);
        when(entradaRepository.findById(ID_ENTRADA)).thenReturn(Optional.of(entradaVendida()));
        when(entradaMapper.toEntrada(any(Entrada.class))).thenReturn(dto);
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(eventoActivo());

        EntradaResponseDTO res = service.obtener(ID_CLIENTE, ID_ENTRADA, false);

        assertNotNull(res);
        assertEquals(ID_ENTRADA, res.getId());
    }

    @Test
    void obtener_admin_devuelveDto() {
        EntradaResponseDTO dto = new EntradaResponseDTO();
        dto.setId(ID_ENTRADA);
        when(entradaRepository.findById(ID_ENTRADA)).thenReturn(Optional.of(entradaVendida()));
        when(entradaMapper.toEntrada(any(Entrada.class))).thenReturn(dto);
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(eventoActivo());

        EntradaResponseDTO res = service.obtener(ID_OTRO_CLIENTE, ID_ENTRADA, true);

        assertNotNull(res);
    }

    @Test
    void obtener_noEsDelCliente_throws() {
        when(entradaRepository.findById(ID_ENTRADA)).thenReturn(Optional.of(entradaVendida()));

        assertThrows(ResourceNotFoundException.class,
                () -> service.obtener(ID_OTRO_CLIENTE, ID_ENTRADA, false));
    }

    @Test
    void obtener_noEncontrado_throws() {
        when(entradaRepository.findById(ID_ENTRADA)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.obtener(ID_CLIENTE, ID_ENTRADA, false));
    }

    // ---------- qrMia ----------

    @Test
    void qrMia_vendidaEventoActivo_devuelveQr() {
        QrResponseDTO dto = new QrResponseDTO();
        dto.setId(ID_ENTRADA);
        when(entradaRepository.findById(ID_ENTRADA)).thenReturn(Optional.of(entradaVendida()));
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(eventoActivo());
        when(entradaMapper.toQr(any(Entrada.class))).thenReturn(dto);

        QrResponseDTO res = service.qrMia(ID_CLIENTE, ID_ENTRADA);

        assertNotNull(res);
    }

    @Test
    void qrMia_estadoNoVendida_throws() {
        when(entradaRepository.findById(ID_ENTRADA)).thenReturn(Optional.of(entradaUsada()));

        assertThrows(BusinessConflictException.class,
                () -> service.qrMia(ID_CLIENTE, ID_ENTRADA));
    }

    @Test
    void qrMia_eventoCancelado_throws() {
        when(entradaRepository.findById(ID_ENTRADA)).thenReturn(Optional.of(entradaVendida()));
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(eventoCancelado());

        assertThrows(BusinessConflictException.class,
                () -> service.qrMia(ID_CLIENTE, ID_ENTRADA));
    }

    @Test
    void qrMia_noEsDelCliente_throws() {
        when(entradaRepository.findById(ID_ENTRADA)).thenReturn(Optional.of(entradaVendida()));

        assertThrows(ResourceNotFoundException.class,
                () -> service.qrMia(ID_OTRO_CLIENTE, ID_ENTRADA));
    }

    // ---------- solicitarDevolucion ----------

    @Test
    void solicitarDevolucion_ok_devuelveDto() {
        Entrada e = entradaVendida();
        e.setFechaEmision(LocalDateTime.now().minusHours(1));
        DevolucionResponseDTO dto = new DevolucionResponseDTO();
        dto.setId(ID_DEVOLUCION);
        when(entradaRepository.findById(ID_ENTRADA)).thenReturn(Optional.of(e));
        when(devolucionRepository.findByEntradaIdEntrada(ID_ENTRADA)).thenReturn(new ArrayList<>());
        when(devolucionRepository.save(any(Devolucion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(entradaMapper.toDevolucion(any(Devolucion.class))).thenReturn(dto);

        DevolucionResponseDTO res = service.solicitarDevolucion(
                ID_CLIENTE, ID_ENTRADA, new DevolucionCreateRequestDTO("motivo"));

        assertNotNull(res);
        verify(devolucionRepository).save(any(Devolucion.class));
    }

    @Test
    void solicitarDevolucion_estadoNoVendida_throws() {
        when(entradaRepository.findById(ID_ENTRADA)).thenReturn(Optional.of(entradaUsada()));

        assertThrows(BusinessConflictException.class,
                () -> service.solicitarDevolucion(ID_CLIENTE, ID_ENTRADA, null));
    }

    @Test
    void solicitarDevolucion_plazoVencido_throws() {
        Entrada e = entradaVendida();
        e.setFechaEmision(LocalDateTime.now().minusHours(100));
        when(entradaRepository.findById(ID_ENTRADA)).thenReturn(Optional.of(e));

        assertThrows(BusinessConflictException.class,
                () -> service.solicitarDevolucion(ID_CLIENTE, ID_ENTRADA, null));
    }

    @Test
    void solicitarDevolucion_yaExistePendiente_throws() {
        Entrada e = entradaVendida();
        e.setFechaEmision(LocalDateTime.now().minusHours(1));
        Devolucion pendiente = new Devolucion();
        pendiente.setEstado("PENDIENTE");
        when(entradaRepository.findById(ID_ENTRADA)).thenReturn(Optional.of(e));
        when(devolucionRepository.findByEntradaIdEntrada(ID_ENTRADA)).thenReturn(List.of(pendiente));

        assertThrows(BusinessConflictException.class,
                () -> service.solicitarDevolucion(ID_CLIENTE, ID_ENTRADA, null));
    }

    @Test
    void solicitarDevolucion_noEsDelCliente_throws() {
        when(entradaRepository.findById(ID_ENTRADA)).thenReturn(Optional.of(entradaVendida()));

        assertThrows(ResourceNotFoundException.class,
                () -> service.solicitarDevolucion(ID_OTRO_CLIENTE, ID_ENTRADA, null));
    }

    // ---------- obtenerDevolucion ----------

    @Test
    void obtenerDevolucion_dueño_devuelveDto() {
        Devolucion d = new Devolucion();
        d.setIdDevolucion(ID_DEVOLUCION);
        d.setEntrada(entradaVendida());
        d.setEstado("PENDIENTE");
        DevolucionResponseDTO dto = new DevolucionResponseDTO();
        dto.setId(ID_DEVOLUCION);
        when(devolucionRepository.findById(ID_DEVOLUCION)).thenReturn(Optional.of(d));
        when(entradaMapper.toDevolucion(d)).thenReturn(dto);

        DevolucionResponseDTO res = service.obtenerDevolucion(ID_CLIENTE, ID_DEVOLUCION, false);

        assertNotNull(res);
    }

    @Test
    void obtenerDevolucion_admin_devuelveDto() {
        Devolucion d = new Devolucion();
        d.setIdDevolucion(ID_DEVOLUCION);
        d.setEntrada(entradaVendida());
        d.setEstado("PENDIENTE");
        DevolucionResponseDTO dto = new DevolucionResponseDTO();
        dto.setId(ID_DEVOLUCION);
        when(devolucionRepository.findById(ID_DEVOLUCION)).thenReturn(Optional.of(d));
        when(entradaMapper.toDevolucion(d)).thenReturn(dto);

        DevolucionResponseDTO res = service.obtenerDevolucion(ID_OTRO_CLIENTE, ID_DEVOLUCION, true);

        assertNotNull(res);
    }

    @Test
    void obtenerDevolucion_noEsDelCliente_throws() {
        Devolucion d = new Devolucion();
        d.setIdDevolucion(ID_DEVOLUCION);
        d.setEntrada(entradaVendida());
        d.setEstado("PENDIENTE");
        when(devolucionRepository.findById(ID_DEVOLUCION)).thenReturn(Optional.of(d));

        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerDevolucion(ID_OTRO_CLIENTE, ID_DEVOLUCION, false));
    }

    @Test
    void obtenerDevolucion_noEncontrado_throws() {
        when(devolucionRepository.findById(ID_DEVOLUCION)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerDevolucion(ID_CLIENTE, ID_DEVOLUCION, false));
    }

    // ---------- listarDevolucionesPendientes ----------

    @Test
    void listarDevolucionesPendientes_devuelveLista() {
        Devolucion d = new Devolucion();
        d.setIdDevolucion(ID_DEVOLUCION);
        d.setEstado("PENDIENTE");
        when(devolucionRepository.findByEstadoOrderByFechaSolicitudAsc("PENDIENTE"))
                .thenReturn(List.of(d));
        when(entradaMapper.toDevolucion(d)).thenReturn(new DevolucionResponseDTO());

        List<DevolucionResponseDTO> res = service.listarDevolucionesPendientes();

        assertEquals(1, res.size());
    }

    // ---------- listarDevolucionesCliente ----------

    @Test
    void listarDevolucionesCliente_devuelveLista() {
        Devolucion d = new Devolucion();
        d.setIdDevolucion(ID_DEVOLUCION);
        d.setEstado("PENDIENTE");
        when(devolucionRepository.findByEntradaIdClienteOrderByFechaSolicitudDesc(ID_CLIENTE))
                .thenReturn(List.of(d));
        when(entradaMapper.toDevolucion(d)).thenReturn(new DevolucionResponseDTO());

        List<DevolucionResponseDTO> res = service.listarDevolucionesCliente(ID_CLIENTE);

        assertEquals(1, res.size());
    }

    // ---------- aprobarDevolucion ----------

    @Test
    void aprobarDevolucion_ok() {
        Devolucion d = new Devolucion();
        d.setIdDevolucion(ID_DEVOLUCION);
        d.setEntrada(entradaVendida());
        d.setEstado("PENDIENTE");
        when(devolucionRepository.findByIdWithEntrada(ID_DEVOLUCION)).thenReturn(Optional.of(d));
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(inv -> inv.getArgument(0));
        when(devolucionRepository.save(any(Devolucion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ventasClient.liberarCupo(anyString(), any())).thenReturn(null);
        when(entradaMapper.toDevolucion(d)).thenReturn(new DevolucionResponseDTO());

        DevolucionResponseDTO res = service.aprobarDevolucion(ID_DEVOLUCION);

        assertNotNull(res);
        assertEquals("DEVUELTA", d.getEntrada().getEstado());
        assertEquals("APROBADA", d.getEstado());
        verify(ventasClient).liberarCupo(eq(ID_EVENTO), any(VentasClient.LiberarCupoBody.class));
    }

    @Test
    void aprobarDevolucion_noEsPendiente_throws() {
        Devolucion d = new Devolucion();
        d.setIdDevolucion(ID_DEVOLUCION);
        d.setEstado("APROBADA");
        when(devolucionRepository.findByIdWithEntrada(ID_DEVOLUCION)).thenReturn(Optional.of(d));

        assertThrows(BusinessConflictException.class,
                () -> service.aprobarDevolucion(ID_DEVOLUCION));
    }

    @Test
    void aprobarDevolucion_liberarCupoFalla_throws() {
        Devolucion d = new Devolucion();
        d.setIdDevolucion(ID_DEVOLUCION);
        d.setEntrada(entradaVendida());
        d.setEstado("PENDIENTE");
        when(devolucionRepository.findByIdWithEntrada(ID_DEVOLUCION)).thenReturn(Optional.of(d));
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(inv -> inv.getArgument(0));
        when(devolucionRepository.save(any(Devolucion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ventasClient.liberarCupo(anyString(), any()))
                .thenThrow(new RuntimeException("error"));

        assertThrows(BusinessConflictException.class,
                () -> service.aprobarDevolucion(ID_DEVOLUCION));
    }

    // ---------- rechazarDevolucion ----------

    @Test
    void rechazarDevolucion_ok() {
        Devolucion d = new Devolucion();
        d.setIdDevolucion(ID_DEVOLUCION);
        d.setEstado("PENDIENTE");
        when(devolucionRepository.findByIdWithEntrada(ID_DEVOLUCION)).thenReturn(Optional.of(d));
        when(devolucionRepository.save(any(Devolucion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(entradaMapper.toDevolucion(d)).thenReturn(new DevolucionResponseDTO());

        DevolucionResponseDTO res = service.rechazarDevolucion(ID_DEVOLUCION);

        assertNotNull(res);
        assertEquals("RECHAZADA", d.getEstado());
    }

    @Test
    void rechazarDevolucion_noEsPendiente_throws() {
        Devolucion d = new Devolucion();
        d.setIdDevolucion(ID_DEVOLUCION);
        d.setEstado("APROBADA");
        when(devolucionRepository.findByIdWithEntrada(ID_DEVOLUCION)).thenReturn(Optional.of(d));

        assertThrows(BusinessConflictException.class,
                () -> service.rechazarDevolucion(ID_DEVOLUCION));
    }

    // ---------- reintegroPorCancelacionEvento ----------

    @Test
    void reintegroPorCancelacionEvento_conEntradas_devuelveCantidad() {
        Entrada e = entradaVendida();
        e.setIdEntrada(ID_ENTRADA);
        when(entradaRepository.findByIdEventoAndEstado(ID_EVENTO, "VENDIDA"))
                .thenReturn(List.of(e));
        when(devolucionRepository.findByEntradaIdEntrada(ID_ENTRADA)).thenReturn(new ArrayList<>());
        when(devolucionRepository.save(any(Devolucion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(entradaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int n = service.reintegroPorCancelacionEvento(ID_EVENTO);

        assertEquals(1, n);
        assertEquals("A_REINTEGRAR", e.getEstado());
    }

    @Test
    void reintegroPorCancelacionEvento_sinEntradas_devuelveCero() {
        when(entradaRepository.findByIdEventoAndEstado(ID_EVENTO, "VENDIDA"))
                .thenReturn(new ArrayList<>());

        int n = service.reintegroPorCancelacionEvento(ID_EVENTO);

        assertEquals(0, n);
    }

    @Test
    void reintegroPorCancelacionEvento_yaTieneTramite_noCreaDuplicado() {
        Entrada e = entradaVendida();
        e.setIdEntrada(ID_ENTRADA);
        Devolucion existente = new Devolucion();
        existente.setMotivo("Reintegro por cancelación del evento");
        when(entradaRepository.findByIdEventoAndEstado(ID_EVENTO, "VENDIDA")).thenReturn(List.of(e));
        when(devolucionRepository.findByEntradaIdEntrada(ID_ENTRADA)).thenReturn(List.of(existente));
        when(entradaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int n = service.reintegroPorCancelacionEvento(ID_EVENTO);

        assertEquals(1, n);
        verify(devolucionRepository, never()).save(any(Devolucion.class));
    }

    // ---------- puedeExponerQr ----------

    @Test
    void puedeExponerQr_vendidaEventoActivo_true() {
        Entrada e = entradaVendida();
        assertTrue(EntradaServiceImpl.puedeExponerQr(e, eventoActivo()));
    }

    @Test
    void puedeExponerQr_estadoNoVendida_false() {
        Entrada e = entradaUsada();
        assertTrue(!EntradaServiceImpl.puedeExponerQr(e, eventoActivo()));
    }

    @Test
    void puedeExponerQr_eventoCancelado_false() {
        Entrada e = entradaVendida();
        assertTrue(!EntradaServiceImpl.puedeExponerQr(e, eventoCancelado()));
    }

    @Test
    void puedeExponerQr_eventoNull_true() {
        Entrada e = entradaVendida();
        assertTrue(EntradaServiceImpl.puedeExponerQr(e, null));
    }

    @Test
    void puedeExponerQr_entradaNull_false() {
        assertTrue(!EntradaServiceImpl.puedeExponerQr(null, eventoActivo()));
    }

    // ---------- applyQrPolicy ----------

    @Test
    void applyQrPolicy_eventoCancelado_nulificaQr() throws Exception {
        EntradaResponseDTO dto = new EntradaResponseDTO();
        dto.setCodigoQR("QR-SEED-001");
        java.lang.reflect.Method m = EntradaServiceImpl.class.getDeclaredMethod(
                "applyQrPolicy", Entrada.class, EntradaResponseDTO.class,
                EventoDetalleFeignDTO.class);
        m.setAccessible(true);
        m.invoke(service, entradaVendida(), dto, eventoCancelado());

        assertNull(dto.getCodigoQR());
    }

    @Test
    void applyQrPolicy_eventoActivo_mantieneQr() throws Exception {
        EntradaResponseDTO dto = new EntradaResponseDTO();
        dto.setCodigoQR("QR-SEED-001");
        java.lang.reflect.Method m = EntradaServiceImpl.class.getDeclaredMethod(
                "applyQrPolicy", Entrada.class, EntradaResponseDTO.class,
                EventoDetalleFeignDTO.class);
        m.setAccessible(true);
        m.invoke(service, entradaVendida(), dto, eventoActivo());

        assertEquals("QR-SEED-001", dto.getCodigoQR());
    }

    // ---------- helpers ----------

    private EventoDetalleFeignDTO eventoActivo() {
        EventoDetalleFeignDTO ev = new EventoDetalleFeignDTO();
        ev.setEstado("PUBLICADO");
        ev.setNombre("Evento Activo");
        LugarFeignDTO lugar = new LugarFeignDTO();
        lugar.setNombre("Estadio");
        lugar.setCiudad("CABA");
        ev.setLugar(lugar);
        List<SectorFeignDTO> sectores = new ArrayList<>();
        SectorFeignDTO s = new SectorFeignDTO();
        s.setId(ID_SECTOR);
        s.setNombre("Campo");
        sectores.add(s);
        ev.setSectores(sectores);
        return ev;
    }

    private EventoDetalleFeignDTO eventoCancelado() {
        EventoDetalleFeignDTO ev = new EventoDetalleFeignDTO();
        ev.setEstado("CANCELADO");
        ev.setNombre("Evento Cancelado");
        return ev;
    }
}
