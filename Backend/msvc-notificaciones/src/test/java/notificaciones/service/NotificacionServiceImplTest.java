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
import notificaciones.messaging.DevolucionAprobadaDTO;
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
    private static final String DEVOLUCION_ID = "d0000000-0000-4000-8000-000000000001";
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
        assertEquals("EventoCancelado:PUSH:" + EVENTO_ID + ":m-1",
                captor.getAllValues().get(0).getClaveIdempotencia());
        assertEquals("EventoCancelado:EMAIL:" + EVENTO_ID + ":m-1",
                captor.getAllValues().get(1).getClaveIdempotencia());
        assertEquals(
                captor.getAllValues().get(0).getClaveMensaje(),
                captor.getAllValues().get(1).getClaveMensaje());
        assertEquals("EventoCancelado:" + EVENTO_ID + ":m-1",
                captor.getAllValues().get(0).getClaveMensaje());
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
                CLIENTE_1, "EventoCancelado:PUSH:" + EVENTO_ID + ":m-1")).thenReturn(true);
        when(notificacionRepository.existsByIdUsuarioAndClaveIdempotencia(
                CLIENTE_2, "EventoCancelado:PUSH:" + EVENTO_ID + ":m-1")).thenReturn(false);
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.procesarEvento(evento("EventoCancelado"));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, times(1)).save(captor.capture());
        assertEquals(CLIENTE_2, captor.getValue().getIdUsuario());
    }

    @Test
    void procesarEvento_distintosMensajesDelMismoEvento_creanAvisosSeparados() {
        when(ventasClient.clientesAprobadosPorEvento(EVENTO_ID)).thenReturn(List.of(CLIENTE_1));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("EventoCancelado", "PUSH"))
                .thenReturn(Optional.of(plantilla));
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.procesarEvento(evento("EventoCancelado", "m-1"));
        service.procesarEvento(evento("EventoCancelado", "m-2"));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, times(2)).save(captor.capture());
        assertEquals(
                List.of(
                        "EventoCancelado:PUSH:" + EVENTO_ID + ":m-1",
                        "EventoCancelado:PUSH:" + EVENTO_ID + ":m-2"),
                captor.getAllValues().stream()
                        .map(Notificacion::getClaveIdempotencia)
                        .toList());
        assertEquals(
                List.of(
                        "EventoCancelado:" + EVENTO_ID + ":m-1",
                        "EventoCancelado:" + EVENTO_ID + ":m-2"),
                captor.getAllValues().stream()
                        .map(Notificacion::getClaveMensaje)
                        .toList());
    }

    @Test
    void procesarEvento_mismoMensajeReintentado_noDuplica() {
        when(ventasClient.clientesAprobadosPorEvento(EVENTO_ID)).thenReturn(List.of(CLIENTE_1));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("EventoCancelado", "PUSH"))
                .thenReturn(Optional.of(plantilla));
        when(notificacionRepository.existsByIdUsuarioAndClaveIdempotencia(
                CLIENTE_1, "EventoCancelado:PUSH:" + EVENTO_ID + ":m-1")).thenReturn(true);

        service.procesarEvento(evento("EventoCancelado", "m-1"));

        verify(notificacionRepository, never()).save(any(Notificacion.class));
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
        assertEquals("CompraConfirmada:" + COMPRA_ID, n.getClaveMensaje());
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
    void procesarDevolucionAprobada_creaDualPushEmailConClaveCompartida() {
        PlantillaNotificacion push = plantilla(
                "DevolucionAprobada", "PUSH", "Devolución aprobada", "Reintegro {{monto}} {{moneda}}");
        PlantillaNotificacion email = plantilla(
                "DevolucionAprobada", "EMAIL", "Devolución aprobada", "Reintegro {{monto}} {{moneda}}");
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("DevolucionAprobada", "PUSH"))
                .thenReturn(Optional.of(push));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("DevolucionAprobada", "EMAIL"))
                .thenReturn(Optional.of(email));
        when(usuariosClient.porId(CLIENTE_1))
                .thenReturn(new UsuariosClient.UsuarioEmailDTO(CLIENTE_1, "demo@ticketya.com"));
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.procesarDevolucionAprobada(devolucion());

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, times(2)).save(captor.capture());
        List<Notificacion> guardadas = captor.getAllValues();
        assertEquals(List.of("PUSH", "EMAIL"),
                guardadas.stream().map(Notificacion::getCanal).toList());
        assertEquals("DevolucionAprobada:PUSH:" + DEVOLUCION_ID,
                guardadas.get(0).getClaveIdempotencia());
        assertEquals("DevolucionAprobada:EMAIL:" + DEVOLUCION_ID,
                guardadas.get(1).getClaveIdempotencia());
        assertEquals("DevolucionAprobada:" + DEVOLUCION_ID, guardadas.get(0).getClaveMensaje());
        assertEquals(guardadas.get(0).getClaveMensaje(), guardadas.get(1).getClaveMensaje());
        assertEquals(EVENTO_ID, guardadas.get(0).getIdEventoOrigen());
        assertEquals("Reintegro 95000.00 ARS", guardadas.get(0).getContenido());
        assertEquals("demo@ticketya.com", guardadas.get(1).getDestinatario());
    }

    @Test
    void procesarDevolucionAprobada_respetaPreferenciaDeCanal() {
        PlantillaNotificacion push = plantilla(
                "DevolucionAprobada", "PUSH", "Devolución aprobada", "ok");
        PlantillaNotificacion email = plantilla(
                "DevolucionAprobada", "EMAIL", "Devolución aprobada", "ok");
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("DevolucionAprobada", "PUSH"))
                .thenReturn(Optional.of(push));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("DevolucionAprobada", "EMAIL"))
                .thenReturn(Optional.of(email));
        when(preferenciasClient.preferenciasNotificaciones(CLIENTE_1))
                .thenReturn(new PreferenciasNotificaciones(true, false, List.of()));
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.procesarDevolucionAprobada(devolucion());

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, times(1)).save(captor.capture());
        assertEquals("PUSH", captor.getValue().getCanal());
    }

    @Test
    void procesarDevolucionAprobada_yaNotificada_noDuplica() {
        PlantillaNotificacion plantilla = plantilla(
                "DevolucionAprobada", "PUSH", "Devolución aprobada", "ok");
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("DevolucionAprobada", "PUSH"))
                .thenReturn(Optional.of(plantilla));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("DevolucionAprobada", "EMAIL"))
                .thenReturn(Optional.of(plantilla));
        when(notificacionRepository.existsByIdUsuarioAndClaveIdempotencia(
                CLIENTE_1, "DevolucionAprobada:PUSH:" + DEVOLUCION_ID)).thenReturn(true);
        when(notificacionRepository.existsByIdUsuarioAndClaveIdempotencia(
                CLIENTE_1, "DevolucionAprobada:EMAIL:" + DEVOLUCION_ID)).thenReturn(true);

        service.procesarDevolucionAprobada(devolucion());

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void procesarDevolucionAprobada_sinPlantillas_noPersiste() {
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("DevolucionAprobada", "PUSH"))
                .thenReturn(Optional.empty());
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("DevolucionAprobada", "EMAIL"))
                .thenReturn(Optional.empty());

        service.procesarDevolucionAprobada(devolucion());

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void procesarDevolucionAprobada_incompleta_seIgnora() {
        service.procesarDevolucionAprobada(new DevolucionAprobadaDTO(
                "m-4", DEVOLUCION_ID, "entrada-1", null, EVENTO_ID,
                BigDecimal.TEN, "ARS", Instant.now()));

        verifyNoInteractions(notificacionRepository, plantillaRepository);
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
        assertTrue(n.getClaveMensaje().startsWith("ADMIN:"));
    }

    private DevolucionAprobadaDTO devolucion() {
        return new DevolucionAprobadaDTO(
                "m-4", DEVOLUCION_ID, "entrada-1", CLIENTE_1, EVENTO_ID,
                new BigDecimal("95000.00"), "ARS", Instant.now());
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
        return evento(tipo, "m-1");
    }

    private EventoCambioDTO evento(String tipo, String messageId) {
        return new EventoCambioDTO(
                messageId, EVENTO_ID, tipo, "Rock", LocalDateTime.of(2026, 12, 20, 21, 0), null,
                Instant.parse("2026-11-13T19:20:00Z"));
    }

    private EventoCambioDTO eventoPublicado() {
        return new EventoCambioDTO(
                "m-3", EVENTO_ID, "EventoPublicado", "Rock",
                LocalDateTime.of(2026, 12, 20, 21, 0), null,
                Instant.parse("2026-11-13T19:20:00Z"), "Buenos Aires", List.of("Metallica"));
    }
}
