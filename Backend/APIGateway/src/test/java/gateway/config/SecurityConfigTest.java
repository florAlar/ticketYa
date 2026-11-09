package gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SecurityConfigTest {

    @Test
    void securityConfig_instanciaCorrecta() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins",
                "http://localhost:5173,http://localhost:3000");
        assertNotNull(config);
    }

    @Test
    void corsOrigins_configurables() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins",
                "http://localhost:5173,http://localhost:3000");
        String origins = (String) ReflectionTestUtils.getField(config, "allowedOrigins");
        assertEquals("http://localhost:5173,http://localhost:3000", origins);
    }
}
