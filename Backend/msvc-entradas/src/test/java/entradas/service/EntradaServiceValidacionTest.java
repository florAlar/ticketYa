package entradas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import entradas.clients.EventosClient;
import entradas.clients.EventosClient.EventoDetalleFeignDTO;
import entradas.clients.VentasClient;
import entradas.dto.ValidacionRegistroDTO;
import entradas.dto.ValidacionRequestDTO;
import entradas.dto.ValidacionResponseDTO;
import entradas.entity.Entrada;
import entradas.entity.ValidacionAcceso;
import entradas.exception.ForbiddenException;
import entradas.exception.ResourceNotFoundException;
import entradas.mapper.EntradaMapper;
import entradas.repository.DevolucionRepository;
import entradas.repository.EntradaRepository;
import entradas.repository.ValidacionAccesoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** E2-ACC-01: validación de ingreso por QR en la puerta. */
class EntradaServiceValidacionTest {

    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";
    private static final String OTRO_EVENTO = "d0000000-0000-4000-8000-000000000002";
    private static final String QR = "QR-8F3A1C5D2B7E9A40";
    private static final String PERSONAL = "b0000000-0000-4000-8000-000000000004";
    private static final String ORGANIZADOR = "b0000000-0000-4000-8000-000000000002";

    private EntradaRepository entradaRepository;
    private ValidacionAccesoRepository validacionRepository;
    private EventosClient eventosClient;
    private EntradaServiceImpl service;

    @BeforeEach
    void setUp() {
        entradaRepository = mock(EntradaRepository.class);
        eventosClient = mock(EventosClient.class);
        service = nuevoServicio(mock(ValidacionAccesoRepository.class));
        when(eventosClient.getEvento(EVENTO)).thenReturn(evento("PUBLICADO", ORGANIZADOR));
    }

    private EntradaServiceImpl nuevoServicio(ValidacionAccesoRepository repo) {
        validacionRepository = repo;
        return new EntradaServiceImpl(
                entradaRepository,
                mock(DevolucionRepository.class),
                new EntradaMapper(),
                eventosClient,
                mock(VentasClient.class),
                repo,
                72L);
    }

    private static EventoDetalleFeignDTO evento(String estado, String idOrganizador) {
        EventoDetalleFeignDTO ev = new EventoDetalleFeignDTO();
        ev.setId(EVENTO);
        ev.setNombre("Metallica - M72 World Tour");
        ev.setEstado(estado);
        ev.setIdOrganizador(idOrganizador);
        return ev;
    }

    private Entrada entrada(String idEvento, String estado) {
        Entrada e = new Entrada();
        e.setIdEntrada("66666666-6666-4666-8666-666666666666");
        e.setIdCompra("33333333-3333-4333-8333-333333333333");
        e.setIdCliente("b0000000-0000-4000-8000-000000000001");
        e.setIdEvento(idEvento);
        e.setIdSector("c0000000-0000-4000-8000-000000000012");
        e.setFila("C");
        e.setNumero(14);
        e.setPrecio(BigDecimal.valueOf(180000));
        e.setCodigoQr(QR);
        e.setEstado(estado);
        return e;
    }

    private void entradaExiste(Entrada e) {
        when(entradaRepository.findByCodigoQrForUpdate(QR)).thenReturn(Optional.of(e));
    }

    private ValidacionAcceso registroGuardado() {
        ArgumentCaptor<ValidacionAcceso> cap = ArgumentCaptor.forClass(ValidacionAcceso.class);
        verify(validacionRepository).save(cap.capture());
        return cap.getValue();
    }

    private ValidacionResponseDTO validar(String idEvento) {
        return service.validarIngreso(idEvento, PERSONAL, new ValidacionRequestDTO(QR, "puerta-3"));
    }

    @Test
    void entradaVendida_seMarcaUsada_yQuedaRegistrada() {
        Entrada e = entrada(EVENTO, "VENDIDA");
        entradaExiste(e);

        ValidacionResponseDTO res = validar(EVENTO);

        assertTrue(res.isOk());
        assertEquals("VALIDA", res.getResultado());
        assertEquals("USADA", e.getEstado());
        assertNotNull(e.getFechaValidacion());
        verify(entradaRepository).save(e);
        ValidacionAcceso v = registroGuardado();
        assertEquals("VALIDA", v.getResultado());
        assertEquals(PERSONAL, v.getIdPersonalAcceso());
        assertEquals("puerta-3", v.getDispositivo());
        assertEquals(e, v.getEntrada());
        // datos para mostrar en la puerta
        assertEquals("Metallica - M72 World Tour", res.getEntrada().getEventoNombre());
        assertEquals("C", res.getEntrada().getFila());
        assertEquals(14, res.getEntrada().getAsiento());
        assertNull(res.getEntrada().getCodigoQR(), "una entrada USADA ya no expone el QR");
    }

