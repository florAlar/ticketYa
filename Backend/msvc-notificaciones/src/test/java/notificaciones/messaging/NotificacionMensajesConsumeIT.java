package notificaciones.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import notificaciones.clients.PreferenciasClient;
import notificaciones.clients.UsuariosClient;
import notificaciones.clients.VentasClient;
import notificaciones.entity.Notificacion;
import notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * IT de los consumidores de notificaciones con MySQL y RabbitMQ reales.
 *
 * <p>Publica los mismos contratos que producen eventos/ventas/entradas y verifica el efecto
 * observable en la base: filas PUSH/EMAIL, agrupación por mensaje e idempotencia. Los clientes
 * Feign remotos se mockean porque este test aísla el borde del consumidor.
 *
 * <p>Se ejecuta con {@code mvn -Pit verify}; requiere Docker.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "ticketya.notificaciones.envio.intervalo-ms=3600000")
@Testcontainers(disabledWithoutDocker = true)
class NotificacionMensajesConsumeIT {

    private static final String CLIENTE = "b0000000-0000-4000-8000-000000000001";
    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";
    private static final String COMPRA = "f0000000-0000-4000-8000-0000000000aa";
    private static final String DEVOLUCION = "aa000000-0000-4000-8000-000000000011";

    @Container
    static final MySQLContainer MYSQL =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("notificaciones")
                    .withUsername("ticketya")
                    .withPassword("ticketya_dev")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    "../infrastructure/mysql/notificaciones/init.sql"),
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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @MockitoBean
    private VentasClient ventasClient;

    @MockitoBean
    private PreferenciasClient preferenciasClient;

    @MockitoBean
    private UsuariosClient usuariosClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM intento_envio");
        jdbc.update("DELETE FROM notificacion");
        lenient().when(ventasClient.clientesAprobadosPorEvento(any()))
                .thenReturn(List.of(CLIENTE));
        lenient().when(preferenciasClient.preferenciasNotificaciones(any()))
                .thenReturn(new PreferenciasClient.PreferenciasNotificaciones(
                        true, true, List.of()));
        lenient().when(preferenciasClient.clientesInteresados(any()))
                .thenReturn(List.of(CLIENTE));
        lenient().when(usuariosClient.porId(any()))
                .thenReturn(new UsuariosClient.UsuarioEmailDTO(CLIENTE, "demo@ticketya.com"));
    }

    @Test
    void eventoCancelado_creaPushYEmailYEsIdempotente() throws Exception {
        EventoCambioDTO evento = evento("msg-cancelado", "EventoCancelado");

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "evento.cancelado", evento);

        List<Notificacion> filas = esperar("EventoCancelado:" + EVENTO + ":msg-cancelado", 2);
        assertEquals(2, filas.size());
        assertEquals(Set.of("PUSH", "EMAIL"), canales(filas));
        assertTrue(filas.stream().allMatch(n -> "PENDIENTE".equals(n.getEstado())));

        // Misma ocurrencia: no debe duplicar.
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "evento.cancelado", evento);
        Thread.sleep(1500);
        assertEquals(2, notificacionRepository
                .findByIdUsuarioAndClaveMensaje(CLIENTE, "EventoCancelado:" + EVENTO + ":msg-cancelado")
                .size());
    }

    @Test
    void eventoSuspendidoYModificado_creanUnMensajePorTipo() throws Exception {
        for (String tipo : List.of("EventoSuspendido", "EventoModificado")) {
            String messageId = "msg-" + tipo;
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE,
                    "evento." + tipo.substring("Evento".length()).toLowerCase(),
                    evento(messageId, tipo));

            List<Notificacion> filas = esperar(tipo + ":" + EVENTO + ":" + messageId, 2);
            assertEquals(2, filas.size(), "El aviso " + tipo + " debe tener PUSH y EMAIL");
            assertEquals(Set.of("PUSH", "EMAIL"), canales(filas));
        }
    }

    @Test
    void eventoPublicado_notificaArtistaFavorito() throws Exception {
        EventoCambioDTO evento = new EventoCambioDTO(
                "msg-publicado",
                EVENTO,
                "EventoPublicado",
                "Metallica — M72",
                LocalDateTime.of(2027, 3, 15, 21, 0),
                null,
                Instant.now(),
                "Buenos Aires",
                List.of("Metallica"));

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "evento.publicado", evento);

        List<Notificacion> filas =
                esperar("NuevoEventoArtistaFavorito:" + EVENTO + ":msg-publicado", 2);
        assertEquals(2, filas.size());
        assertEquals(Set.of("PUSH", "EMAIL"), canales(filas));
        assertTrue(filas.stream().allMatch(n -> n.getContenido().contains("Metallica")));
    }

    @Test
    void compraConfirmada_creaEmailYEsIdempotente() throws Exception {
        CompraConfirmadaDTO compra = new CompraConfirmadaDTO(
                "msg-compra",
                COMPRA,
                CLIENTE,
                EVENTO,
                new BigDecimal("95000.00"),
                "ARS",
                2,
                Instant.now());

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "compra.confirmada", compra);

        List<Notificacion> filas = esperar("CompraConfirmada:" + COMPRA, 1);
        assertEquals(1, filas.size());
        assertEquals("EMAIL", filas.get(0).getCanal());
        assertEquals("demo@ticketya.com", filas.get(0).getDestinatario());

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "compra.confirmada", compra);
        Thread.sleep(1500);
        assertEquals(1, notificacionRepository
                .findByIdUsuarioAndClaveMensaje(CLIENTE, "CompraConfirmada:" + COMPRA)
                .size());
    }

    @Test
    void devolucionAprobada_creaPushYEmailYEsIdempotente() throws Exception {
        DevolucionAprobadaDTO devolucion = new DevolucionAprobadaDTO(
                "msg-devolucion",
                DEVOLUCION,
                "aa000000-0000-4000-8000-000000000001",
                CLIENTE,
                EVENTO,
                new BigDecimal("95000.00"),
                "ARS",
                Instant.now());

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE, "devolucion.aprobada", devolucion);

        List<Notificacion> filas =
                esperar("DevolucionAprobada:" + DEVOLUCION, 2);
        assertEquals(2, filas.size());
        assertEquals(Set.of("PUSH", "EMAIL"), canales(filas));
        assertTrue(filas.stream().allMatch(n -> n.getContenido().contains("95000.00")));

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE, "devolucion.aprobada", devolucion);
        Thread.sleep(1500);
        assertEquals(2, notificacionRepository
                .findByIdUsuarioAndClaveMensaje(CLIENTE, "DevolucionAprobada:" + DEVOLUCION)
                .size());
    }

    private static EventoCambioDTO evento(String messageId, String tipo) {
        return new EventoCambioDTO(
                messageId,
                EVENTO,
                tipo,
                "Metallica — M72",
                LocalDateTime.of(2027, 3, 15, 21, 0),
                "cambio de prueba",
                Instant.now());
    }

    private static Set<String> canales(List<Notificacion> filas) {
        return filas.stream().map(Notificacion::getCanal).collect(Collectors.toSet());
    }

    private List<Notificacion> esperar(String claveMensaje, int esperadas) throws Exception {
        long deadline = System.currentTimeMillis() + 20_000;
        List<Notificacion> filas = List.of();
        while (System.currentTimeMillis() < deadline) {
            filas = notificacionRepository
                    .findByIdUsuarioAndClaveMensaje(CLIENTE, claveMensaje);
            if (filas.size() >= esperadas) {
                return filas;
            }
            Thread.sleep(200);
        }
        fail("Timeout esperando " + esperadas + " notificación(es) con clave " + claveMensaje
                + "; encontradas: " + filas.size());
        return filas;
    }
}
