package preferencias.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import preferencias.clients.EventosClient;
import preferencias.clients.EventosClient.EventoDetalleDTO;
import preferencias.clients.EventosClient.EventoResumenDTO;
import preferencias.clients.EventosClient.EventosPageDTO;
import preferencias.clients.EventosClient.LugarDTO;
import preferencias.clients.EventosClient.ParticipanteDTO;
import preferencias.config.RecomendacionProperties;
import preferencias.document.InteraccionEvento;
import preferencias.document.PreferenciasCliente;
import preferencias.document.RangoPrecio;
import preferencias.dto.EventoRecomendadoDTO;
import preferencias.exception.ResourceNotFoundException;
import preferencias.repository.PreferenciasClienteRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecomendacionServiceImplTest {

    @Mock
    private EventosClient eventosClient;

    @Mock
    private InteraccionService interaccionService;

    @Mock
    private PreferenciasClienteRepository preferenciasRepository;

    private RecomendacionProperties props;
    private RecomendacionServiceImpl service;

    private static final String E1 = "d0000000-0000-4000-8000-000000000001";
    private static final String E2 = "d0000000-0000-4000-8000-000000000004";
    private static final String E3 = "d0000000-0000-4000-8000-000000000005";
    private static final String E4 = "d0000000-0000-4000-8000-000000000008";
    private static final String CLIENTE = "b0000000-0000-4000-8000-000000000001";

    @BeforeEach
    void setUp() {
        props = new RecomendacionProperties();
        service = new RecomendacionServiceImpl(
                eventosClient, interaccionService, preferenciasRepository, props);
        when(eventosClient.listar(anyString(), anyString(), anyInt(), eq(true)))
                .thenReturn(pagina(
                        resumen(E1, "RECITAL", "Buenos Aires", List.of("Metallica"), 95000, true),
                        resumen(E2, "RECITAL", "Buenos Aires",
                                List.of("Iron Maiden", "Metallica"), 90000, true),
                        resumen(E3, "RECITAL", "Buenos Aires", List.of("Tool"), 100000, false),
                        resumen(E4, "DEPORTE", "London", List.of(), 200000, false)));
    }

    @Test
    void similares_priorizaCoComprasYExcluyeElEventoFuente() {
        when(eventosClient.getEvento(E1)).thenReturn(detalle(E1, "Metallica", "RECITAL",
                "Buenos Aires", List.of("Metallica", "Gojira")));
        when(interaccionService.comprasDeEvento(E1)).thenReturn(List.of(
                interaccion("c1", E1), interaccion("c2", E1)));
        when(interaccionService.comprasDeClientes(anyCollection())).thenReturn(List.of(
                interaccion("c1", E2), interaccion("c2", E2), interaccion("c2", E3)));

        List<EventoRecomendadoDTO> result = service.similares(E1, 3);

        assertEquals(3, result.size());
        assertEquals(E2, result.get(0).getId());
        assertEquals(E3, result.get(1).getId());
        assertFalse(result.stream().anyMatch(r -> E1.equals(r.getId())));
        assertEquals("Quienes compraron este evento también compraron este", result.get(0).getMotivo());
    }

    @Test
    void similares_eventoInexistente_devuelve404() {
        when(eventosClient.getEvento("no-existe")).thenThrow(mock(FeignException.NotFound.class));

        assertThrows(ResourceNotFoundException.class, () -> service.similares("no-existe", 5));
    }

    @Test
    void similares_siEventosNoResponde_devuelveVacio() {
        when(eventosClient.getEvento(E1)).thenThrow(new RuntimeException("timeout"));

        assertTrue(service.similares(E1, 5).isEmpty());
    }

    @Test
    void paraCliente_usaPreferenciasYExcluyeComprados() {
        PreferenciasCliente prefs = new PreferenciasCliente();
        prefs.setIdCliente(CLIENTE);
        prefs.setCategorias(List.of("RECITAL"));
        prefs.setCiudades(List.of("Buenos Aires"));
        prefs.setArtistas(List.of("Metallica"));
        RangoPrecio rango = new RangoPrecio();
        rango.setMin(new BigDecimal("20000"));
        rango.setMax(new BigDecimal("300000"));
        prefs.setRangoPrecio(rango);
        when(preferenciasRepository.findByIdCliente(CLIENTE)).thenReturn(Optional.of(prefs));
        when(interaccionService.comprasDeCliente(CLIENTE)).thenReturn(List.of(interaccion(CLIENTE, E1)));
        when(interaccionService.comprasDeEventos(anyCollection()))
                .thenReturn(List.of(interaccion(CLIENTE, E1)));

        List<EventoRecomendadoDTO> result = service.paraCliente(CLIENTE, 5);

        assertFalse(result.stream().anyMatch(r -> E1.equals(r.getId())));
        assertEquals(E2, result.get(0).getId());
        assertEquals("Porque te gusta Metallica", result.get(0).getMotivo());
    }

    @Test
    void paraCliente_sinPreferenciasNiHistorial_devuelveProximos() {
        when(preferenciasRepository.findByIdCliente(CLIENTE)).thenReturn(Optional.empty());
        when(interaccionService.comprasDeCliente(CLIENTE)).thenReturn(List.of());

        List<EventoRecomendadoDTO> result = service.paraCliente(CLIENTE, 2);

        assertEquals(2, result.size());
        assertEquals(E1, result.get(0).getId());
        assertEquals("Evento con alta demanda", result.get(0).getMotivo());
    }

    @Test
    void paraCliente_sinCandidatos_devuelveVacio() {
        when(eventosClient.listar(anyString(), anyString(), anyInt(), eq(true)))
                .thenReturn(pagina());
        when(preferenciasRepository.findByIdCliente(CLIENTE)).thenReturn(Optional.empty());
        when(interaccionService.comprasDeCliente(CLIENTE)).thenReturn(List.of());

        assertTrue(service.paraCliente(CLIENTE, 5).isEmpty());
    }

    @Test
    void limiteSeAcotaAlMaximoConfigurado() {
        props.setLimiteMaximo(2);
        when(eventosClient.getEvento(E1)).thenReturn(detalle(E1, "Metallica", "RECITAL",
                "Buenos Aires", List.of("Metallica")));
        when(interaccionService.comprasDeEvento(E1)).thenReturn(List.of());

        assertEquals(2, service.similares(E1, 50).size());
    }

    private static EventosPageDTO pagina(EventoResumenDTO... eventos) {
        EventosPageDTO page = new EventosPageDTO();
        page.setContent(List.of(eventos));
        page.setPage(0);
        page.setSize(20);
        page.setTotalElements(eventos.length);
        page.setTotalPages(1);
        return page;
    }

    private static EventoResumenDTO resumen(
            String id, String tipo, String ciudad, List<String> artistas,
            int precio, boolean altaDemanda) {
        EventoResumenDTO dto = new EventoResumenDTO();
        dto.setId(id);
        dto.setNombre("Evento " + id);
        dto.setTipo(tipo);
        dto.setFechaInicio(LocalDateTime.now().plusMonths(2));
        LugarDTO lugar = new LugarDTO();
        lugar.setNombre("Lugar");
        lugar.setCiudad(ciudad);
        dto.setLugar(lugar);
        dto.setArtistas(artistas);
        dto.setPrecioDesde(new BigDecimal(precio));
        dto.setEsAltaDemanda(altaDemanda);
        return dto;
    }

    private static EventoDetalleDTO detalle(
            String id, String nombre, String tipo, String ciudad, List<String> artistas) {
        EventoDetalleDTO dto = new EventoDetalleDTO();
        dto.setId(id);
        dto.setNombre(nombre);
        dto.setTipo(tipo);
        EventosClient.LugarDetalleDTO lugar = new EventosClient.LugarDetalleDTO();
        lugar.setCiudad(ciudad);
        dto.setLugar(lugar);
        dto.setParticipantes(artistas.stream().map(n -> {
            ParticipanteDTO p = new ParticipanteDTO();
            p.setNombre(n);
            return p;
        }).toList());
        return dto;
    }

    private static InteraccionEvento interaccion(String idCliente, String idEvento) {
        InteraccionEvento i = new InteraccionEvento();
        i.setIdCliente(idCliente);
        i.setIdEvento(idEvento);
        i.setTipo(InteraccionServiceImpl.TIPO_COMPRA);
        return i;
    }
}
