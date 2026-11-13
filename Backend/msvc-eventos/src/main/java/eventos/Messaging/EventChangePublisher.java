package eventos.Messaging;

import eventos.RabbitConfig.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventChangePublisher {

    private final RabbitTemplate rabbitTemplate;

    public EventChangePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(EventChangeEvent e) {
        EventMessaging msg = new EventMessaging(
                e.eventoId(), e.tipo(), e.nombre(), e.fecha(), e.cambios());
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE, "evento." + e.tipo().toLowerCase(), msg);
    }
}