    @Test
    void entradaYaUsada_seRechaza_noSeModifica_yQuedaRegistrada() {
        Entrada e = entrada(EVENTO, "USADA");
        LocalDateTime antes = LocalDateTime.of(2027, 3, 15, 20, 30);
        e.setFechaValidacion(antes);
        entradaExiste(e);

        ValidacionResponseDTO res = validar(EVENTO);

        assertFalse(res.isOk());
        assertEquals("YA_USADA", res.getResultado());
        assertEquals(antes, e.getFechaValidacion(), "no se pisa la fecha del primer ingreso");
        verify(entradaRepository, never()).save(any());
        assertEquals("YA_USADA", registroGuardado().getResultado());
    }

    @Test
    void entradaDeOtroEvento_seRechaza_yNoSeUsa() {
        Entrada e = entrada(OTRO_EVENTO, "VENDIDA");
        entradaExiste(e);

        ValidacionResponseDTO res = validar(EVENTO);

        assertFalse(res.isOk());
        assertEquals("OTRO_EVENTO", res.getResultado());
        assertEquals("VENDIDA", e.getEstado());
        verify(entradaRepository, never()).save(any());
        assertEquals("OTRO_EVENTO", registroGuardado().getResultado());
        assertNull(res.getEntrada(), "no se devuelven datos de una entrada de otro evento");
    }

    @Test
    void entradaDevuelta_o_aReintegrar_noEsValida() {
        for (String estado : List.of("DEVUELTA", "A_REINTEGRAR")) {
            service = nuevoServicio(mock(ValidacionAccesoRepository.class));
            Entrada e = entrada(EVENTO, estado);
            entradaExiste(e);

            ValidacionResponseDTO res = validar(EVENTO);

            assertFalse(res.isOk(), estado);
            assertEquals("NO_VALIDA", res.getResultado(), estado);
            assertEquals(estado, e.getEstado());
            assertEquals("NO_VALIDA", registroGuardado().getResultado());
        }
    }

    @Test
    void eventoCancelado_noPermiteIngreso() {
        when(eventosClient.getEvento(EVENTO)).thenReturn(evento("CANCELADO", "x"));
        Entrada e = entrada(EVENTO, "VENDIDA");
        entradaExiste(e);

        ValidacionResponseDTO res = validar(EVENTO);

        assertFalse(res.isOk());
        assertEquals("EVENTO_CANCELADO", res.getResultado());
        assertEquals("VENDIDA", e.getEstado());
        verify(entradaRepository, never()).save(any());
    }

    @Test
    void siEventosNoResponde_seConfiaEnElEstadoDeLaEntrada() {
        when(eventosClient.getEvento(EVENTO)).thenThrow(new RuntimeException("eventos caido"));
        Entrada e = entrada(EVENTO, "VENDIDA");
        entradaExiste(e);

        ValidacionResponseDTO res = validar(EVENTO);

        assertTrue(res.isOk(), "no se bloquea la puerta si falla un servicio externo");
        assertEquals("USADA", e.getEstado());
    }

    @Test
    void qrInexistente_lanza404_yNoRegistra() {
        when(entradaRepository.findByCodigoQrForUpdate(QR)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> validar(EVENTO));

        verify(validacionRepository, never()).save(any());
    }

    @Test
    void qrConEspacios_seNormaliza_yDispositivoVacioQuedaNulo() {
        Entrada e = entrada(EVENTO, "VENDIDA");
        entradaExiste(e);

        ValidacionResponseDTO res =
                service.validarIngreso(EVENTO, PERSONAL, new ValidacionRequestDTO("  " + QR + " ", " "));

        assertTrue(res.isOk());
        assertNull(registroGuardado().getDispositivo());
    }

    @Test
    void listarValidaciones_admin_veCualquierEvento() {
        Entrada e = entrada(EVENTO, "USADA");
        ValidacionAcceso v = new ValidacionAcceso();
        v.setIdValidacion("v1");
        v.setEntrada(e);
        v.setIdPersonalAcceso(PERSONAL);
        v.setResultado("VALIDA");
        v.setFechaValidacion(LocalDateTime.of(2027, 3, 15, 20, 30));
        when(validacionRepository.findByEntrada_IdEventoOrderByFechaValidacionDesc(EVENTO))
                .thenReturn(List.of(v));

        List<ValidacionRegistroDTO> out = service.listarValidaciones(EVENTO, "cualquiera", true);

        assertEquals(1, out.size());
        assertEquals("VALIDA", out.get(0).getResultado());
        assertEquals("C", out.get(0).getFila());
        assertEquals(14, out.get(0).getAsiento());
    }

    @Test
    void listarValidaciones_organizadorDuenio_puede() {
        when(validacionRepository.findByEntrada_IdEventoOrderByFechaValidacionDesc(EVENTO))
                .thenReturn(List.of());

        assertTrue(service.listarValidaciones(EVENTO, ORGANIZADOR, false).isEmpty());
    }

    @Test
    void listarValidaciones_organizadorAjeno_oEventosCaido_esForbidden() {
        assertThrows(ForbiddenException.class,
                () -> service.listarValidaciones(EVENTO, "b0000000-0000-4000-8000-0000000000ff", false));

        when(eventosClient.getEvento(EVENTO)).thenThrow(new RuntimeException("caido"));
        assertThrows(ForbiddenException.class,
                () -> service.listarValidaciones(EVENTO, ORGANIZADOR, false));
    }
}
