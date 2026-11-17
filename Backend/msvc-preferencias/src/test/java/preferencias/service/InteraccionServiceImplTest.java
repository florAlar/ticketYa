package preferencias.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import preferencias.clients.EventosClient;
import preferencias.clients.EventosClient.EventoDetalleDTO;
import preferencias.clients.EventosClient.LugarDetalleDTO;
import preferencias.clients.EventosClient.ParticipanteDTO;
import preferencias.document.InteraccionEvento;
import preferencias.dto.HistorialItemDTO;
import preferencias.dto.PageResponseDTO;
import preferencias.messaging.CompraConfirmadaDTO;
import preferencias.repository.InteraccionEventoRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InteraccionServiceImplTest {

    @Mock
    private InteraccionEventoRepository repository;

    @Mock
    private EventosClient eventosClient;

    private InteraccionServiceImpl service;

    private static final String COMPRA = "f0000000-0000-4000-8000-000000000021";
    private static final String CLIENTE = "b0000000-0000-4000-8000-000000000001";
    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";

    @BeforeEach
    void setUp() {
        service = new InteraccionServiceImpl(repository, eventosClient);
        when(repository.save(any(InteraccionEvento.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registrarCompra_guardaSnapshotDelEvento() {
        when(repository.findByIdCompra(COMPRA)).thenReturn(Optional.empty());
        when(eventosClient.getEvento(EVENTO)).thenReturn(detalle());

        InteraccionEvento doc = service.registrarCompra(
                COMPRA, CLIENTE, EVENTO, new BigDecimal("95000"), 2,
                Instant.parse("2026-10-01T14:05:00Z"));

        assertEquals(COMPRA, doc.getIdCompra());
        assertEquals(InteraccionServiceImpl.TIPO_COMPRA, doc.getTipo());
        assertEquals("Metallica — M72", doc.getNombreEvento());
        assertEquals("RECITAL", doc.getCategoria());
        assertEquals("Buenos Aires", doc.getCiudad());
        assertEquals(List.of("Metallica", "Gojira"), doc.getArtistas());
        assertEquals(2, doc.getCantidadEntradas());
        assertNotNull(doc.getFechaEvento());
        verify(repository).save(doc);
    }

    @Test
    void registrarCompra_esIdempotentePorIdCompra() {
        InteraccionEvento existente = new InteraccionEvento();
        existente.setId("mongo-1");
        existente.setIdCompra(COMPRA);
        existente.setIdCliente(CLIENTE);
        existente.setIdEvento(EVENTO);
        existente.setTipo(InteraccionServiceImpl.TIPO_COMPRA);
        existente.setNombreEvento("Snapshot viejo");
        existente.setArtistas(List.of("Metallica"));
        when(repository.findByIdCompra(COMPRA)).thenReturn(Optional.of(existente));

        InteraccionEvento doc = service.registrarCompra(
                COMPRA, CLIENTE, EVENTO, new BigDecimal("100"), 1,
                Instant.parse("2026-10-02T10:00:00Z"));

        assertEquals("mongo-1", doc.getId());
        assertEquals(new BigDecimal("100"), doc.getMonto());
        assertEquals("Snapshot viejo", doc.getNombreEvento());
    }

    @Test
    void registrarCompra_siEventosFalla_guardaIgual() {
        when(repository.findByIdCompra(COMPRA)).thenReturn(Optional.empty());
        when(eventosClient.getEvento(EVENTO)).thenThrow(new RuntimeException("timeout"));

        InteraccionEvento doc = service.registrarCompra(
                COMPRA, CLIENTE, EVENTO, new BigDecimal("95000"), 1, Instant.now());

        assertNull(doc.getNombreEvento());
        assertNotNull(doc.getIdCliente());
        verify(repository).save(doc);
    }

    @Test
    void registrarCompra_desdeMensaje_validaCampos() {
        CompraConfirmadaDTO valido = new CompraConfirmadaDTO(
                "msg", COMPRA, CLIENTE, EVENTO, new BigDecimal("1"), "ARS", 1, Instant.now());
        when(repository.findByIdCompra(COMPRA)).thenReturn(Optional.empty());

        service.registrarCompra(valido);

        verify(repository).save(any(InteraccionEvento.class));
        assertThrowsIllegalArgument(new CompraConfirmadaDTO(
                "msg", null, CLIENTE, EVENTO, null, "ARS", 1, Instant.now()));
    }

    @Test
    void historial_mapeaSnapshotYPagina() {
        InteraccionEvento doc = new InteraccionEvento();
        doc.setId("mongo-1");
        doc.setIdCompra(COMPRA);
        doc.setIdCliente(CLIENTE);
        doc.setIdEvento(EVENTO);
        doc.setTipo(InteraccionServiceImpl.TIPO_COMPRA);
        doc.setNombreEvento("Metallica — M72");
        doc.setCategoria("RECITAL");
        doc.setCiudad("Buenos Aires");
        doc.setArtistas(List.of("Metallica"));
        doc.setMonto(new BigDecimal("95000"));
        doc.setCantidadEntradas(1);
        doc.setFechaCompra(Instant.parse("2026-10-01T14:05:00Z"));
        when(repository.findByIdClienteAndTipo(
                org.mockito.ArgumentMatchers.eq(CLIENTE),
                org.mockito.ArgumentMatchers.eq(InteraccionServiceImpl.TIPO_COMPRA),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(doc), PageRequest.of(0, 20), 1));

        PageResponseDTO<HistorialItemDTO> page = service.historial(CLIENTE, 0, 20);

        assertEquals(1, page.getTotalElements());
        assertEquals("Metallica — M72", page.getContent().get(0).getNombreEvento());
        assertEquals(List.of("Metallica"), page.getContent().get(0).getArtistas());
    }

    @Test
    void comprasDeCliente_vacioSiNoHayId() {
        assertTrue(service.comprasDeCliente(" ").isEmpty());
        assertTrue(service.comprasDeEvento(null).isEmpty());
        assertTrue(service.comprasDeClientes(List.of()).isEmpty());
    }

    private void assertThrowsIllegalArgument(CompraConfirmadaDTO dto) {
        try {
            service.registrarCompra(dto);
            throw new AssertionError("Se esperaba IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    private static EventoDetalleDTO detalle() {
        EventoDetalleDTO dto = new EventoDetalleDTO();
        dto.setId(EVENTO);
        dto.setNombre("Metallica — M72");
        dto.setTipo("RECITAL");
        dto.setFechaInicio(LocalDateTime.of(2027, 3, 15, 21, 0));
        LugarDetalleDTO lugar = new LugarDetalleDTO();
        lugar.setCiudad("Buenos Aires");
        dto.setLugar(lugar);
        dto.setParticipantes(List.of(participante("Metallica"), participante("Gojira")));
        return dto;
    }

    private static ParticipanteDTO participante(String nombre) {
        ParticipanteDTO p = new ParticipanteDTO();
        p.setNombre(nombre);
        return p;
    }
}
