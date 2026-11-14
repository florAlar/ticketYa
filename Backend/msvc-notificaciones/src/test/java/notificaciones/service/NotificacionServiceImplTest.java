package notificaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import notificaciones.clients.VentasClient;
import notificaciones.entity.Notificacion;
import notificaciones.entity.PlantillaNotificacion;
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
    private static final String CLIENTE_1 = "c0000000-0000-4000-8000-000000000001";
    private static final String CLIENTE_2 = "c0000000-0000-4000-8000-000000000002";

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private PlantillaNotificacionRepository plantillaRepository;

    @Mock
    private VentasClient ventasClient;

    @InjectMocks
    private NotificacionServiceImpl service;

    private PlantillaNotificacion plantilla;

    @BeforeEach
    void setUp() {
        plantilla = new PlantillaNotificacion();
        plantilla.setIdPlantilla("n0000000-0000-4000-8000-000000000002");
        plantilla.setTipoEvento("EventoCancelado");
        plantilla.setCanal("PUSH");
        plantilla.setAsunto("Evento cancelado");
        plantilla.setContenido("El evento fue cancelado. Gestionaremos el reintegro.");
        plantilla.setActiva(true);
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
        assertEquals(plantilla, primera.getPlantilla());
    }

    @Test
    void procesarEvento_saltaClientesYaNotificados() {
        when(ventasClient.clientesAprobadosPorEvento(EVENTO_ID))
                .thenReturn(List.of(CLIENTE_1, CLIENTE_2));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("EventoCancelado", "PUSH"))
                .thenReturn(Optional.of(plantilla));
        when(notificacionRepository.existsByIdUsuarioAndIdEventoOrigenAndTipo(
                CLIENTE_1, EVENTO_ID, "EventoCancelado")).thenReturn(true);
        when(notificacionRepository.existsByIdUsuarioAndIdEventoOrigenAndTipo(
                CLIENTE_2, EVENTO_ID, "EventoCancelado")).thenReturn(false);
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
    void procesarEvento_otroTipo_noConsultaVentas() {
        service.procesarEvento(evento("EventoSuspendido"));

        verifyNoInteractions(ventasClient, notificacionRepository, plantillaRepository);
    }

    @Test
    void procesarEvento_sinPlantilla_lanzaIllegalState() {
        when(ventasClient.clientesAprobadosPorEvento(EVENTO_ID)).thenReturn(List.of(CLIENTE_1));
        when(plantillaRepository.findFirstByTipoEventoAndCanalAndActivaTrue("EventoCancelado", "PUSH"))
                .thenReturn(Optional.empty());
        when(plantillaRepository.findFirstByTipoEventoAndActivaTrue("EventoCancelado"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.procesarEvento(evento("EventoCancelado")));
    }

    private EventoCambioDTO evento(String tipo) {
        return new EventoCambioDTO(
                "m-1", EVENTO_ID, tipo, "Rock", LocalDateTime.of(2026, 12, 20, 21, 0), null,
                java.time.Instant.parse("2026-11-13T19:20:00Z"));
    }
}
