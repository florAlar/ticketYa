package ventas.service;

import feign.FeignException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
 *
 * <p>La carga es <em>single-flight por evento</em>: una sola llamada Feign por evento y por
 * vencimiento, sin serializar el tráfico de eventos distintos. El mapa está acotado y expulsa
 * entradas vencidas cuando supera {@link #MAX_EVENTOS}.</p>
 */
@Component
public class FilaEventoCache {

    public record Info(String idEvento, String estado, boolean altaDemanda) {}

    private static final int MAX_EVENTOS = 5_000;

    /** Algo por encima de connect (2 s) + read (3 s) de Feign para no cortar antes que el cliente. */
    private static final long ESPERA_MAX_MS = 6_000;

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
    private final Map<String, CompletableFuture<Info>> enVuelo = new ConcurrentHashMap<>();

    public FilaEventoCache(EventosClient eventosClient,
            @Value("${ticketya.fila.evento-cache-ttl-seconds:10}") long ttlSegundos) {
        this.eventosClient = eventosClient;
        this.ttlMs = ttlSegundos * 1000;
    }

    public Info obtener(String idEvento) {
        Info vigente = vigente(idEvento);
        if (vigente != null) {
            return vigente;
        }

        CompletableFuture<Info> propia = new CompletableFuture<>();
        CompletableFuture<Info> enCurso = enVuelo.putIfAbsent(idEvento, propia);
        if (enCurso != null) {
            return esperarEnVuelo(enCurso);
        }

        try {
            Info info = consultar(idEvento);
            guardar(idEvento, info);
            propia.complete(info);
            return info;
        } catch (RuntimeException ex) {
            Entrada previa = cache.get(idEvento);
            if (previa != null && !(ex instanceof ResourceNotFoundException)) {
                propia.complete(previa.info); // dato viejo antes que bloquear el acceso
                return previa.info;
            }
            RuntimeException salida = (ex instanceof ResourceNotFoundException || ex instanceof FilaException)
                    ? ex
                    : FilaException.noDisponible(ex);
            propia.completeExceptionally(salida);
            throw salida;
        } finally {
            enVuelo.remove(idEvento, propia);
        }
    }

    /** Descarta el dato en caché: la próxima consulta vuelve a msvc-eventos. */
    public void invalidar(String idEvento) {
        if (idEvento != null) {
            cache.remove(idEvento);
        }
    }

    private Info vigente(String idEvento) {
        Entrada entrada = cache.get(idEvento);
        return entrada != null && entrada.expiraEnMs > System.currentTimeMillis()
                ? entrada.info
                : null;
    }

    private Info consultar(String idEvento) {
        try {
            EventoDetalleFeignDTO dto = eventosClient.getEvento(idEvento);
            if (dto == null) {
                throw new ResourceNotFoundException("Evento no encontrado: " + idEvento);
            }
            return new Info(idEvento, dto.getEstado(), Boolean.TRUE.equals(dto.getEsAltaDemanda()));
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Evento no encontrado: " + idEvento);
        }
    }

    private void guardar(String idEvento, Info info) {
        cache.put(idEvento, new Entrada(info, System.currentTimeMillis() + ttlMs));
        if (cache.size() <= MAX_EVENTOS) {
            return;
        }
        long ahora = System.currentTimeMillis();
        cache.entrySet().removeIf(e -> e.getValue().expiraEnMs <= ahora);
        for (String clave : cache.keySet()) {
            if (cache.size() <= MAX_EVENTOS) {
                break;
            }
            cache.remove(clave);
        }
    }

    /** Espera la carga que ya está en curso para el mismo evento, sin hacer otra llamada Feign. */
    private Info esperarEnVuelo(CompletableFuture<Info> enCurso) {
        try {
            return enCurso.get(ESPERA_MAX_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            throw FilaException.noDisponible(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw FilaException.noDisponible(ex);
        } catch (ExecutionException ex) {
            Throwable causa = ex.getCause();
            if (causa instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw FilaException.noDisponible(causa);
        }
    }
}
