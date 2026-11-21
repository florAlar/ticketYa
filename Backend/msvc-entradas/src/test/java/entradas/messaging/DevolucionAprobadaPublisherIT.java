package entradas.messaging;

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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
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

/**
 * IT del publicador de {@code devolucion.aprobada} con RabbitMQ y MySQL reales.
 *
 * <p>Comprueba que el aviso sale solo después del commit de la aprobación y que un rollback no
 * publica nada. Es el borde que conecta msvc-entradas con msvc-notificaciones.
 *
 * <p>Se ejecuta con {@code mvn -Pit verify}; requiere Docker.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers(disabledWithoutDocker = true)
class DevolucionAprobadaPublisherIT {

    static final String QUEUE = "devolucion.aprobada.it";

    @Container
    static final MySQLContainer MYSQL =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("entradas")
                    .withUsername("ticketya")
                    .withPassword("ticketya_dev")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    "../infrastructure/mysql/entradas/init.sql"),
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
        DevolucionAprobadaEvent evento = evento("devolucion-commit");

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(evento);
            assertNull(rabbitTemplate.receive(QUEUE, 300),
                    "No debe publicarse antes del commit");
        });

        Message mensaje = rabbitTemplate.receive(QUEUE, 5_000);
        assertNotNull(mensaje, "El mensaje no llegó después del commit");
        DevolucionAprobadaMessaging wire =
                objectMapper.readValue(mensaje.getBody(), DevolucionAprobadaMessaging.class);
        assertEquals("devolucion-commit", wire.devolucionId());
        assertEquals("entrada-1", wire.entradaId());
        assertEquals("cliente-1", wire.clienteId());
        assertEquals("evento-1", wire.eventoId());
        assertEquals(new BigDecimal("95000.00"), wire.monto());
        assertEquals("ARS", wire.moneda());
        assertNotNull(wire.messageId());
        assertNotNull(wire.occurredAt());
    }

    @Test
    void noPublicaSiLaTransaccionHaceRollback() {
        DevolucionAprobadaEvent evento = evento("devolucion-rollback");

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(evento);
            status.setRollbackOnly();
        });

        assertNull(rabbitTemplate.receive(QUEUE, 1_000),
                "No debe publicarse si la transacción hizo rollback");
    }

    private static DevolucionAprobadaEvent evento(String devolucionId) {
        return new DevolucionAprobadaEvent(
                devolucionId,
                "entrada-1",
                "cliente-1",
                "evento-1",
                new BigDecimal("95000.00"));
    }

    @TestConfiguration
    static class ColaDePrueba {

        @Bean
        Queue devolucionAprobadaItQueue() {
            return QueueBuilder.durable(QUEUE).build();
        }

        @Bean
        Binding devolucionAprobadaItBinding(
                Queue devolucionAprobadaItQueue, TopicExchange eventosExchange) {
            return BindingBuilder.bind(devolucionAprobadaItQueue)
                    .to(eventosExchange)
                    .with(DevolucionEventPublisher.ROUTING_KEY);
        }
    }
}
