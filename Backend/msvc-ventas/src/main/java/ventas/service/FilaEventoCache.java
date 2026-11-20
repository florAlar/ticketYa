package ventas.service;

import feign.FeignException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ventas.clients.EventosClient;
import ventas.clients.EventosClient.EventoDetalleFeignDTO;
import ventas.exception.FilaException;
import ventas.exception.ResourceNotFoundException;

/**
 * Cache en memoria del flag de alta demanda y del estado de publicación (D8). No cachea stock ni
 * cupos. Si msvc-eventos falla y hay dato (aunque esté vencido), se responde con él; si no hay
 * ningún dato, se decide fail-closed con 503.
 */
@Component
public class FilaEventoCache {

    public record Info(String idEvento, String estado, boolean altaDemanda) {}

    private static final class Entrada {
        final Info info;
        final long expiraEnMs;

        Entrada(Info info, long expiraEnMs) {
            this.info = info;
            this.expiraEnMs = expiraEnMs;
        }
    }

    private final EventosClient eventosClient;
    private final long ttlMs;
    private final Map<String, Entrada> cache = new ConcurrentHashMap<>();

    public FilaEventoCache(EventosClient eventosClient,
            @Value("${ticketya.fila.evento-cache-ttl-seconds:10}") long ttlSegundos) {
        this.eventosClient = eventosClient;
        this.ttlMs = ttlSegundos * 1000;
    }

    /** Sincronizado para que dos requests concurrentes dentro del TTL hagan una sola llamada Feign. */
    public synchronized Info obtener(String idEvento) {
        long ahora = System.currentTimeMillis();
        Entrada previa = cache.get(idEvento);
        if (previa != null && previa.expiraEnMs > ahora) {
            return previa.info;
        }
        try {
            EventoDetalleFeignDTO dto = eventosClient.getEvento(idEvento);
            if (dto == null) {
                throw new ResourceNotFoundException("Evento no encontrado: " + idEvento);
            }
            Info info = new Info(idEvento, dto.getEstado(), Boolean.TRUE.equals(dto.getEsAltaDemanda()));
            cache.put(idEvento, new Entrada(info, ahora + ttlMs));
            return info;
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Evento no encontrado: " + idEvento);
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            if (previa != null) {
                return previa.info; // dato viejo antes que bloquear el acceso
            }
            throw FilaException.noDisponible(ex);
        }
    }
}
