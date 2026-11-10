package ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;
import ventas.clients.EventosClient;
import ventas.clients.EventosClient.EventoDetalleFeignDTO;
import ventas.clients.EventosClient.SectorFeignDTO;
import ventas.dto.AsientoRefDTO;
import ventas.dto.ReservaCreateRequestDTO;
import ventas.dto.ReservaResponseDTO;
import ventas.entity.AsientoReservado;
import ventas.entity.CupoSector;
import ventas.exception.BusinessConflictException;
import ventas.mapper.VentaMapper;
import ventas.redis.StockCache;
import ventas.repository.AsientoReservadoRepository;
import ventas.repository.CompraRepository;
import ventas.repository.CupoSectorRepository;
import ventas.repository.DetalleReservaRepository;
import ventas.repository.PagoRepository;
import ventas.repository.ReservaRepository;

/**
 * N hilos compiten por EL MISMO asiento de un sector numerado: exactamente uno lo reserva.
 *
 * <p>Sin Spring ni base: el lock de fila del cupo (SELECT ... FOR UPDATE) se emula con un
 * ReentrantLock que se toma al leer el cupo y se libera al terminar el intento (fin de la
 * "transacción"); la tabla de asientos tomados se emula con un set concurrente. No emula el
 * rollback de contadores, por eso el cupo tiene capacidad de sobra: lo que decide aquí es el asiento.
 */
class ReservaAsientoConcurrenciaTest {

    private static final String ID_EVENTO = "e0000000-0000-4000-8000-000000000001";
    private static final String ID_SECTOR = "c0000000-0000-4000-8000-000000000012";
    private static final int HILOS = 20;

    @Test
    void mismoAsiento_soloUnHiloLoReserva() throws Exception {
        CupoSector fila = new CupoSector();
        fila.setIdEvento(ID_EVENTO);
        fila.setIdSector(ID_SECTOR);
        fila.setCapacidadTotal(1000);
        fila.setCantidadDisponible(1000);
        fila.setCantidadReservada(0);
        fila.setCantidadVendida(0);

        ReentrantLock lockFila = new ReentrantLock(true);
        CupoSectorRepository cupoRepo = mock(CupoSectorRepository.class);
        when(cupoRepo.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR)).thenAnswer(inv -> {
            lockFila.lock();
            return Optional.of(fila);
        });

        Set<String> tomados = ConcurrentHashMap.newKeySet();
        AsientoReservadoRepository asientoRepo = mock(AsientoReservadoRepository.class);
        when(asientoRepo.existsByIdEventoAndIdSectorAndFilaAndNumero(anyString(), anyString(), anyString(), anyInt()))
                .thenAnswer(inv -> tomados.contains(inv.getArgument(2) + "-" + inv.getArgument(3)));
        doAnswer(inv -> {
            Iterable<AsientoReservado> it = inv.getArgument(0);
            it.forEach(a -> tomados.add(a.getFila() + "-" + a.getNumero()));
            return null;
        }).when(asientoRepo).saveAll(org.mockito.ArgumentMatchers.<Iterable<AsientoReservado>>any());

        ReservaRepository reservaRepo = mock(ReservaRepository.class);
        CompraRepository compraRepo = mock(CompraRepository.class);
        EventosClient eventosClient = mock(EventosClient.class);
        VentaMapper mapper = mock(VentaMapper.class);
        VentaServiceImpl service = new VentaServiceImpl(
                cupoRepo, reservaRepo, mock(DetalleReservaRepository.class), asientoRepo, compraRepo,
                mock(PagoRepository.class), eventosClient, mock(StockCache.class), mapper,
                mock(EmisionEntradasService.class), 10L, 6, 24L);

        EventoDetalleFeignDTO evento = new EventoDetalleFeignDTO();
        evento.setId(ID_EVENTO);
        evento.setEstado("PUBLICADO");
        SectorFeignDTO sector = new SectorFeignDTO();
        sector.setId(ID_SECTOR);
        sector.setNombre("Platea");
        sector.setPrecio(BigDecimal.valueOf(180000));
        sector.setEsNumerado(true);
        sector.setFilas(10);
        sector.setAsientosPorFila(20);
        evento.setSectores(new ArrayList<>(List.of(sector)));
        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(evento);
        when(reservaRepo.findByIdClienteAndIdEventoAndEstadoIn(anyString(), anyString(), anyCollection()))
                .thenReturn(List.of());
        when(compraRepo.sumCantidadByClienteEventoAndEstados(anyString(), anyString(), anyCollection()))
                .thenReturn(0L);
        when(mapper.toReserva(any(), anyList())).thenReturn(new ReservaResponseDTO());

        ReservaCreateRequestDTO request = new ReservaCreateRequestDTO(
                ID_EVENTO,
                List.of(new ReservaCreateRequestDTO.Item(
                        ID_SECTOR, null, List.of(new AsientoRefDTO("C", 14)))));

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(HILOS);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger conflictos = new AtomicInteger();
        AtomicInteger errores = new AtomicInteger();

        for (int i = 0; i < HILOS; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    salida.await();
                    try {
                        service.crearReserva("cliente-" + idx, request, null);
                        exitos.incrementAndGet();
                    } catch (BusinessConflictException e) {
                        conflictos.incrementAndGet();
                    } catch (RuntimeException e) {
                        errores.incrementAndGet();
                    } finally {
                        if (lockFila.isHeldByCurrentThread()) {
                            lockFila.unlock(); // fin de la transacción (commit o rollback)
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    fin.countDown();
                }
            });
        }

        salida.countDown();
        assertTrue(fin.await(15, TimeUnit.SECONDS), "Los hilos no terminaron a tiempo");
        pool.shutdownNow();

        assertEquals(0, errores.get(), "No debe haber errores inesperados");
        assertEquals(1, exitos.get(), "Solo un hilo debe conseguir el asiento");
        assertEquals(HILOS - 1, conflictos.get(), "El resto debe recibir conflicto de asiento");
        assertEquals(Set.of("C-14"), tomados, "El asiento debe quedar tomado una sola vez");
    }
}
