package eventos.Messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import eventos.RabbitConfig.RabbitConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class EventChangePublisherTest {

    private static final String EVENTO_ID = "e0000000-0000-4000-8000-000000000001";
    private static final LocalDateTime FECHA_EVENTO = LocalDateTime.of(2026, 12, 20, 21, 0);

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventChangePublisher publisher;

    @Test
    void publicar_cancelado_usaRoutingKeyYTipoCanonicos() {
        publisher.publicar(new EventChangeEvent(EVENTO_ID, "CANCELADO", "Rock", FECHA_EVENTO, null));

        ArgumentCaptor<EventMessaging> captor = ArgumentCaptor.forClass(EventMessaging.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq("evento.cancelado"), captor.capture());

        EventMessaging msg = captor.getValue();
        assertEquals("EventoCancelado", msg.getTipo());
        assertEquals(EVENTO_ID, msg.getEventoId());
        assertEquals(FECHA_EVENTO, msg.getFecha());
        assertNotNull(msg.getMessageId());
        assertNotNull(msg.getOccurredAt());
    }

    @Test
    void publicar_suspendido_usaRoutingKeyYTipoCanonicos() {
        publisher.publicar(new EventChangeEvent(EVENTO_ID, "SUSPENDIDO", "Rock", FECHA_EVENTO, null));

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq("evento.suspendido"), any(EventMessaging.class));
    }

    @Test
    void publicar_estadoSinEvento_noPublica() {
        publisher.publicar(new EventChangeEvent(EVENTO_ID, "AGOTADO", "Rock", FECHA_EVENTO, null));

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
