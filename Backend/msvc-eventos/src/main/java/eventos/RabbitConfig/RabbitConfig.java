package eventos.RabbitConfig;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "eventos.exchange";

    @Bean
    TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    JacksonJsonMessageConverter jsonConverter() {
        return new JacksonJsonMessageConverter();
    }
}