package ventas.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exchange compartido con msvc-eventos. Se declara con los mismos atributos (topic, durable,
 * no auto-delete) para no chocar con PRECONDITION_FAILED si arranca primero.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "eventos.exchange";

    @Bean
    TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    JacksonJsonMessageConverter jsonConverter() {
        return new JacksonJsonMessageConverter();
    }
}
