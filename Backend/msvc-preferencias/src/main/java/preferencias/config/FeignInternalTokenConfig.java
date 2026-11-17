package preferencias.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Propaga {@code X-Internal-Token} en las llamadas Feign. Lo usa, por ejemplo,
 * {@code VentasClient} para el backfill ({@code GET /internas/compras}).
 * Enviarlo también a endpoints públicos (eventos) es inofensivo.
 */
@Configuration
public class FeignInternalTokenConfig {

    @Bean
    RequestInterceptor internalTokenInterceptor(
            @Value("${ticketya.interno.token:}") String token) {
        String value = token == null ? "" : token.trim();
        return template -> {
            if (!value.isEmpty()) {
                template.header("X-Internal-Token", value);
            }
        };
    }
}
