package gateway.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class GatewayControllerTest {

    @Test
    void ok_devuelveTextoEstado() {
        GatewayController controller = new GatewayController();
        String res = controller.ok();

        assertEquals("api gateway ok!", res);
    }
}
