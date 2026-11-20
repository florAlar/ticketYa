package ventas.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ventas.exception.FilaException;
import ventas.observability.Bitacora;
import ventas.redis.FilaRedisStore;
import ventas.repository.CupoSectorRepository;

/**
 * Apertura automática de tandas (FIL-02). Un lock en Redis evita que dos instancias abran la misma
 * tanda; cada evento se procesa por separado y ninguna excepción sale del scheduler.
 */
@Component
public class FilaTandaScheduler {

    private static final Logger log = LoggerFactory.getLogger(FilaTandaScheduler.class);
    private static final String PUBLICADO = "PUBLICADO";

    private final FilaRedisStore store;
    private final FilaEventoCache eventos;
    private final CupoSectorRepository cupos;
    private final boolean enabled;
    private final int tamanio;
    private final String idInstancia = UUID.randomUUID().toString();

    public FilaTandaScheduler(
            FilaRedisStore store,
            FilaEventoCache eventos,
            CupoSectorRepository cupos,
            @Value("${ticketya.fila.auto.enabled:true}") boolean enabled,
            @Value("${ticketya.fila.auto.tamanio:25}") int tamanio) {
        this.store = store;
        this.eventos = eventos;
        this.cupos = cupos;
        this.enabled = enabled;
        this.tamanio = tamanio;
    }

    @Scheduled(fixedDelayString = "${ticketya.fila.auto.intervalo-ms:10000}")
    public void tick() {
        if (!enabled) {
            return;
        }
        if (!store.adquirirLock(idInstancia)) {
            return; // otra instancia ya está corriendo este tick
        }
        try {
            for (String idEvento : store.eventos()) {
                try {
                    procesar(idEvento);
                } catch (Exception ex) {
                    log.warn("FILA tick: evento {} omitido: {}", idEvento, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("FILA tick: no se pudo recorrer el índice de eventos: {}", ex.getMessage());
        } finally {
            try {
                store.liberarLock(idInstancia);
            } catch (FilaException ex) {
                log.warn("FILA tick: no se pudo liberar el lock: {}", ex.getMessage());
            }
        }
    }

    void procesar(String idEvento) {
        if (!store.existeEvento(idEvento)) {
            store.removerEvento(idEvento);
            return;
        }
        FilaEventoCache.Info info = eventos.obtener(idEvento);
        if (!PUBLICADO.equals(info.estado()) || !info.altaDemanda()) {
            return;
        }
        FilaRedisStore.Resumen r = store.resumen(idEvento);
        long nueva = siguienteTanda(r, cupos.sumarDisponible(idEvento), tamanio);
        if (nueva <= r.tanda()) {
            return;
        }
        long resultado = store.habilitar(idEvento, nueva);
        if (resultado >= 0) { // -1: un ADMIN se adelantó y fijó una tanda mayor
            Bitacora.registrar("FILA_HABILITACION", "SISTEMA", idEvento,
                    "modo=AUTO anterior=" + r.tanda() + " nueva=" + resultado);
        }
    }

    /** min(tamanio, max(0, cupo − activos)) sin pasar de seq (CA-2.5). */
    static long siguienteTanda(FilaRedisStore.Resumen resumen, long cupo, int tamanio) {
        long espacio = cupo - resumen.activos();
        long paso = Math.min(tamanio, Math.max(0, espacio));
        return Math.min(resumen.tanda() + paso, resumen.seq());
    }
}
