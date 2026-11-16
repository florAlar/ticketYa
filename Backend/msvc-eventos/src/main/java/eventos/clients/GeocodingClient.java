package eventos.clients;

import eventos.dto.CoordenadasDTO;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cliente de geocodificación con Caché con TTL (E2-NFR-01) y Resiliencia:
 * timeout por intento, reintentos acotados y fallback best-effort (E2-NFR-02).
 * Delega la consulta real o simulada al {@link GeocodingProvider} configurado.
 */
@Component
public class GeocodingClient {

    private static final Logger log = LoggerFactory.getLogger(GeocodingClient.class);

    private final GeocodingProvider provider;
    private final boolean enabled;
    private final long timeoutMs;
    private final int maxRetries;
    private final Duration cacheTtl;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public GeocodingClient(
            GeocodingProvider provider,
            @Value("${ticketya.geocoding.enabled:true}") boolean enabled,
            @Value("${ticketya.geocoding.timeout-ms:2000}") long timeoutMs,
            @Value("${ticketya.geocoding.max-retries:2}") int maxRetries,
            @Value("${ticketya.geocoding.cache-ttl-minutes:60}") long cacheTtlMinutes) {
        this.provider = provider;
        this.enabled = enabled;
        this.timeoutMs = Math.max(10L, timeoutMs);
        this.maxRetries = Math.max(0, maxRetries);
        this.cacheTtl = Duration.ofMinutes(Math.max(1L, cacheTtlMinutes));
    }

    /** Construye un cliente mock con valores por defecto para uso directo en tests de servicio. */
    public static GeocodingClient defaultMock() {
        return new GeocodingClient(new MockGeocodingAdapter(), true, 2000L, 2, 60L);
    }

    public Optional<CoordenadasDTO> resolver(String direccion, String ciudad) {
        if (!enabled || provider == null || ciudad == null || ciudad.isBlank()) {
            return Optional.empty();
        }
        String key = cacheKey(direccion, ciudad);
        Instant now = Instant.now();
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            log.debug("Geocoding cache HIT [provider={}] key={}", provider.providerName(), key);
            return cached.value();
        }
        if (cached != null) {
            cache.remove(key, cached);
        }

        int totalIntentos = maxRetries + 1;
        for (int intento = 1; intento <= totalIntentos; intento++) {
            try {
                Optional<CoordenadasDTO> resultado = ejecutarConTimeout(direccion, ciudad);
                cache.put(key, new CacheEntry(resultado, Instant.now().plus(cacheTtl)));
                return resultado;
            } catch (TimeoutException te) {
                log.warn("Geocoding timeout ({} ms) en intento {}/{} [provider={}] ciudad='{}'",
                        timeoutMs, intento, totalIntentos, provider.providerName(), ciudad);
            } catch (Exception ex) {
                log.warn("Geocoding error en intento {}/{} [provider={}] ciudad='{}': {}",
                        intento, totalIntentos, provider.providerName(), ciudad, ex.getMessage());
            }
        }
        log.warn("Geocoding fallback a vacío tras {} intentos [provider={}] ciudad='{}'",
                totalIntentos, provider.providerName(), ciudad);
        return Optional.empty();
    }

    public void limpiarCache() {
        cache.clear();
    }

    public int tamanoCache() {
        return cache.size();
    }

    private Optional<CoordenadasDTO> ejecutarConTimeout(String direccion, String ciudad) throws Exception {
        CompletableFuture<Optional<CoordenadasDTO>> future =
                CompletableFuture.supplyAsync(() -> provider.geocode(direccion, ciudad));
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            future.cancel(true);
            throw te;
        } catch (java.util.concurrent.ExecutionException ee) {
            Throwable cause = ee.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            throw new RuntimeException(cause);
        }
    }

    private static String cacheKey(String direccion, String ciudad) {
        String dir = direccion == null ? "" : direccion.trim().toLowerCase(Locale.ROOT);
        String ciu = ciudad == null ? "" : ciudad.trim().toLowerCase(Locale.ROOT);
        return dir + "|" + ciu;
    }

    private record CacheEntry(Optional<CoordenadasDTO> value, Instant expiresAt) {}
}
