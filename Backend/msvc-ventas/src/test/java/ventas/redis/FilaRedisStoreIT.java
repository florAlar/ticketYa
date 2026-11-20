package ventas.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** FIL-01 a FIL-03: los scripts Lua contra un Redis real. Solo corre con -Pit y Docker disponible. */
@Testcontainers(disabledWithoutDocker = true)
class FilaRedisStoreIT {

    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static FilaRedisStore store;
    private static StringRedisTemplate template;

    @BeforeAll
    static void setUp() {
        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        factory.afterPropertiesSet();
        template = new StringRedisTemplate(factory);
        store = new FilaRedisStore(template, "fila-it", 24, 10, 8000);
    }

    @Test
    void cienUsuariosConcurrentes_posicionesUnicasYConsecutivas() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<FilaRedisStore.Ingreso>> futures = new ArrayList<>();
        try {
            for (int i = 1; i <= 100; i++) {
                String user = "user-" + i;
                futures.add(pool.submit(() -> {
                    salida.await();
                    return store.ingresar(EVENTO, user);
                }));
            }
            salida.countDown();
            List<Long> posiciones = new ArrayList<>();
            for (Future<FilaRedisStore.Ingreso> f : futures) {
                posiciones.add(f.get(20, TimeUnit.SECONDS).posicion());
            }
            posiciones.sort(Long::compareTo);
            for (int i = 0; i < 100; i++) {
                assertEquals(i + 1, posiciones.get(i));
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void reingreso_mismaPosicionTipoNumericoYNoCreado() {
        FilaRedisStore.Ingreso primera = store.ingresar(EVENTO + "-reingreso", "user-reingreso");
        FilaRedisStore.Ingreso segunda = store.ingresar(EVENTO + "-reingreso", "user-reingreso");

        assertTrue(primera.creado());
        assertFalse(segunda.creado());
        assertEquals(primera.posicion(), segunda.posicion());
        assertNotNull(template.opsForZSet().score(store.zsetKey(EVENTO + "-reingreso"), "user-reingreso"));
    }

    @Test
    void mismoUsuarioConVeinteHilos_unaSolaEntrada() throws Exception {
        String evento = EVENTO + "-mismo-usuario";
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<FilaRedisStore.Ingreso>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 20; i++) {
                futures.add(pool.submit(() -> {
                    salida.await();
                    return store.ingresar(evento, "user-unico");
                }));
            }
            salida.countDown();
            long posicion = -1;
            for (Future<FilaRedisStore.Ingreso> f : futures) {
                FilaRedisStore.Ingreso r = f.get(20, TimeUnit.SECONDS);
                if (posicion < 0) {
                    posicion = r.posicion();
                }
                assertEquals(posicion, r.posicion());
            }
            assertEquals(1L, template.opsForZSet().size(store.zsetKey(evento)));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void trasPrimerIngreso_lasCuatroClavesTienenTtl() {
        String evento = EVENTO + "-ttl";
        store.ingresar(evento, "user-ttl");

        assertTrue(template.getExpire(store.zsetKey(evento)) > 0);
        assertTrue(template.getExpire(store.seqKey(evento)) > 0);
        assertTrue(template.getExpire(store.tandaKey(evento)) > 0);
        assertTrue(template.getExpire(store.usadosKey(evento)) > 0);
    }

    @Test
    void habilitacionConcurrente_elResultadoEsElValorMayor() throws Exception {
        String evento = EVENTO + "-habilitar";
        for (int i = 1; i <= 20; i++) {
            store.ingresar(evento, "user-" + i);
        }
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();
        try {
            for (int i = 1; i <= 20; i++) {
                final long pedido = i;
                futures.add(pool.submit(() -> {
                    salida.await();
                    return store.habilitar(evento, pedido);
                }));
            }
            salida.countDown();
            long mayor = 0;
            for (Future<Long> f : futures) {
                mayor = Math.max(mayor, f.get(20, TimeUnit.SECONDS));
            }
            assertEquals(20, mayor);
            assertEquals(20, store.resumen(evento).tanda());
            assertEquals(-1, store.habilitar(evento, 19));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void lockSoloLoObtieneUnaInstancia_ySeLiberaConComparacion() {
        String instancia = "instancia-" + System.nanoTime();
        String otra = "otra-" + System.nanoTime();

        assertTrue(store.adquirirLock(instancia));
        assertFalse(store.adquirirLock(otra));
        store.liberarLock(otra); // no era suyo: no lo suelta
        assertFalse(store.adquirirLock(otra));
        store.liberarLock(instancia);
        assertTrue(store.adquirirLock(otra));
    }

    @Test
    void resumenIncluyeActivos() {
        String evento = EVENTO + "-resumen";
        store.ingresar(evento, "user-1");
        store.ingresar(evento, "user-2");
        store.habilitar(evento, 1);

        FilaRedisStore.Resumen r = store.resumen(evento);

        assertEquals(2, r.seq());
        assertEquals(1, r.tanda());
        assertEquals(1, r.activos());
    }

    @Test
    void lecturasDePosicionYEstado() {
        String evento = EVENTO + "-lecturas";
        FilaRedisStore.Ingreso ingreso = store.ingresar(evento, "user-lecturas");

        FilaRedisStore.Posicion p = store.posicion(evento, "user-lecturas");
        assertEquals(ingreso.posicion(), p.posicion());
        assertFalse(p.usado());
        assertFalse(p.conToken());

        FilaRedisStore.Estado e = store.estado(evento);
        assertEquals(1, e.totalEnFila());
        assertEquals(0, e.tanda());
    }
}
