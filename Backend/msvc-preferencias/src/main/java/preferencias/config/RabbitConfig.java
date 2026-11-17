package preferencias.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología del consumidor de compras confirmadas. El exchange lo declara msvc-eventos/ventas con
 * los mismos atributos (topic, durable, no auto-delete); si cambian acá, el canal falla con
 * PRECONDITION_FAILED.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "eventos.exchange";
    public static final String QUEUE = "preferencias.compras";
    public static final String DLX = "preferencias.dlx";
    public static final String DLQ = "preferencias.compras.dlq";
    public static final String ROUTING_COMPRA = "compra.confirmada";

    @Bean
    TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange preferenciasDlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue colaCompras() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    Queue colaDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding bindingCompras(
            @Qualifier("colaCompras") Queue cola,
            @Qualifier("eventosExchange") TopicExchange exchange) {
        return BindingBuilder.bind(cola).to(exchange).with(ROUTING_COMPRA);
    }

    @Bean
    Binding bindingDlq(
            @Qualifier("colaDlq") Queue cola,
            @Qualifier("preferenciasDlx") DirectExchange exchange) {
        return BindingBuilder.bind(cola).to(exchange).with(DLQ);
    }

    @Bean
    JacksonJsonMessageConverter jsonConverter() {
        return new JacksonJsonMessageConverter();
    }
}
