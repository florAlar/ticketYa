package ventas.redis;

import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Hold de reserva en Redis (TTL) + idempotencia de compras.
 * La fuente de verdad del cupo sigue siendo MySQL (lock pesimista).
 */
@Component
public class StockCache {

    private static final Logger log = LoggerFactory.getLogger(StockCache.class);

    private static final String KEY_RESERVA = "ventas:reserva:";
    private static final String KEY_IDEMPOTENCY = "ventas:idem:";

    private final StringRedisTemplate redis;

    public StockCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void putReservaHold(String idReserva, String idCliente, Duration ttl) {
        try {
            redis.opsForValue().set(KEY_RESERVA + idReserva, idCliente, ttl);
        } catch (Exception e) {
            log.warn("Redis putReservaHold falló (se sigue con MySQL TTL): {}", e.getMessage());
        }
    }

    public void renewReservaHold(String idReserva, Duration ttl) {
        try {
            String key = KEY_RESERVA + idReserva;
            if (Boolean.TRUE.equals(redis.hasKey(key))) {
                redis.expire(key, ttl);
            } else {
                // si no estaba, no inventamos cliente; el caller ya validó MySQL
                redis.opsForValue().set(key, "1", ttl);
            }
        } catch (Exception e) {
            log.warn("Redis renewReservaHold falló: {}", e.getMessage());
        }
    }

    public void releaseReservaHold(String idReserva) {
        try {
            redis.delete(KEY_RESERVA + idReserva);
        } catch (Exception e) {
            log.warn("Redis releaseReservaHold falló: {}", e.getMessage());
        }
    }

    public Optional<String> getIdempotentCompraId(String key) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(KEY_IDEMPOTENCY + key));
        } catch (Exception e) {
            log.warn("Redis getIdempotentCompraId falló: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void putIdempotentCompraId(String key, String idCompra, Duration ttl) {
        try {
            redis.opsForValue().set(KEY_IDEMPOTENCY + key, idCompra, ttl);
        } catch (Exception e) {
            log.warn("Redis putIdempotentCompraId falló: {}", e.getMessage());
        }
    }
}
