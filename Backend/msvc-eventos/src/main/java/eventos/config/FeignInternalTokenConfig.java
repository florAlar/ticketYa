package eventos.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Propaga {@code X-Internal-Token} en las llamadas Feign a msvc-ventas. Se aplica
 * solo a {@code VentasClient} mediante {@code @FeignClient(configuration = ...)}
 * (sin {@code @Configuration}) para no enviar el token al geocodificador externo.
 */
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
