package notificaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import notificaciones.clients.PreferenciasClient;
import notificaciones.clients.PreferenciasClient.MatchingClientesRequest;
import notificaciones.clients.PreferenciasClient.PreferenciasNotificaciones;
import notificaciones.clients.UsuariosClient;
import notificaciones.clients.VentasClient;
import notificaciones.entity.Notificacion;
import notificaciones.entity.PlantillaNotificacion;
import notificaciones.messaging.CompraConfirmadaDTO;
import notificaciones.messaging.EventoCambioDTO;
import notificaciones.repository.NotificacionRepository;
import notificaciones.repository.PlantillaNotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceImplTest {

    private static final String EVENTO_ID = "e0000000-0000-4000-8000-000000000001";
    private static final String COMPRA_ID = "f0000000-0000-4000-8000-000000000021";
    private static final String CLIENTE_1 = "c0000000-0000-4000-8000-000000000001";
    private static final String CLIENTE_2 = "c0000000-0000-4000-8000-000000000002";

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private PlantillaNotificacionRepository plantillaRepository;

    @Mock
    private VentasClient ventasClient;

    @Mock
    private PreferenciasClient preferenciasClient;

    @Mock
    private UsuariosClient usuariosClient;

    @InjectMocks
    private NotificacionServiceImpl service;

    private PlantillaNotificacion plantilla;

    @BeforeEach
    void setUp() {
        plantilla = plantilla("EventoCancelado", "PUSH", "Evento cancelado",
                "El evento {{evento}} del {{fecha}} fue cancelado.");
    }

    @Test
    void procesarEvento_cancelado_creaUnaNotificacionPushPorCliente() {
        when(ventasClient.clientesAprobadosPorEvento(EVENTO_ID))
                .thenReturn(List.of(CLIENTE_1, CLIENTE_2));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("EventoCancelado", "PUSH"))
                .thenReturn(Optional.of(plantilla));
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.procesarEvento(evento("EventoCancelado"));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, times(2)).save(captor.capture());

        Notificacion primera = captor.getAllValues().get(0);
        assertEquals(CLIENTE_1, primera.getIdUsuario());
        assertEquals(EVENTO_ID, primera.getIdEventoOrigen());
        assertEquals("EventoCancelado", primera.getTipo());
        assertEquals("PUSH", primera.getCanal());
        assertEquals("PENDIENTE", primera.getEstado());
        assertEquals("Evento cancelado", primera.getAsunto());
        assertEquals("El evento Rock del 20/12/2026 21:00 fue cancelado.", primera.getContenido());
        assertEquals(plantilla, primera.getPlantilla());
    }

    @Test
    void procesarEvento_cancelado_conPlantillasPushYEmail_creaDualCanal() {
        PlantillaNotificacion push = plantilla(
                "EventoCancelado", "PUSH", "Evento cancelado", "Cancelado {{evento}}");
        PlantillaNotificacion email = plantilla(
                "EventoCancelado", "EMAIL", "Evento cancelado", "Cancelado {{evento}}");
        when(ventasClient.clientesAprobadosPorEvento(EVENTO_ID)).thenReturn(List.of(CLIENTE_1));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("EventoCancelado", "PUSH"))
                .thenReturn(Optional.of(push));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("EventoCancelado", "EMAIL"))
                .thenReturn(Optional.of(email));
        when(usuariosClient.porId(CLIENTE_1))
                .thenReturn(new UsuariosClient.UsuarioEmailDTO(CLIENTE_1, "demo@ticketya.com"));
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.procesarEvento(evento("EventoCancelado"));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, times(2)).save(captor.capture());
        assertEquals(List.of("PUSH", "EMAIL"),
                captor.getAllValues().stream().map(Notificacion::getCanal).toList());
        assertEquals("EventoCancelado:PUSH:" + EVENTO_ID,
                captor.getAllValues().get(0).getClaveIdempotencia());
        assertEquals("EventoCancelado:EMAIL:" + EVENTO_ID,
                captor.getAllValues().get(1).getClaveIdempotencia());
        assertEquals("demo@ticketya.com", captor.getAllValues().get(1).getDestinatario());
    }

    @Test
    void procesarEvento_suspendido_tambienHaceFanOut() {
        PlantillaNotificacion suspendido = plantilla(
                "EventoSuspendido", "PUSH", "Evento suspendido", "Suspendido: {{evento}}");
        when(ventasClient.clientesAprobadosPorEvento(EVENTO_ID)).thenReturn(List.of(CLIENTE_1));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("EventoSuspendido", "PUSH"))
                .thenReturn(Optional.of(suspendido));
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.procesarEvento(evento("EventoSuspendido"));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertEquals("EventoSuspendido", captor.getValue().getTipo());
        assertEquals("Suspendido: Rock", captor.getValue().getContenido());
    }

    @Test
    void procesarEvento_saltaClientesYaNotificados() {
        when(ventasClient.clientesAprobadosPorEvento(EVENTO_ID))
                .thenReturn(List.of(CLIENTE_1, CLIENTE_2));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("EventoCancelado", "PUSH"))
                .thenReturn(Optional.of(plantilla));
        when(notificacionRepository.existsByIdUsuarioAndClaveIdempotencia(
                CLIENTE_1, "EventoCancelado:PUSH:" + EVENTO_ID)).thenReturn(true);
        when(notificacionRepository.existsByIdUsuarioAndClaveIdempotencia(
                CLIENTE_2, "EventoCancelado:PUSH:" + EVENTO_ID)).thenReturn(false);
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.procesarEvento(evento("EventoCancelado"));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, times(1)).save(captor.capture());
        assertEquals(CLIENTE_2, captor.getValue().getIdUsuario());
    }

    @Test
    void procesarEvento_sinClientes_noPersiste() {
        when(ventasClient.clientesAprobadosPorEvento(EVENTO_ID)).thenReturn(List.of());

        service.procesarEvento(evento("EventoCancelado"));

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void procesarEvento_tipoDesconocido_noConsultaOtrosServicios() {
        service.procesarEvento(evento("EventoRandom"));

        verifyNoInteractions(ventasClient, preferenciasClient, notificacionRepository, plantillaRepository);
    }

    @Test
    void procesarEvento_sinPlantillas_noPersiste() {
        when(ventasClient.clientesAprobadosPorEvento(EVENTO_ID)).thenReturn(List.of(CLIENTE_1));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("EventoCancelado", "PUSH"))
                .thenReturn(Optional.empty());
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("EventoCancelado", "EMAIL"))
                .thenReturn(Optional.empty());

        service.procesarEvento(evento("EventoCancelado"));

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void procesarEvento_publicado_notificaArtistaFavoritoEnLaCiudad() {
        PlantillaNotificacion favorito = plantilla(
                "NuevoEventoArtistaFavorito", "PUSH", "Tu artista favorito",
                "¡{{evento}} en {{ciudad}}!");
        when(preferenciasClient.clientesInteresados(
                new MatchingClientesRequest("Buenos Aires", List.of("Metallica"))))
                .thenReturn(List.of(CLIENTE_1));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue(
                "NuevoEventoArtistaFavorito", "PUSH"))
                .thenReturn(Optional.of(favorito));
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.procesarEvento(eventoPublicado());

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertEquals(CLIENTE_1, captor.getValue().getIdUsuario());
        assertEquals("NuevoEventoArtistaFavorito", captor.getValue().getTipo());
        assertEquals("¡Rock en Buenos Aires!", captor.getValue().getContenido());
        verifyNoInteractions(ventasClient);
    }

    @Test
    void procesarEvento_publicado_sinCiudadNoLlamaPreferencias() {
        service.procesarEvento(new EventoCambioDTO(
                "m-1", EVENTO_ID, "EventoPublicado", "Rock", LocalDateTime.now(), null,
                Instant.now(), null, List.of("Metallica")));

        verifyNoInteractions(preferenciasClient, notificacionRepository);
    }

    @Test
    void procesarCompraConfirmada_creaEmailYUsaCompraComoOrigen() {
        PlantillaNotificacion compra = plantilla(
                "CompraConfirmada", "EMAIL", "Tu compra", "Compra {{compraId}} por {{cantidadEntradas}}");
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("CompraConfirmada", "EMAIL"))
                .thenReturn(Optional.of(compra));
        when(usuariosClient.porId(CLIENTE_1))
                .thenReturn(new UsuariosClient.UsuarioEmailDTO(CLIENTE_1, "demo@ticketya.com"));
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.procesarCompraConfirmada(new CompraConfirmadaDTO(
                "m-2", COMPRA_ID, CLIENTE_1, EVENTO_ID, new BigDecimal("1500.00"), "ARS", 2,
                Instant.now()));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion n = captor.getValue();
        assertEquals("CompraConfirmada", n.getTipo());
        assertEquals("EMAIL", n.getCanal());
        assertEquals(EVENTO_ID, n.getIdEventoOrigen());
        assertEquals("CompraConfirmada:EMAIL:" + COMPRA_ID, n.getClaveIdempotencia());
        assertEquals("demo@ticketya.com", n.getDestinatario());
        assertEquals("Compra " + COMPRA_ID + " por 2", n.getContenido());
    }

    @Test
    void procesarCompraConfirmada_respetaPreferenciaDeCanal() {
        PlantillaNotificacion compra = plantilla("CompraConfirmada", "EMAIL", "Tu compra", "ok");
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("CompraConfirmada", "EMAIL"))
                .thenReturn(Optional.of(compra));
        when(preferenciasClient.preferenciasNotificaciones(CLIENTE_1))
                .thenReturn(new PreferenciasNotificaciones(false, false, List.of()));

        service.procesarCompraConfirmada(new CompraConfirmadaDTO(
                "m-2", COMPRA_ID, CLIENTE_1, EVENTO_ID, BigDecimal.TEN, "ARS", 1, Instant.now()));

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void procesarCompraConfirmada_yaNotificada_noDuplica() {
        when(notificacionRepository.existsByIdUsuarioAndClaveIdempotencia(
                CLIENTE_1, "CompraConfirmada:EMAIL:" + COMPRA_ID)).thenReturn(true);

        service.procesarCompraConfirmada(new CompraConfirmadaDTO(
                "m-2", COMPRA_ID, CLIENTE_1, EVENTO_ID, BigDecimal.TEN, "ARS", 1, Instant.now()));

        verify(notificacionRepository, never()).save(any(Notificacion.class));
        verifyNoInteractions(plantillaRepository);
    }

    @Test
    void crearAdministrativa_quedaPendienteYNoLeida() {
        notificaciones.dto.NotificacionRequestDTO request = new notificaciones.dto.NotificacionRequestDTO();
        request.setIdUsuario(CLIENTE_1);
        request.setTipo("Aviso");
        request.setCanal("push");
        request.setAsunto("Hola");
        request.setContenido("Mensaje");
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(i -> i.getArgument(0));

        Notificacion n = service.crearAdministrativa(request);

        assertEquals("PUSH", n.getCanal());
        assertEquals("PENDIENTE", n.getEstado());
        assertTrue(!n.getLeida());
    }

    private static PlantillaNotificacion plantilla(
            String tipo, String canal, String asunto, String contenido) {
        PlantillaNotificacion p = new PlantillaNotificacion();
        p.setIdPlantilla("n-1");
        p.setTipoEvento(tipo);
        p.setCanal(canal);
        p.setAsunto(asunto);
        p.setContenido(contenido);
        p.setActiva(true);
        return p;
    }

    private EventoCambioDTO evento(String tipo) {
        return new EventoCambioDTO(
                "m-1", EVENTO_ID, tipo, "Rock", LocalDateTime.of(2026, 12, 20, 21, 0), null,
                Instant.parse("2026-11-13T19:20:00Z"));
    }

    private EventoCambioDTO eventoPublicado() {
        return new EventoCambioDTO(
                "m-3", EVENTO_ID, "EventoPublicado", "Rock",
                LocalDateTime.of(2026, 12, 20, 21, 0), null,
                Instant.parse("2026-11-13T19:20:00Z"), "Buenos Aires", List.of("Metallica"));
    }
}
