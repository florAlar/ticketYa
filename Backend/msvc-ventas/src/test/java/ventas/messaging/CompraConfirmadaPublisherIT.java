package ventas.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import ventas.config.RabbitConfig;

/**
 * IT del publicador de {@code compra.confirmada} con RabbitMQ y MySQL reales.
 *
 * <p>Comprueba la semántica del {@code @TransactionalEventListener(AFTER_COMMIT)}: el mensaje se
 * publica al commitear y no se publica si la transacción hace rollback.
 *
 * <p>Se ejecuta con {@code mvn -Pit verify}; requiere Docker.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "ticketya.fila.auto.enabled=false",
                "ticketya.ventas.emision-job-ms=3600000",
                "ticketya.ventas.expire-job-ms=3600000"
        })
@Testcontainers(disabledWithoutDocker = true)
class CompraConfirmadaPublisherIT {

    static final String QUEUE = "compra.confirmada.it";

    @Container
    static final MySQLContainer MYSQL =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("ventas")
                    .withUsername("ticketya")
                    .withPassword("ticketya_dev")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    "../infrastructure/mysql/ventas/init.sql"),
                            "/docker-entrypoint-initdb.d/init.sql");

    @Container
    static final GenericContainer<?> RABBIT =
            new GenericContainer<>(DockerImageName.parse("rabbitmq:3.13-management"))
                    .withExposedPorts(5672)
                    .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        rabbitAdmin.initialize();
        rabbitAdmin.purgeQueue(QUEUE);
    }

    @Test
    void publicaDespuesDelCommit() throws Exception {
        CompraConfirmadaEvent evento = evento("compra-commit");

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(evento);
            assertNull(rabbitTemplate.receive(QUEUE, 300),
                    "No debe publicarse antes del commit");
        });

        Message mensaje = rabbitTemplate.receive(QUEUE, 5_000);
        assertNotNull(mensaje, "El mensaje no llegó después del commit");
        CompraConfirmadaMessaging wire =
                objectMapper.readValue(mensaje.getBody(), CompraConfirmadaMessaging.class);
        assertEquals("compra-commit", wire.compraId());
        assertEquals("cliente-1", wire.clienteId());
        assertEquals("evento-1", wire.eventoId());
        assertEquals(new BigDecimal("95000.00"), wire.total());
        assertEquals("ARS", wire.moneda());
        assertEquals(2, wire.cantidadEntradas());
        assertNotNull(wire.messageId());
        assertNotNull(wire.occurredAt());
    }

    @Test
    void noPublicaSiLaTransaccionHaceRollback() {
        CompraConfirmadaEvent evento = evento("compra-rollback");

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(evento);
            status.setRollbackOnly();
        });

        assertNull(rabbitTemplate.receive(QUEUE, 1_000),
                "No debe publicarse si la transacción hizo rollback");
    }

    private static CompraConfirmadaEvent evento(String compraId) {
        return new CompraConfirmadaEvent(
                compraId,
                "cliente-1",
                "evento-1",
                new BigDecimal("95000.00"),
                "ARS",
                2);
    }

    @TestConfiguration
    static class ColaDePrueba {

        @Bean
        Queue compraConfirmadaItQueue() {
            return QueueBuilder.durable(QUEUE).build();
        }

        @Bean
        Binding compraConfirmadaItBinding(
                Queue compraConfirmadaItQueue, TopicExchange eventosExchange) {
            return BindingBuilder.bind(compraConfirmadaItQueue)
                    .to(eventosExchange)
                    .with(CompraEventPublisher.ROUTING_KEY);
        }
    }
}
