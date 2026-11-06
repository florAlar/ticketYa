package gateway.config;

/**
 * Rutas de negocio del gateway (API-05 / E1-GW-01).
 *
 * <p>Definidas en {@code application.yml} bajo
 * {@code spring.cloud.gateway.server.webmvc.routes} (Spring Cloud Gateway MVC).
 *
 * <p>Orden: excepciones {@code /api/eventos/{id}/disponibilidad|fila|validaciones|recomendaciones}
 * antes del catch-all {@code /api/eventos/**} → msvc-eventos. Prefijo externo {@code /api}
 * se elimina con {@code StripPrefix=1}.
 *
 * <p>Esta clase queda como ancla de paquete/documentación; no registra beans extra.
 */
public final class RouteConfig {

    private RouteConfig() {
    }
}
