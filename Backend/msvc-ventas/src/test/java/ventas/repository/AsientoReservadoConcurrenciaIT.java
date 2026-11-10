package ventas.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.dao.DataIntegrityViolationException;
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
import ventas.entity.AsientoReservado;
import ventas.entity.CupoSector;
import ventas.entity.Reserva;
import ventas.exception.BusinessConflictException;

/**
 * SEAT-06: valida con MySQL REAL la no doble venta del MISMO asiento de un sector numerado.
 * Mismo patron que {@link CupoSectorConcurrenciaIT}, pero sobre el esquema real: se carga
 * {@code infrastructure/mysql/ventas/init.sql} en el contenedor (con {@code ddl-auto=validate}),
 * porque la clave unica {@code (id_evento, id_sector, fila, numero)} existe solo ahi.
 *
 * <p>Cubre las dos defensas de {@code asiento_reservado} y el rollback de contadores:
 * <ol>
 *   <li>Lock pesimista {@code SELECT ... FOR UPDATE} sobre {@code cupo_sector} + pre-chequeo de
 *       asiento tomado: la segunda transaccion espera, ve el asiento ocupado y falla con
 *       {@link BusinessConflictException}.
 *   <li>Clave unica de MySQL: aun sin lock ni pre-chequeo, un duplicado es rechazado.
 * </ol>
 *
 * <p>Solo se ejecuta con {@code mvn verify -Pit} (failsafe, patron {@code *IT.java}).
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AsientoReservadoConcurrenciaIT {

    private static final String ID_EVENTO = "e0000000-0000-4000-8000-000000000001";
    private static final String ID_SECTOR = "c0000000-0000-4000-8000-000000000012";
    private static final String FILA = "C";
    private static final int NUMERO = 14;

    @Container
    static final MySQLContainer MYSQL =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("ventas")
                    .withUsername("ticketya")
                    .withPassword("ticketya_dev")
                    // Esquema real del proyecto (tablas + claves unicas); corre en el primer arranque.
                    .withCopyFileToContainer(
                            MountableFile.forHostPath("../infrastructure/mysql/ventas/init.sql"),
                            "/docker-entrypoint-initdb.d/init.sql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        // El esquema lo crea init.sql; Hibernate solo valida que las entidades coincidan.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private CupoSectorRepository cupoRepo;

    @Autowired
    private AsientoReservadoRepository asientoRepo;

    @Autowired
    private ReservaRepository reservaRepo;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager txManager;

    private TransactionTemplate tx;
    private String idCupo;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);
        // init.sql trae datos seed: se limpian las tablas (en orden de FK) para partir de cero.
        jdbc.execute("DELETE FROM asiento_reservado");
        jdbc.execute("DELETE FROM pago");
        jdbc.execute("DELETE FROM compra");
        jdbc.execute("DELETE FROM detalle_reserva");
        jdbc.execute("DELETE FROM reserva");
        jdbc.execute("DELETE FROM cupo_sector");

        idCupo = UUID.randomUUID().toString();
        CupoSector cupo = new CupoSector();
        cupo.setIdCupo(idCupo);
        cupo.setIdEvento(ID_EVENTO);
        cupo.setIdSector(ID_SECTOR);
        cupo.setCapacidadTotal(200);
        cupo.setCantidadDisponible(200);
        cupo.setCantidadReservada(0);
        cupo.setCantidadVendida(0);
        cupoRepo.saveAndFlush(cupo);
    }

    @Test
    @Timeout(90)
    void mismoAsiento_unaTransaccionGanaOtraEsperaVeAsientoTomadoYFalla() throws Exception {
        Reserva reservaA = crearReserva();
        Reserva reservaB = crearReserva();
        CountDownLatch primeraTieneLock = new CountDownLatch(1);

        Callable<Void> primera = () -> tx.execute(status -> {
            CupoSector cupo = cupoRepo.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR)
                    .orElseThrow();
            primeraTieneLock.countDown();
            try {
                // Retiene el lock de fila mientras la segunda transaccion intenta entrar.
                Thread.sleep(600);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            tomarAsiento(reservaA, cupo);
            return null;
        });

        Callable<Void> segunda = () -> tx.execute(status -> {
            CupoSector cupo = cupoRepo.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR)
                    .orElseThrow();
            if (asientoRepo.existsByIdEventoAndIdSectorAndFilaAndNumero(ID_EVENTO, ID_SECTOR, FILA, NUMERO)) {
                throw new BusinessConflictException("Asiento no disponible: " + FILA + "-" + NUMERO);
            }
            tomarAsiento(reservaB, cupo);
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

        assertEquals(1, exitos.get(), "Solo una transaccion debe reservar el asiento");
        assertEquals(1, conflictos.get(), "La otra debe fallar por asiento ya tomado");
        assertTrue(msSegunda >= 300,
                "La segunda transaccion debio esperar el lock de fila (espero " + msSegunda + " ms)");

        List<AsientoReservado> tomados = asientoRepo.findByIdEvento(ID_EVENTO);
        assertEquals(1, tomados.size(), "El asiento debe quedar tomado una sola vez");
        assertEquals(FILA, tomados.get(0).getFila());
        assertEquals(NUMERO, tomados.get(0).getNumero());
        assertEquals("RESERVADO", tomados.get(0).getEstado());

        CupoSector filaFinal = cupoRepo.findById(idCupo).orElseThrow();
        assertEquals(199, filaFinal.getCantidadDisponible(), "Solo se descuenta un lugar");
        assertEquals(1, filaFinal.getCantidadReservada(), "Debe quedar exactamente una unidad reservada");
    }

    @Test
    @Timeout(90)
    void mismoAsiento_sinLockLaClaveUnicaDeMySqlRechazaElDuplicado() throws Exception {
        int hilos = 8;
        List<Reserva> reservas = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            reservas.add(crearReserva());
        }

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger duplicados = new AtomicInteger();
        List<Future<?>> futuros = new ArrayList<>();

        for (Reserva reserva : reservas) {
            futuros.add(pool.submit(() -> {
                try {
                    salida.await();
                    // Sin lock de cupo ni pre-chequeo: solo la defensa de la base de datos.
                    tx.execute(status -> asientoRepo.saveAndFlush(nuevoAsiento(reserva, "RESERVADO")));
                    exitos.incrementAndGet();
                } catch (DataIntegrityViolationException e) {
                    duplicados.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    throw new IllegalStateException("Fallo inesperado", e);
                }
            }));
        }

        salida.countDown();
        for (Future<?> f : futuros) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        assertEquals(1, exitos.get(), "Solo un insert del mismo asiento puede prosperar");
        assertEquals(hilos - 1, duplicados.get(), "El resto debe chocar con la clave unica");
        assertEquals(1, asientoRepo.findByIdEvento(ID_EVENTO).size(), "El asiento queda una sola vez");
    }

    @Test
    @Timeout(90)
    void conflictoDeClaveUnica_haceRollbackDeLosContadoresDelCupo() {
        Reserva reservaA = crearReserva();
        Reserva reservaB = crearReserva();

        // La primera reserva queda confirmada en la base: asiento tomado + 1 lugar reservado.
        tx.execute(status -> {
            CupoSector cupo = cupoRepo.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR).orElseThrow();
            tomarAsiento(reservaA, cupo);
            return null;
        });

        // La segunda descuenta el cupo y recien despues choca con la clave unica: debe revertirse todo.
        try {
            tx.execute(status -> {
                CupoSector cupo = cupoRepo.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR).orElseThrow();
                cupo.setCantidadDisponible(cupo.getCantidadDisponible() - 1);
                cupo.setCantidadReservada(cupo.getCantidadReservada() + 1);
                cupoRepo.saveAndFlush(cupo);
                asientoRepo.saveAndFlush(nuevoAsiento(reservaB, "RESERVADO"));
                return null;
            });
            throw new AssertionError("Debio fallar por asiento duplicado");
        } catch (DataIntegrityViolationException esperado) {
            // la clave unica de MySQL rechazo el duplicado
        }

        CupoSector filaFinal = cupoRepo.findById(idCupo).orElseThrow();
        assertEquals(199, filaFinal.getCantidadDisponible(), "El descuento de la transaccion fallida se revirtio");
        assertEquals(1, filaFinal.getCantidadReservada(), "Solo cuenta la primera reserva");
        assertEquals(1, asientoRepo.findByIdEvento(ID_EVENTO).size(), "El asiento sigue tomado una sola vez");
    }

    @Test
    @Timeout(90)
    void asientosDistintos_ambasReservasProsperan() throws Exception {
        Reserva reservaA = crearReserva();
        Reserva reservaB = crearReserva();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger exitos = new AtomicInteger();
        List<Future<?>> futuros = new ArrayList<>();
        int[] numeros = {14, 15};
        Reserva[] reservas = {reservaA, reservaB};

        for (int i = 0; i < 2; i++) {
            final int numero = numeros[i];
            final Reserva reserva = reservas[i];
            futuros.add(pool.submit(() -> {
                try {
                    tx.execute(status -> {
                        CupoSector cupo = cupoRepo.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR)
                                .orElseThrow();
                        if (asientoRepo.existsByIdEventoAndIdSectorAndFilaAndNumero(
                                ID_EVENTO, ID_SECTOR, FILA, numero)) {
                            throw new BusinessConflictException("Asiento no disponible");
                        }
                        asientoRepo.saveAndFlush(nuevoAsiento(reserva, FILA, numero, "RESERVADO"));
                        cupo.setCantidadDisponible(cupo.getCantidadDisponible() - 1);
                        cupo.setCantidadReservada(cupo.getCantidadReservada() + 1);
                        cupoRepo.saveAndFlush(cupo);
                        return null;
                    });
                    exitos.incrementAndGet();
                } catch (Exception e) {
                    throw new IllegalStateException("No debia haber conflicto entre asientos distintos", e);
                }
            }));
        }
        for (Future<?> f : futuros) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        assertEquals(2, exitos.get(), "Dos asientos distintos no se bloquean entre si");
        assertEquals(2, asientoRepo.findByIdEvento(ID_EVENTO).size());
        CupoSector filaFinal = cupoRepo.findById(idCupo).orElseThrow();
        assertEquals(198, filaFinal.getCantidadDisponible());
        assertEquals(2, filaFinal.getCantidadReservada());
    }

    private void tomarAsiento(Reserva reserva, CupoSector cupo) {
        asientoRepo.saveAndFlush(nuevoAsiento(reserva, "RESERVADO"));
        cupo.setCantidadDisponible(cupo.getCantidadDisponible() - 1);
        cupo.setCantidadReservada(cupo.getCantidadReservada() + 1);
        cupoRepo.saveAndFlush(cupo);
    }

    private AsientoReservado nuevoAsiento(Reserva reserva, String estado) {
        return nuevoAsiento(reserva, FILA, NUMERO, estado);
    }

    private AsientoReservado nuevoAsiento(Reserva reserva, String fila, int numero, String estado) {
        AsientoReservado a = new AsientoReservado();
        a.setIdAsientoReservado(UUID.randomUUID().toString());
        a.setReserva(reserva);
        a.setIdEvento(ID_EVENTO);
        a.setIdSector(ID_SECTOR);
        a.setFila(fila);
        a.setNumero(numero);
        a.setEstado(estado);
        return a;
    }

    private Reserva crearReserva() {
        Reserva r = new Reserva();
        r.setIdReserva(UUID.randomUUID().toString());
        r.setIdCliente(UUID.randomUUID().toString());
        r.setIdEvento(ID_EVENTO);
        r.setFechaCreacion(LocalDateTime.now());
        r.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));
        r.setEstado("PENDIENTE");
        r.setTotal(BigDecimal.valueOf(180000));
        return reservaRepo.saveAndFlush(r);
    }
}
