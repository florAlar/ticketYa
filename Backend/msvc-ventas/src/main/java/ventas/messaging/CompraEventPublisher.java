package ventas.messaging;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ventas.config.RabbitConfig;

/** Publica la compra confirmada después del commit: si la transacción falla, no se notifica. */
@Component
public class CompraEventPublisher {

    public static final String ROUTING_KEY = "compra.confirmada";

    private static final Logger log = LoggerFactory.getLogger(CompraEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public CompraEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(CompraConfirmadaEvent e) {
        CompraConfirmadaMessaging msg = new CompraConfirmadaMessaging(
                UUID.randomUUID().toString(),
                e.compraId(),
                e.clienteId(),
                e.eventoId(),
                e.total(),
                e.moneda(),
                e.cantidadEntradas(),
                Instant.now());
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, ROUTING_KEY, msg);
            log.info("Compra confirmada publicada: compraId={} clienteId={} eventoId={} messageId={}",
                    msg.compraId(), msg.clienteId(), msg.eventoId(), msg.messageId());
        } catch (RuntimeException ex) {
            // El pago ya está confirmado: no se debe romper la respuesta si el broker no responde.
            log.error("No se pudo publicar compra.confirmada para compra {}: {}",
                    msg.compraId(), ex.getMessage());
        }
    }
}
