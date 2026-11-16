package eventos.Messaging;

import eventos.RabbitConfig.RabbitConfig;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventChangePublisher {

    private static final Logger log = LoggerFactory.getLogger(EventChangePublisher.class);

    private static final Map<String, String> TIPOS = Map.of(
            "CANCELADO", "EventoCancelado",
            "SUSPENDIDO", "EventoSuspendido",
            "MODIFICADO", "EventoModificado",
            "PUBLICADO", "EventoPublicado");

    private static final Map<String, String> ROUTING_KEYS = Map.of(
            "CANCELADO", "evento.cancelado",
            "SUSPENDIDO", "evento.suspendido",
            "MODIFICADO", "evento.modificado",
            "PUBLICADO", "evento.publicado");

    private final RabbitTemplate rabbitTemplate;

    public EventChangePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(EventChangeEvent e) {
        String estado = e.tipo() == null ? "" : e.tipo().toUpperCase(Locale.ROOT);
        String routingKey = ROUTING_KEYS.get(estado);
        if (routingKey == null) {
            log.warn("Estado sin evento de dominio asociado, no se publica: {}", e.tipo());
            return;
        }
        EventMessaging msg = new EventMessaging(
                UUID.randomUUID().toString(),
                e.eventoId(),
                TIPOS.get(estado),
                e.nombre(),
                e.fecha(),
                e.cambios(),
                Instant.now(),
                e.ciudad(),
                e.artistas());
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, msg);
            log.info("Evento publicado: tipo={} eventoId={} routingKey={} messageId={}",
                    msg.getTipo(), msg.getEventoId(), routingKey, msg.getMessageId());
        } catch (RuntimeException ex) {
            // El cambio ya está commiteado en la base: se registra y se sigue (best-effort).
            log.error("No se pudo publicar {} para evento {}: {}",
                    msg.getTipo(), msg.getEventoId(), ex.getMessage());
        }
    }
}
