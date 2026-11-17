package preferencias.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
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
import preferencias.dto.HistorialItemDTO;
import preferencias.dto.PageResponseDTO;
import preferencias.repository.InteraccionEventoRepository;
import preferencias.repository.PreferenciasClienteRepository;

/**
 * IT de persistencia y algoritmo con MongoDB real. Se ejecuta con {@code mvn -Pit verify}.
 * No levanta Rabbit ni el contexto web: verifica las queries/índices y el scoring.
 */
@DataMongoTest
@Testcontainers(disabledWithoutDocker = true)
class RecomendacionMongoIT {

    @Container
    static final GenericContainer<?> MONGO = new GenericContainer<>(DockerImageName.parse("mongo:7.0"))
            .withExposedPorts(27017)
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        String uri = "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017)
                + "/preferencias_test";
        registry.add("spring.mongodb.uri", () -> uri);
        registry.add("spring.data.mongodb.uri", () -> uri);
        registry.add("spring.data.mongodb.auto-index-creation", () -> true);
    }

    @Autowired
    private InteraccionEventoRepository interacciones;

    @Autowired
    private PreferenciasClienteRepository preferencias;

    private EventosClient eventosClient;
    private InteraccionServiceImpl interaccionService;
    private RecomendacionServiceImpl recomendacionService;

    private static final String CLIENTE = "b0000000-0000-4000-8000-000000000001";
    private static final String E1 = "d0000000-0000-4000-8000-000000000001";
    private static final String E2 = "d0000000-0000-4000-8000-000000000004";
    private static final String E3 = "d0000000-0000-4000-8000-000000000005";
    private static final String E4 = "d0000000-0000-4000-8000-000000000006";

    @BeforeEach
    void setUp() {
        interacciones.deleteAll();
        preferencias.deleteAll();
        eventosClient = mock(EventosClient.class);
        interaccionService = new InteraccionServiceImpl(interacciones, eventosClient);
        recomendacionService = new RecomendacionServiceImpl(
                eventosClient, interaccionService, preferencias, new RecomendacionProperties());

        when(eventosClient.listar(anyString(), anyString(), anyInt(), eq(true)))
                .thenReturn(pagina(
                        resumen(E1, "RECITAL", "Buenos Aires", List.of("Metallica")),
                        resumen(E2, "RECITAL", "Buenos Aires", List.of("Iron Maiden")),
                        resumen(E3, "RECITAL", "Buenos Aires", List.of("Rammstein")),
                        resumen(E4, "RECITAL", "London", List.of("Coldplay"))));
        when(eventosClient.getEvento(E1)).thenReturn(detalle(
                E1, "Metallica", "RECITAL", "Buenos Aires", List.of("Metallica", "Gojira")));
        when(eventosClient.getEvento(E2)).thenReturn(detalle(
                E2, "Iron Maiden", "RECITAL", "Buenos Aires", List.of("Iron Maiden")));
    }

    @Test
    void similares_priorizaCoComprasSobreContenido() {
        seed("compra-1", CLIENTE, E1);
        seed("compra-2", "cliente-2", E1);
        seed("compra-3", "cliente-2", E2);
        seed("compra-4", "cliente-3", E1);
        seed("compra-5", "cliente-3", E2);
        seed("compra-6", "cliente-3", E3);

        List<EventoRecomendadoDTO> result = recomendacionService.similares(E1, 3);

        assertFalse(result.isEmpty());
        assertEquals(E2, result.get(0).getId());
        assertEquals(E3, result.get(1).getId());
        assertFalse(result.stream().anyMatch(r -> E1.equals(r.getId())));
    }

    @Test
    void paraCliente_excluyeCompradosYPriorizaArtistaFavorito() {
        PreferenciasCliente prefs = new PreferenciasCliente();
        prefs.setIdCliente(CLIENTE);
        prefs.setCategorias(List.of("RECITAL"));
        prefs.setCiudades(List.of("Buenos Aires"));
        prefs.setArtistas(List.of("Iron Maiden"));
        RangoPrecio rango = new RangoPrecio();
        rango.setMin(new BigDecimal("20000"));
        rango.setMax(new BigDecimal("300000"));
        prefs.setRangoPrecio(rango);
        preferencias.save(prefs);

        seed("compra-1", CLIENTE, E1);

        List<EventoRecomendadoDTO> result = recomendacionService.paraCliente(CLIENTE, 4);

        assertFalse(result.stream().anyMatch(r -> E1.equals(r.getId())));
        assertEquals(E2, result.get(0).getId());
        assertTrue(result.get(0).getMotivo().contains("Iron Maiden"));
    }

    @Test
    void historial_leeSnapshotYOrdenaPorFechaCompraDesc() {
        InteraccionEvento vieja = interaccion("compra-1", CLIENTE, E1);
        vieja.setFechaCompra(Instant.parse("2026-09-01T10:00:00Z"));
        InteraccionEvento nueva = interaccion("compra-2", CLIENTE, E2);
        nueva.setFechaCompra(Instant.parse("2026-10-01T10:00:00Z"));
        interacciones.saveAll(List.of(vieja, nueva));

        PageResponseDTO<HistorialItemDTO> page = interaccionService.historial(CLIENTE, 0, 10);

        assertEquals(2, page.getTotalElements());
        assertEquals(E2, page.getContent().get(0).getIdEvento());
        assertEquals("Evento " + E2, page.getContent().get(0).getNombreEvento());
    }

    @Test
    void registrarCompra_esIdempotentePorIdCompra() {
        InteraccionEvento primera = interaccionService.registrarCompra(
                "compra-unica", CLIENTE, E1, new BigDecimal("95000"), 1, Instant.now());
        InteraccionEvento segunda = interaccionService.registrarCompra(
                "compra-unica", CLIENTE, E1, new BigDecimal("99000"), 2, Instant.now());

        assertNotNull(primera.getId());
        assertEquals(primera.getId(), segunda.getId());
        assertEquals(1, interacciones.count());
        assertEquals(0, new BigDecimal("99000").compareTo(segunda.getMonto()));
    }

    private void seed(String idCompra, String cliente, String evento) {
        interacciones.save(interaccion(idCompra, cliente, evento));
    }

    private static InteraccionEvento interaccion(String idCompra, String cliente, String evento) {
        InteraccionEvento i = new InteraccionEvento();
        i.setIdCompra(idCompra);
        i.setIdCliente(cliente);
        i.setIdEvento(evento);
        i.setTipo(InteraccionServiceImpl.TIPO_COMPRA);
        i.setNombreEvento("Evento " + evento);
        i.setCategoria("RECITAL");
        i.setCiudad("Buenos Aires");
        i.setArtistas(List.of("Metallica"));
        i.setFechaEvento(Instant.now().plusSeconds(86400 * 30));
        i.setFechaCompra(Instant.now());
        i.setMonto(new BigDecimal("95000"));
        i.setCantidadEntradas(1);
        return i;
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
            String id, String tipo, String ciudad, List<String> artistas) {
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
        dto.setPrecioDesde(new BigDecimal("95000"));
        dto.setEsAltaDemanda(false);
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
}
