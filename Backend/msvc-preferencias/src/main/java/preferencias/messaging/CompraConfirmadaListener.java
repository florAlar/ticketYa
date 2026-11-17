package preferencias.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import preferencias.config.RabbitConfig;
import preferencias.service.InteraccionService;

/**
 * Consume las compras confirmadas publicadas por msvc-ventas y las guarda en el historial.
 * El upsert por {@code compraId} hace la ingesta idempotente ante reentregas de RabbitMQ.
 */
@Component
public class CompraConfirmadaListener {

    private static final Logger log = LoggerFactory.getLogger(CompraConfirmadaListener.class);

    private final InteraccionService interaccionService;

    public CompraConfirmadaListener(InteraccionService interaccionService) {
        this.interaccionService = interaccionService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onCompraConfirmada(CompraConfirmadaDTO compra) {
        log.info("Compra confirmada recibida: compraId={} clienteId={} eventoId={}",
                compra != null ? compra.compraId() : null,
                compra != null ? compra.clienteId() : null,
                compra != null ? compra.eventoId() : null);
        interaccionService.registrarCompra(compra);
    }
}
