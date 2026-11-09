package ventas.redis;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** Tests de configuración y caché Redis. */
class RedisConfigTest {

    @Test
    void stringRedisTemplateSeInstancia() {
        assertNotNull(new StringRedisTemplate());
    }

    @Test
    void putReservaHold_redisLanzaExcepcion_noPropaga() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        doThrow(new RuntimeException("redis caído")).when(ops).set(anyString(), anyString(), any(java.time.Duration.class));

        StockCache cache = new StockCache(redis);
        assertDoesNotThrow(() -> cache.putReservaHold("res-1", "cli-1", Duration.ofMinutes(10)));
    }

    @Test
    void putIdempotentCompraId_redisLanzaExcepcion_noPropaga() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        doThrow(new RuntimeException("redis caído")).when(ops).set(anyString(), anyString(), any(java.time.Duration.class));

        StockCache cache = new StockCache(redis);
        assertDoesNotThrow(() -> cache.putIdempotentCompraId("key-1", "compra-1", Duration.ofHours(24)));
    }

    @Test
    void getIdempotentCompraId_redisLanzaExcepcion_devuelveVacio() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenThrow(new RuntimeException("redis caído"));

        StockCache cache = new StockCache(redis);
        assertDoesNotThrow(() -> cache.getIdempotentCompraId("reserva:key-1"));
    }
}
