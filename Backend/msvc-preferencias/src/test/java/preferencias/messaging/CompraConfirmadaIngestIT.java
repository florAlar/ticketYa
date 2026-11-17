package preferencias.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import preferencias.clients.EventosClient;
import preferencias.clients.EventosClient.EventoDetalleDTO;
import preferencias.clients.EventosClient.LugarDetalleDTO;
import preferencias.clients.EventosClient.ParticipanteDTO;
import preferencias.config.RabbitConfig;
import preferencias.document.InteraccionEvento;
import preferencias.repository.InteraccionEventoRepository;

/**
 * IT de la ingesta real: RabbitMQ publica {@code compra.confirmada}, el listener la consume,
 * se enriquece por Feign (mockeado) y se upsertea en MongoDB. Verifica idempotencia.
 * Se ejecuta con {@code mvn -Pit verify}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class CompraConfirmadaIngestIT {

    @Container
    static final GenericContainer<?> MONGO = new GenericContainer<>(DockerImageName.parse("mongo:7.0"))
            .withExposedPorts(27017)
            .waitingFor(Wait.forListeningPort());

    @Container
    static final GenericContainer<?> RABBIT = new GenericContainer<>(
            DockerImageName.parse("rabbitmq:3.13-management"))
            .withExposedPorts(5672)
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        String mongoUri = "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017)
                + "/preferencias_it";
        registry.add("spring.mongodb.uri", () -> mongoUri);
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
        registry.add("spring.data.mongodb.auto-index-creation", () -> true);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @MockitoBean
    private EventosClient eventosClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private InteraccionEventoRepository repository;

    private static final String COMPRA = "f0000000-0000-4000-8000-0000000000aa";
    private static final String CLIENTE = "b0000000-0000-4000-8000-000000000001";
    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        when(eventosClient.getEvento(EVENTO)).thenReturn(detalle());
    }

    @Test
    void compraConfirmada_sePersisteConSnapshotYEsIdempotente() throws Exception {
        CompraConfirmadaDTO compra = new CompraConfirmadaDTO(
                "msg-1", COMPRA, CLIENTE, EVENTO,
                new BigDecimal("95000"), "ARS", 2, Instant.now());

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_COMPRA, compra);

        InteraccionEvento guardada = esperar(COMPRA, 20_000);
        assertNotNull(guardada, "La interacción no llegó a Mongo");
        assertEquals("Metallica — M72", guardada.getNombreEvento());
        assertEquals("RECITAL", guardada.getCategoria());
        assertEquals("Buenos Aires", guardada.getCiudad());
        assertEquals(List.of("Metallica", "Gojira"), guardada.getArtistas());
        assertEquals(2, guardada.getCantidadEntradas());

        // Reentrega: mismo idCompra → no duplica.
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_COMPRA, compra);
        Thread.sleep(1500);

        assertEquals(1, repository.count());
    }

    private InteraccionEvento esperar(String idCompra, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            var encontrada = repository.findByIdCompra(idCompra);
            if (encontrada.isPresent()) {
                return encontrada.get();
            }
            Thread.sleep(250);
        }
        fail("Timeout esperando la interacción " + idCompra);
        return null;
    }

    private static EventoDetalleDTO detalle() {
        EventoDetalleDTO dto = new EventoDetalleDTO();
        dto.setId(EVENTO);
        dto.setNombre("Metallica — M72");
        dto.setTipo("RECITAL");
        dto.setFechaInicio(LocalDateTime.of(2027, 3, 15, 21, 0));
        LugarDetalleDTO lugar = new LugarDetalleDTO();
        lugar.setCiudad("Buenos Aires");
        dto.setLugar(lugar);
        dto.setParticipantes(List.of(participante("Metallica"), participante("Gojira")));
        return dto;
    }

    private static ParticipanteDTO participante(String nombre) {
        ParticipanteDTO p = new ParticipanteDTO();
        p.setNombre(nombre);
        return p;
    }
}
