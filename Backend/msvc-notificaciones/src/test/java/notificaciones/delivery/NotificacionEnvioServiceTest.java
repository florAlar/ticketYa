package notificaciones.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import notificaciones.entity.IntentoEnvio;
import notificaciones.entity.Notificacion;
import notificaciones.repository.IntentoEnvioRepository;
import notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificacionEnvioServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private IntentoEnvioRepository intentoEnvioRepository;

    @Mock
    private NotificacionProvider pushProvider;

    private NotificacionEnvioService service;
    private Notificacion notificacion;

    @BeforeEach
    void setUp() {
        lenient().when(pushProvider.canal()).thenReturn("PUSH");
        service = new NotificacionEnvioService(
                notificacionRepository, intentoEnvioRepository, List.of(pushProvider));
        ReflectionTestUtils.setField(service, "maxIntentos", 3);
        ReflectionTestUtils.setField(service, "lote", 50);

        notificacion = new Notificacion();
        notificacion.setIdNotificacion("noti-1");
        notificacion.setIdUsuario("cli-1");
        notificacion.setTipo("EventoCancelado");
        notificacion.setCanal("PUSH");
        notificacion.setAsunto("Evento cancelado");
        notificacion.setContenido("Se canceló");
        notificacion.setEstado("PENDIENTE");
        notificacion.setFechaCreacion(LocalDateTime.now());
        notificacion.setLeida(false);
    }

    @Test
    void pendienteConEnvioOk_quedaEnviadaYRegistraIntento() {
        when(notificacionRepository.findPendientesParaEnvio(any())).thenReturn(List.of(notificacion));
        when(intentoEnvioRepository.countByNotificacion_IdNotificacion("noti-1")).thenReturn(0L);
        when(pushProvider.enviar(notificacion))
                .thenReturn(NotificacionProvider.ResultadoEnvio.ok("mock-push-1"));

        service.procesarPendientes();

        assertEquals("ENVIADA", notificacion.getEstado());
        assertNotNull(notificacion.getFechaEnvio());
        verify(notificacionRepository).save(notificacion);

        ArgumentCaptor<IntentoEnvio> captor = ArgumentCaptor.forClass(IntentoEnvio.class);
        verify(intentoEnvioRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getNumeroIntento());
        assertEquals("OK", captor.getValue().getResultado());
        assertEquals("mock-push-1", captor.getValue().getDetalleError());
    }

    @Test
    void primerFallo_quedaPendienteParaElProximoCiclo() {
        when(notificacionRepository.findPendientesParaEnvio(any())).thenReturn(List.of(notificacion));
        when(intentoEnvioRepository.countByNotificacion_IdNotificacion("noti-1")).thenReturn(0L);
        when(pushProvider.enviar(notificacion)).thenThrow(new RuntimeException("proveedor caído"));

        service.procesarPendientes();

        assertEquals("PENDIENTE", notificacion.getEstado());
        assertNull(notificacion.getFechaEnvio());
        ArgumentCaptor<IntentoEnvio> captor = ArgumentCaptor.forClass(IntentoEnvio.class);
        verify(intentoEnvioRepository).save(captor.capture());
        assertEquals("ERROR", captor.getValue().getResultado());
        assertEquals("proveedor caído", captor.getValue().getDetalleError());
    }

    @Test
    void tercerFallo_quedaFallida() {
        when(notificacionRepository.findPendientesParaEnvio(any())).thenReturn(List.of(notificacion));
        when(intentoEnvioRepository.countByNotificacion_IdNotificacion("noti-1")).thenReturn(2L);
        when(pushProvider.enviar(notificacion)).thenThrow(new RuntimeException("boom"));

        service.procesarPendientes();

        assertEquals("FALLIDA", notificacion.getEstado());
        verify(notificacionRepository).save(notificacion);
    }

    @Test
    void maxIntentosAlcanzado_noVuelveAEnviar() {
        when(notificacionRepository.findPendientesParaEnvio(any())).thenReturn(List.of(notificacion));
        when(intentoEnvioRepository.countByNotificacion_IdNotificacion("noti-1")).thenReturn(3L);

        service.procesarPendientes();

        assertEquals("FALLIDA", notificacion.getEstado());
        verify(pushProvider, never()).enviar(any());
    }

    @Test
    void canalSinProveedor_registraError() {
        notificacion.setCanal("SMS");
        when(notificacionRepository.findPendientesParaEnvio(any())).thenReturn(List.of(notificacion));
        when(intentoEnvioRepository.countByNotificacion_IdNotificacion("noti-1")).thenReturn(0L);

        service.procesarPendientes();

        assertEquals("PENDIENTE", notificacion.getEstado());
        ArgumentCaptor<IntentoEnvio> captor = ArgumentCaptor.forClass(IntentoEnvio.class);
        verify(intentoEnvioRepository).save(captor.capture());
        assertEquals("ERROR", captor.getValue().getResultado());
    }
}
