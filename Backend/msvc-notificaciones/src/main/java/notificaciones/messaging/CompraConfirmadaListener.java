package notificaciones.messaging;

import notificaciones.service.NotificacionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consume pagos aprobados publicados por msvc-ventas en {@code compra.confirmada}. */
@Component
public class CompraConfirmadaListener {

    private final NotificacionService notificacionService;

    public CompraConfirmadaListener(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_COMPRAS)
    public void onCompraConfirmada(CompraConfirmadaDTO compra) {
        notificacionService.procesarCompraConfirmada(compra);
    }
}
