package ventas.queue;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/** Tests de clases de cola (vacías en E1). */
class VentaQueueTest {

    @Test
    void productorSeInstancia() {
        assertNotNull(new VentaQueueProducer());
    }

    @Test
    void consumidorSeInstancia() {
        assertNotNull(new VentaQueueConsumer());
    }
}
