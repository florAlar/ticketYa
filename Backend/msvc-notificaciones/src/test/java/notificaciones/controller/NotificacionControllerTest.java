package notificaciones.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import notificaciones.dto.NotificacionPageResponseDTO;
import notificaciones.dto.NotificacionResponseDTO;
import notificaciones.entity.Notificacion;
import notificaciones.repository.NotificacionRepository;
import notificaciones.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NotificacionControllerTest {

    private static final String CLIENTE = "c0000000-0000-4000-8000-000000000001";
    private static final String CLAVE_MENSAJE = "EventoCancelado:e0000000-0000-4000-8000-000000000001:m-1";

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private NotificacionRepository notificacionRepository;

    private NotificacionController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificacionController(notificacionService, notificacionRepository);
    }

    @Test
    void listarPropias_devuelveUnaEntradaPorMensajeConSusCanales() {
        Notificacion push = notificacion("n-push", "PUSH");
        when(notificacionRepository.findVisiblesByIdUsuario(
                eq(CLIENTE), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(push)));
        when(notificacionRepository.countVisiblesNoLeidas(CLIENTE)).thenReturn(1L);
        when(notificacionRepository.findCanalesPorClaves(eq(CLIENTE), any()))
                .thenReturn(canales("PUSH", "EMAIL"));

        NotificacionPageResponseDTO respuesta =
                controller.listarPropias(CLIENTE, 0, 20, null);

        assertEquals(1, respuesta.getContent().size());
        assertEquals(1L, respuesta.getNoLeidas());
        NotificacionResponseDTO dto = respuesta.getContent().get(0);
        assertEquals("n-push", dto.getId());
        assertEquals(List.of("PUSH", "EMAIL"), dto.getCanales());
        verify(notificacionRepository)
                .findVisiblesByIdUsuario(eq(CLIENTE), any(PageRequest.class));
    }

    @Test
    void marcarLeida_actualizaPushYEmailDelMismoMensaje() {
        Notificacion push = notificacion("n-push", "PUSH");
        Notificacion email = notificacion("n-email", "EMAIL");
        when(notificacionRepository.findByIdNotificacionAndIdUsuario("n-push", CLIENTE))
                .thenReturn(Optional.of(push));
        when(notificacionRepository.findByIdUsuarioAndClaveMensaje(CLIENTE, CLAVE_MENSAJE))
                .thenReturn(List.of(push, email));
        when(notificacionRepository.findCanalesPorClaves(eq(CLIENTE), any()))
                .thenReturn(canales("PUSH", "EMAIL"));

        NotificacionResponseDTO dto = controller.marcarLeida(CLIENTE, "n-push");

        assertTrue(dto.getLeida());
        assertEquals(List.of("PUSH", "EMAIL"), dto.getCanales());
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(n -> Boolean.TRUE.equals(n.getLeida())));
        assertTrue(captor.getAllValues().stream().allMatch(n -> n.getFechaLectura() != null));
    }

    @Test
    void marcarLeida_yaLeida_noVuelveAGuardar() {
        Notificacion push = notificacion("n-push", "PUSH");
        push.setLeida(Boolean.TRUE);
        when(notificacionRepository.findByIdNotificacionAndIdUsuario("n-push", CLIENTE))
                .thenReturn(Optional.of(push));
        when(notificacionRepository.findByIdUsuarioAndClaveMensaje(CLIENTE, CLAVE_MENSAJE))
                .thenReturn(List.of(push));

        controller.marcarLeida(CLIENTE, "n-push");

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    private static List<Object[]> canales(String... canales) {
        List<Object[]> filas = new ArrayList<>();
        for (String canal : canales) {
            filas.add(new Object[] {CLAVE_MENSAJE, canal});
        }
        return filas;
    }

    private static Notificacion notificacion(String id, String canal) {
        Notificacion n = new Notificacion();
        n.setIdNotificacion(id);
        n.setIdUsuario(CLIENTE);
        n.setIdEventoOrigen("e0000000-0000-4000-8000-000000000001");
        n.setTipo("EventoCancelado");
        n.setCanal(canal);
        n.setClaveIdempotencia("EventoCancelado:" + canal
                + ":e0000000-0000-4000-8000-000000000001:m-1");
        n.setClaveMensaje(CLAVE_MENSAJE);
        n.setAsunto("Evento cancelado");
        n.setContenido("Se canceló");
        n.setEstado("ENVIADA");
        n.setLeida(Boolean.FALSE);
        n.setFechaCreacion(LocalDateTime.now());
        return n;
    }
}
