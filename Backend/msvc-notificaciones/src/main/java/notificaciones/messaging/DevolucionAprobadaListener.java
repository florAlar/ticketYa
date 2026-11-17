package notificaciones.messaging;

import notificaciones.service.NotificacionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consume devoluciones aprobadas publicadas por msvc-entradas en {@code devolucion.aprobada}. */
@Component
public class DevolucionAprobadaListener {

    private final NotificacionService notificacionService;

    public DevolucionAprobadaListener(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_DEVOLUCIONES)
    public void onDevolucionAprobada(DevolucionAprobadaDTO devolucion) {
        notificacionService.procesarDevolucionAprobada(devolucion);
    }
}
