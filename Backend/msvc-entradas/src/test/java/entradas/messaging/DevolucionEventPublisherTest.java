package entradas.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import entradas.config.RabbitConfig;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class DevolucionEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DevolucionEventPublisher publisher;

    @Test
    void publicar_usaRoutingKeyYContratoEsperado() {
        publisher.publicar(new DevolucionAprobadaEvent(
                "dev-1", "ent-1", "cli-1", "evt-1", new BigDecimal("95000.00")));

        ArgumentCaptor<DevolucionAprobadaMessaging> captor =
                ArgumentCaptor.forClass(DevolucionAprobadaMessaging.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE), eq("devolucion.aprobada"), captor.capture());

        DevolucionAprobadaMessaging msg = captor.getValue();
        assertEquals("dev-1", msg.devolucionId());
        assertEquals("ent-1", msg.entradaId());
        assertEquals("cli-1", msg.clienteId());
        assertEquals("evt-1", msg.eventoId());
        assertEquals(new BigDecimal("95000.00"), msg.monto());
        assertEquals("ARS", msg.moneda());
        assertNotNull(msg.messageId());
        assertNotNull(msg.occurredAt());
    }
}
