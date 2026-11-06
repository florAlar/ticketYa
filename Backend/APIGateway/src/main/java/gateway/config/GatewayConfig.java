package gateway.config;

/**
 * Config general del gateway.
 * CORS y security viven en {@link SecurityConfig} (patrón gatewayejemplo).
 * Rutas: {@code application.yml} → {@code spring.cloud.gateway.server.webmvc.routes}.
 */
public final class GatewayConfig {

    private GatewayConfig() {
    }
}
