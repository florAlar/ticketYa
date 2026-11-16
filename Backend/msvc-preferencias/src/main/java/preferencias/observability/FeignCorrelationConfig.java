package preferencias.observability;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** E2-NFR-03: propaga el correlation id del request actual en toda llamada Feign a otro servicio. */
@Configuration
public class FeignCorrelationConfig {

    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return template -> {
            String id = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (id != null && !id.isBlank()) {
                template.header(CorrelationIdFilter.HEADER, id);
            }
        };
    }
}
