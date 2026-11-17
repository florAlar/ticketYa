package entradas.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exchange compartido con msvc-eventos / msvc-ventas / msvc-notificaciones.
 * Debe declararse con los mismos atributos (topic, durable, no auto-delete) para evitar
 * PRECONDITION_FAILED si este servicio arranca primero.
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
