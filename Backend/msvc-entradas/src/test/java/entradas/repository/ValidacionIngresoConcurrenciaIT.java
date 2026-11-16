package entradas.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import entradas.clients.EventosClient;
import entradas.clients.VentasClient;
import entradas.dto.ValidacionRequestDTO;
import entradas.dto.ValidacionResponseDTO;
import entradas.mapper.EntradaMapper;
import entradas.service.EntradaServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
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
 * E2-ACC-01: con MySQL REAL, un mismo QR escaneado a la vez en varias puertas solo puede ingresar
 * una vez. Se carga {@code infrastructure/mysql/entradas/init.sql} (esquema real, con
 * {@code ddl-auto=validate}). La exclusión la da el {@code SELECT ... FOR UPDATE} de
 * {@link EntradaRepository#findByCodigoQrForUpdate}: sin él, varios hilos leerían VENDIDA y todos
 * entrarían.
 *
 * <p>Solo se ejecuta con {@code mvn verify -Pit} (failsafe, patrón {@code *IT.java}); requiere Docker.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ValidacionIngresoConcurrenciaIT {

    private static final String ID_ENTRADA = "66666666-6666-4666-8666-666666666666";
    private static final String ID_EVENTO = "d0000000-0000-4000-8000-000000000001";
    private static final String OTRO_EVENTO = "d0000000-0000-4000-8000-000000000002";
    private static final String QR = "QR-8F3A1C5D2B7E9A40";
    private static final String PERSONAL = "b0000000-0000-4000-8000-000000000004";

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
    @Autowired private DevolucionRepository devolucionRepository;
    @Autowired private ValidacionAccesoRepository validacionRepository;
    @Autowired private PlatformTransactionManager txManager;
    @Autowired private JdbcTemplate jdbc;

    private EntradaServiceImpl service;
    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM validacion_acceso");
        jdbc.update("DELETE FROM devolucion");
        jdbc.update("DELETE FROM entrada");
        jdbc.update(
                "INSERT INTO entrada (id_entrada, id_compra, id_cliente, id_evento, id_sector, precio,"
                        + " codigo_qr, estado) VALUES (?, ?, ?, ?, ?, 95000, ?, 'VENDIDA')",
                ID_ENTRADA,
                "33333333-3333-4333-8333-333333333333",
                "b0000000-0000-4000-8000-000000000001",
                ID_EVENTO,
                "c0000000-0000-4000-8000-000000000011",
                QR);
        // El servicio real, sin proxy de Spring: cada llamada se envuelve en su propia transacción.
        service = new EntradaServiceImpl(
                entradaRepository,
                devolucionRepository,
                new EntradaMapper(),
                mock(EventosClient.class),
                mock(VentasClient.class),
                validacionRepository,
                72L);
        tx = new TransactionTemplate(txManager);
    }

    private ValidacionResponseDTO escanear(String idEvento) {
        return tx.execute(s -> service.validarIngreso(idEvento, PERSONAL, new ValidacionRequestDTO(QR, "puerta")));
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void ochoPuertasEscanean_elMismoQr_aLaVez_ingresaUnaSolaVez() throws Exception {
        int puertas = 8;
        ExecutorService pool = Executors.newFixedThreadPool(puertas);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<ValidacionResponseDTO>> futuros = new ArrayList<>();
        for (int i = 0; i < puertas; i++) {
            Callable<ValidacionResponseDTO> tarea = () -> {
                salida.await();
                return escanear(ID_EVENTO);
            };
            futuros.add(pool.submit(tarea));
        }
        salida.countDown();

        int validas = 0;
        int yaUsadas = 0;
        for (Future<ValidacionResponseDTO> f : futuros) {
            ValidacionResponseDTO r = f.get(30, TimeUnit.SECONDS);
            if ("VALIDA".equals(r.getResultado())) validas++;
            if ("YA_USADA".equals(r.getResultado())) yaUsadas++;
        }
        pool.shutdownNow();

        assertEquals(1, validas, "exactamente una puerta deja ingresar al QR");
        assertEquals(puertas - 1, yaUsadas, "las demás ven YA_USADA");
        assertEquals("USADA", jdbc.queryForObject("SELECT estado FROM entrada WHERE id_entrada = ?", String.class, ID_ENTRADA));
        assertNotNull(jdbc.queryForObject("SELECT fecha_validacion FROM entrada WHERE id_entrada = ?", Object.class, ID_ENTRADA));
        assertEquals(puertas, jdbc.queryForObject("SELECT COUNT(*) FROM validacion_acceso", Integer.class),
                "todos los intentos quedan auditados");
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM validacion_acceso WHERE resultado = 'VALIDA'", Integer.class));
    }

    @Test
    void qrDeOtroEvento_noIngresa_yNoSeConsume() {
        ValidacionResponseDTO r = escanear(OTRO_EVENTO);

        assertEquals("OTRO_EVENTO", r.getResultado());
        assertEquals("VENDIDA", jdbc.queryForObject("SELECT estado FROM entrada WHERE id_entrada = ?", String.class, ID_ENTRADA));
        assertNull(jdbc.queryForObject("SELECT fecha_validacion FROM entrada WHERE id_entrada = ?", Object.class, ID_ENTRADA));
        // y en su evento sigue pudiendo entrar
        assertEquals("VALIDA", escanear(ID_EVENTO).getResultado());
    }
}
