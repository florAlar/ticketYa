package ventas.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import ventas.entity.CupoSector;
import ventas.exception.BusinessConflictException;

/**
 * TST-CONC-02 (P4.2): valida el lock pesimista REAL de MySQL
 * ({@code SELECT ... FOR UPDATE}) sobre el ultimo cupo de un sector.
 *
 * <p>Dos transacciones compiten: la primera toma el lock y lo retiene 600 ms;
 * la segunda queda bloqueada en el {@code SELECT ... FOR UPDATE}. Al continuar,
 * ya ve {@code disponible = 0} y falla con {@link BusinessConflictException}.
 *
 * <p>Solo se ejecuta con {@code mvn verify -Pit} (failsafe, patron {@code *IT.java}).
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CupoSectorConcurrenciaIT {

    private static final String ID_EVENTO = "e0000000-0000-4000-8000-000000000001";
    private static final String ID_SECTOR = "c0000000-0000-4000-8000-000000000011";

    @Container
    static final MySQLContainer MYSQL =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("ventas")
                    .withUsername("ticketya")
                    .withPassword("ticketya_dev");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private CupoSectorRepository cupoRepo;

    @Autowired
    private PlatformTransactionManager txManager;

    private TransactionTemplate tx;
    private String idCupo;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);
        cupoRepo.deleteAll();

        idCupo = UUID.randomUUID().toString();
        CupoSector cupo = new CupoSector();
        cupo.setIdCupo(idCupo);
        cupo.setIdEvento(ID_EVENTO);
        cupo.setIdSector(ID_SECTOR);
        cupo.setCapacidadTotal(1);
        cupo.setCantidadDisponible(1);
        cupo.setCantidadReservada(0);
        cupo.setCantidadVendida(0);
        cupoRepo.saveAndFlush(cupo);
    }

    @Test
    @Timeout(90)
    void ultimoCupoDeSector_unaTransaccionGanaOtraEsperaYFalla() throws Exception {
        CountDownLatch primeraTieneLock = new CountDownLatch(1);

        Callable<Void> primera = () -> tx.execute(status -> {
            CupoSector cupo = cupoRepo.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR)
                    .orElseThrow();
            primeraTieneLock.countDown();
            try {
                // Retiene el lock de fila mientras la segunda transaccion intenta leer.
                Thread.sleep(600);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            reservarUnaUnidad(cupo);
            return null;
        });

        Callable<Void> segunda = () -> tx.execute(status -> {
            CupoSector cupo = cupoRepo.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR)
                    .orElseThrow();
            if (cupo.getCantidadDisponible() < 1) {
                throw new BusinessConflictException(
                        "Cupo insuficiente sector " + ID_SECTOR
                                + " (disp=" + cupo.getCantidadDisponible() + ", ped=1)");
            }
            reservarUnaUnidad(cupo);
            return null;
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger conflictos = new AtomicInteger();

        Future<?> f1 = pool.submit(() -> {
            try {
                primera.call();
                exitos.incrementAndGet();
            } catch (Exception e) {
                throw new IllegalStateException("Fallo inesperado en la primera transaccion", e);
            }
        });

        assertTrue(primeraTieneLock.await(15, TimeUnit.SECONDS),
                "La primera transaccion no llego a tomar el lock");

        long inicioSegunda = System.nanoTime();
        Future<?> f2 = pool.submit(() -> {
            try {
                segunda.call();
                exitos.incrementAndGet();
            } catch (BusinessConflictException e) {
                conflictos.incrementAndGet();
            } catch (Exception e) {
                throw new IllegalStateException("Fallo inesperado en la segunda transaccion", e);
            }
        });

        f1.get(60, TimeUnit.SECONDS);
        f2.get(60, TimeUnit.SECONDS);
        long msSegunda = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioSegunda);
        pool.shutdownNow();

        assertEquals(1, exitos.get(), "Solo una transaccion debe reservar el ultimo cupo");
        assertEquals(1, conflictos.get(), "La otra debe fallar por cupo insuficiente");
        assertTrue(msSegunda >= 300,
                "La segunda transaccion debio esperar el lock de fila (espero " + msSegunda + " ms)");

        CupoSector filaFinal = cupoRepo.findById(idCupo).orElseThrow();
        assertEquals(0, filaFinal.getCantidadDisponible(), "El cupo no debe quedar negativo");
        assertEquals(1, filaFinal.getCantidadReservada(), "Debe quedar exactamente una unidad reservada");
        assertEquals(0, filaFinal.getCantidadVendida());
        assertEquals(1, filaFinal.getCapacidadTotal());
    }

    private void reservarUnaUnidad(CupoSector cupo) {
        cupo.setCantidadDisponible(cupo.getCantidadDisponible() - 1);
        cupo.setCantidadReservada(cupo.getCantidadReservada() + 1);
        cupoRepo.saveAndFlush(cupo);
    }
}
