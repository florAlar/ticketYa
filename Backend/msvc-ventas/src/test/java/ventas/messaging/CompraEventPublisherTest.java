package ventas.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import ventas.config.RabbitConfig;

@ExtendWith(MockitoExtension.class)
class CompraEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CompraEventPublisher publisher;

    @Test
    void publicar_usaRoutingKeyYContratoEsperado() {
        publisher.publicar(new CompraConfirmadaEvent(
                "compra-1", "cli-1", "evt-1", new BigDecimal("1500.00"), "ARS", 2));

        ArgumentCaptor<CompraConfirmadaMessaging> captor =
                ArgumentCaptor.forClass(CompraConfirmadaMessaging.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE), eq("compra.confirmada"), captor.capture());

        CompraConfirmadaMessaging msg = captor.getValue();
        assertEquals("compra-1", msg.compraId());
        assertEquals("cli-1", msg.clienteId());
        assertEquals("evt-1", msg.eventoId());
        assertEquals(new BigDecimal("1500.00"), msg.total());
        assertEquals("ARS", msg.moneda());
        assertEquals(2, msg.cantidadEntradas());
        assertNotNull(msg.messageId());
        assertNotNull(msg.occurredAt());
    }
}
