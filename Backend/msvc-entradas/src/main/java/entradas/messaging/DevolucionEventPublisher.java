package entradas.messaging;

import entradas.config.RabbitConfig;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica {@code devolucion.aprobada} después del commit: si la transacción de aprobación falla,
 * el cliente no recibe un aviso de un reintegro que no ocurrió.
 */
@Component
public class DevolucionEventPublisher {

    public static final String ROUTING_KEY = "devolucion.aprobada";
    static final String MONEDA = "ARS";

    private static final Logger log = LoggerFactory.getLogger(DevolucionEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public DevolucionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(DevolucionAprobadaEvent e) {
        DevolucionAprobadaMessaging msg = new DevolucionAprobadaMessaging(
                UUID.randomUUID().toString(),
                e.devolucionId(),
                e.entradaId(),
                e.clienteId(),
                e.eventoId(),
                e.monto(),
                MONEDA,
                Instant.now());
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, ROUTING_KEY, msg);
            log.info("Devolución aprobada publicada: devolucionId={} clienteId={} eventoId={} messageId={}",
                    msg.devolucionId(), msg.clienteId(), msg.eventoId(), msg.messageId());
        } catch (RuntimeException ex) {
            // La devolución ya está aprobada y commiteada: no se rompe la respuesta si el broker cae.
            log.error("No se pudo publicar devolucion.aprobada para devolución {}: {}",
                    msg.devolucionId(), ex.getMessage());
        }
    }
}
