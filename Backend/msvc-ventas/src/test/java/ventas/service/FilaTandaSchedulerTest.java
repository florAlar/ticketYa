package ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ventas.redis.FilaRedisStore;
import ventas.repository.CupoSectorRepository;

/** FIL-02: cálculo del paso, lock y tolerancia a fallos del job automático. */
@ExtendWith(MockitoExtension.class)
class FilaTandaSchedulerTest {

    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";

    @Mock
    private FilaRedisStore store;

    @Mock
    private FilaEventoCache eventos;

    @Mock
    private CupoSectorRepository cupos;

    @Test
    void siguienteTanda_topadaPorCupoTamanioYSeq() {
        // cupo 100, 90 activos -> espacio 10; tamanio 25 -> paso 10; tanda 50 -> 60
        assertEquals(60, FilaTandaScheduler.siguienteTanda(
                new FilaRedisStore.Resumen(500, 50, 90), 100, 25));
        // espacio sobrado -> tope por tamanio
        assertEquals(75, FilaTandaScheduler.siguienteTanda(
                new FilaRedisStore.Resumen(500, 50, 0), 1000, 25));
        // nunca supera seq
        assertEquals(55, FilaTandaScheduler.siguienteTanda(
                new FilaRedisStore.Resumen(55, 50, 0), 1000, 25));
        // sin espacio -> no abre
        assertEquals(50, FilaTandaScheduler.siguienteTanda(
                new FilaRedisStore.Resumen(500, 50, 100), 100, 25));
    }

    @Test
    void disabled_noTomaLockNiLeeEventos() {
        new FilaTandaScheduler(store, eventos, cupos, false, 25).tick();

        verify(store, never()).adquirirLock(anyString());
        verify(store, never()).eventos();
    }

    @Test
    void lockNoObtenido_otraInstanciaCorreEsteTick() {
        when(store.adquirirLock(anyString())).thenReturn(false);

        new FilaTandaScheduler(store, eventos, cupos, true, 25).tick();

        verify(store, never()).eventos();
        verify(store, never()).liberarLock(anyString());
    }

    @Test
    void tick_eventoElegible_habilitaYSueltaLock() {
        when(store.adquirirLock(anyString())).thenReturn(true);
        when(store.eventos()).thenReturn(Set.of(EVENTO));
        when(store.existeEvento(EVENTO)).thenReturn(true);
        when(eventos.obtener(EVENTO)).thenReturn(new FilaEventoCache.Info(EVENTO, "PUBLICADO", true));
        when(store.resumen(EVENTO)).thenReturn(new FilaRedisStore.Resumen(500, 50, 90));
        when(cupos.sumarDisponible(EVENTO)).thenReturn(100L);
        when(store.habilitar(EVENTO, 60)).thenReturn(60L);

        new FilaTandaScheduler(store, eventos, cupos, true, 25).tick();

        verify(store).habilitar(EVENTO, 60);
        verify(store).liberarLock(anyString());
    }

    @Test
    void tick_eventoSinZset_seQuitaDelIndice() {
        when(store.adquirirLock(anyString())).thenReturn(true);
        when(store.eventos()).thenReturn(Set.of(EVENTO));
        when(store.existeEvento(EVENTO)).thenReturn(false);

        new FilaTandaScheduler(store, eventos, cupos, true, 25).tick();

        verify(store).removerEvento(EVENTO);
        verify(store, never()).habilitar(anyString(), any(Long.class));
        verify(store).liberarLock(anyString());
    }

    @Test
    void tick_excepcionDeUnEvento_noSaleDelScheduler() {
        when(store.adquirirLock(anyString())).thenReturn(true);
        when(store.eventos()).thenReturn(Set.of(EVENTO));
        when(store.existeEvento(EVENTO)).thenThrow(new IllegalStateException("Redis caído"));

        FilaTandaScheduler job = new FilaTandaScheduler(store, eventos, cupos, true, 25);
        assertDoesNotThrow(job::tick);

        verify(store).liberarLock(anyString());
    }
}
