package notificaciones.messaging;

import notificaciones.service.NotificacionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EventoCambioListener {

    private final NotificacionService notificacionService;

    public EventoCambioListener(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onEventoCambio(EventoCambioDTO evento) {
        notificacionService.procesarEvento(evento);
    }
}
