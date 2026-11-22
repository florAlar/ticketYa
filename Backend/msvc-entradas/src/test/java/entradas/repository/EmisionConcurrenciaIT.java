package entradas.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import entradas.clients.EventosClient;
import entradas.clients.VentasClient;
import entradas.dto.EmitirEntradasRequestDTO;
import entradas.dto.EntradaResponseDTO;
import entradas.exception.ServiceUnavailableException;
import entradas.mapper.EntradaMapper;
import entradas.service.EntradaServiceImpl;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Emisión idempotente con MySQL real: si ventas reintenta la emisión (timeout de Feign) o el job y
 * el post-pago se cruzan, la compra debe terminar con exactamente las entradas de su detalle.
 *
 * <p>El claim {@code emision_compra} se inserta en la misma transacción que las entradas; los
 * requests concurrentes que no lo obtienen reciben 503 y ventas reintenta.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmisionConcurrenciaIT {

    private static final String COMPRA = "99999999-9999-4999-8999-999999999999";
    private static final String CLIENTE = "b0000000-0000-4000-8000-000000000001";
    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";
    private static final String SECTOR = "c0000000-0000-4000-8000-000000000011";

    @Container
    static final MySQLContainer MYSQL =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("entradas")
                    .withUsername("ticketya")
                    .withPassword("ticketya_dev")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath("../infrastructure/mysql/entradas/init.sql"),
                            "/docker-entrypoint-initdb.d/init.sql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private EntradaRepository entradaRepository;
    @Autowired private PlatformTransactionManager txManager;
    @Autowired private JdbcTemplate jdbc;

    private EntradaServiceImpl service;
    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM validacion_acceso");
        jdbc.update("DELETE FROM devolucion");
        jdbc.update("DELETE FROM entrada");
        jdbc.update("DELETE FROM emision_compra");
        service = new EntradaServiceImpl(
                entradaRepository,
                mock(DevolucionRepository.class),
                new EntradaMapper(),
                mock(EventosClient.class),
                mock(VentasClient.class),
                mock(ValidacionAccesoRepository.class),
                mock(ApplicationEventPublisher.class),
                new EmisionClaimRepository(jdbc),
                72L);
        tx = new TransactionTemplate(txManager);
    }

    private List<EntradaResponseDTO> emitir() {
        EmitirEntradasRequestDTO req = new EmitirEntradasRequestDTO(
                COMPRA,
                CLIENTE,
                EVENTO,
                List.of(new EmitirEntradasRequestDTO.Item(
                        SECTOR, 2, BigDecimal.valueOf(95000))));
        return tx.execute(status -> service.emitir(req));
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void emisionesConcurrentes_mismaCompra_creanUnaSolaTanda() throws Exception {
        int hilos = 8;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<List<EntradaResponseDTO>>> futuros = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            futuros.add(pool.submit(() -> {
                salida.await();
                return emitir();
            }));
        }
        salida.countDown();

        int exitosas = 0;
        int reintentables = 0;
        for (Future<List<EntradaResponseDTO>> f : futuros) {
            try {
                assertEquals(2, f.get(30, TimeUnit.SECONDS).size());
                exitosas++;
            } catch (ExecutionException e) {
                assertTrue(e.getCause() instanceof ServiceUnavailableException,
                        "esperaba 503 de emisión en curso, fue " + e.getCause());
                reintentables++;
            }
        }
        pool.shutdownNow();

        assertTrue(exitosas >= 1, "al menos una emisión debe completar");
        assertEquals(hilos, exitosas + reintentables);
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM entrada WHERE id_compra = ?", Integer.class, COMPRA),
                "la compra debe tener exactamente sus 2 entradas");
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM emision_compra WHERE id_compra = ?", Integer.class, COMPRA));
    }
}
