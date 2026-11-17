package notificaciones.messaging;

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
 * Topología del consumidor. El exchange lo declara msvc-eventos con los mismos atributos
 * (topic, durable, no auto-delete); si cambian acá, el canal falla con PRECONDITION_FAILED.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "eventos.exchange";
    public static final String QUEUE = "notificaciones.eventos";
    public static final String QUEUE_COMPRAS = "notificaciones.compras";
    public static final String QUEUE_DEVOLUCIONES = "notificaciones.devoluciones";
    public static final String DLX = "notificaciones.dlx";
    public static final String DLQ = "notificaciones.eventos.dlq";
    public static final String DLQ_COMPRAS = "notificaciones.compras.dlq";
    public static final String DLQ_DEVOLUCIONES = "notificaciones.devoluciones.dlq";
    public static final String ROUTING_COMPRA = "compra.confirmada";
    public static final String ROUTING_DEVOLUCION = "devolucion.aprobada";

    @Bean
    TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange notificacionesDlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue colaEventos() {
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
    Queue colaCompras() {
        return QueueBuilder.durable(QUEUE_COMPRAS)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ_COMPRAS)
                .build();
    }

    @Bean
    Queue colaDlqCompras() {
        return QueueBuilder.durable(DLQ_COMPRAS).build();
    }

    @Bean
    Queue colaDevoluciones() {
        return QueueBuilder.durable(QUEUE_DEVOLUCIONES)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ_DEVOLUCIONES)
                .build();
    }

    @Bean
    Queue colaDlqDevoluciones() {
        return QueueBuilder.durable(DLQ_DEVOLUCIONES).build();
    }

    @Bean
    Binding bindingEventos(@Qualifier("colaEventos") Queue cola,
                           @Qualifier("eventosExchange") TopicExchange exchange) {
        return BindingBuilder.bind(cola).to(exchange).with("evento.#");
    }

    @Bean
    Binding bindingCompras(@Qualifier("colaCompras") Queue cola,
                           @Qualifier("eventosExchange") TopicExchange exchange) {
        return BindingBuilder.bind(cola).to(exchange).with("compra.#");
    }

    @Bean
    Binding bindingDevoluciones(@Qualifier("colaDevoluciones") Queue cola,
                                @Qualifier("eventosExchange") TopicExchange exchange) {
        return BindingBuilder.bind(cola).to(exchange).with("devolucion.#");
    }

    @Bean
    Binding bindingDlq(@Qualifier("colaDlq") Queue cola,
                       @Qualifier("notificacionesDlx") DirectExchange exchange) {
        return BindingBuilder.bind(cola).to(exchange).with(DLQ);
    }

    @Bean
    Binding bindingDlqCompras(@Qualifier("colaDlqCompras") Queue cola,
                              @Qualifier("notificacionesDlx") DirectExchange exchange) {
        return BindingBuilder.bind(cola).to(exchange).with(DLQ_COMPRAS);
    }

    @Bean
    Binding bindingDlqDevoluciones(@Qualifier("colaDlqDevoluciones") Queue cola,
                                   @Qualifier("notificacionesDlx") DirectExchange exchange) {
        return BindingBuilder.bind(cola).to(exchange).with(DLQ_DEVOLUCIONES);
    }

    @Bean
    JacksonJsonMessageConverter jsonConverter() {
        return new JacksonJsonMessageConverter();
    }
}
