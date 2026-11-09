package ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;
import ventas.clients.EventosClient;
import ventas.clients.EventosClient.EventoDetalleFeignDTO;
import ventas.clients.EventosClient.SectorFeignDTO;
import ventas.dto.ReservaCreateRequestDTO;
import ventas.dto.ReservaResponseDTO;
import ventas.entity.CupoSector;
import ventas.exception.BusinessConflictException;
import ventas.mapper.VentaMapper;
import ventas.redis.StockCache;
import ventas.repository.CompraRepository;
import ventas.repository.CupoSectorRepository;
import ventas.repository.DetalleReservaRepository;
import ventas.repository.PagoRepository;
import ventas.repository.ReservaRepository;

/**
 * TST-CONC-01: N hilos compiten por el ultimo cupo de un sector.
 * Exactamente uno reserva; el resto recibe BusinessConflictException.
 *
 * <p>Sin Spring ni base de datos: el lock de fila (SELECT ... FOR UPDATE) se
 * emula con un ReentrantLock que se toma al leer el cupo y se libera al
 * guardar (commit) o al salir del intento (rollback).
 */
class ReservaConcurrenciaTest {

    private static final String ID_EVENTO = "e0000000-0000-4000-8000-000000000001";
    private static final String ID_SECTOR = "c0000000-0000-4000-8000-000000000011";
    private static final int HILOS = 20;

    @Test
    void ultimoCupoDeSector_soloUnHiloReserva() throws Exception {
        CupoSector fila = new CupoSector();
        fila.setIdEvento(ID_EVENTO);
        fila.setIdSector(ID_SECTOR);
        fila.setCapacidadTotal(1);
        fila.setCantidadDisponible(1);
        fila.setCantidadReservada(0);
        fila.setCantidadVendida(0);

        ReentrantLock lockFila = new ReentrantLock(true);

        CupoSectorRepository cupoRepo = mock(CupoSectorRepository.class);
        when(cupoRepo.findByEventoAndSectorForUpdate(ID_EVENTO, ID_SECTOR)).thenAnswer(inv -> {
            lockFila.lock(); // inicio de "transaccion": SELECT ... FOR UPDATE
            return Optional.of(fila);
        });
        when(cupoRepo.save(any(CupoSector.class))).thenAnswer(inv -> {
            try {
                return inv.getArgument(0);
            } finally {
                if (lockFila.isHeldByCurrentThread()) {
                    lockFila.unlock(); // commit: libera el lock de fila
                }
            }
        });

        ReservaRepository reservaRepo = mock(ReservaRepository.class);
        DetalleReservaRepository detalleRepo = mock(DetalleReservaRepository.class);
        CompraRepository compraRepo = mock(CompraRepository.class);
        PagoRepository pagoRepo = mock(PagoRepository.class);
        EventosClient eventosClient = mock(EventosClient.class);
        StockCache stockCache = mock(StockCache.class);
        VentaMapper mapper = mock(VentaMapper.class);
        EmisionEntradasService emision = mock(EmisionEntradasService.class);

        VentaServiceImpl service = new VentaServiceImpl(
                cupoRepo, reservaRepo, detalleRepo, compraRepo, pagoRepo,
                eventosClient, stockCache, mapper, emision, 10L, 6, 24L);

        EventoDetalleFeignDTO evento = new EventoDetalleFeignDTO();
        evento.setId(ID_EVENTO);
        evento.setEstado("PUBLICADO");
        SectorFeignDTO sector = new SectorFeignDTO();
        sector.setId(ID_SECTOR);
        sector.setPrecio(BigDecimal.valueOf(95000));
        evento.setSectores(new ArrayList<>(List.of(sector)));

        when(eventosClient.getEvento(ID_EVENTO)).thenReturn(evento);
        when(reservaRepo.findByIdClienteAndIdEventoAndEstadoIn(
                        anyString(), anyString(), anyCollection()))
                .thenReturn(List.of());
        when(compraRepo.sumCantidadByClienteEventoAndEstados(
                        anyString(), anyString(), anyCollection()))
                .thenReturn(0L);
        when(mapper.toReserva(any(), anyList())).thenReturn(new ReservaResponseDTO());

        ReservaCreateRequestDTO request = new ReservaCreateRequestDTO(
                ID_EVENTO,
                List.of(new ReservaCreateRequestDTO.Item(ID_SECTOR, Integer.valueOf(1))));

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(HILOS);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger conflictos = new AtomicInteger();
        AtomicInteger errores = new AtomicInteger();
        List<Future<?>> tareas = new ArrayList<>();

        for (int i = 0; i < HILOS; i++) {
            final int idx = i;
            tareas.add(pool.submit(() -> {
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
                            lockFila.unlock(); // rollback: libera si no hubo save
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    fin.countDown();
                }
            }));
        }

        salida.countDown();
        assertTrue(fin.await(15, TimeUnit.SECONDS), "Los hilos no terminaron a tiempo");
        pool.shutdownNow();
        for (Future<?> t : tareas) {
            t.get(1, TimeUnit.SECONDS);
        }

        assertEquals(0, errores.get(), "No debe haber errores inesperados");
        assertEquals(1, exitos.get(), "Solo un hilo debe conseguir el ultimo cupo");
        assertEquals(HILOS - 1, conflictos.get(), "El resto debe recibir conflicto de cupo");
        assertEquals(0, fila.getCantidadDisponible(), "El cupo no debe quedar negativo");
        assertEquals(1, fila.getCantidadReservada(), "Debe quedar una unica unidad reservada");
        assertEquals(0, fila.getCantidadVendida());
    }
}
