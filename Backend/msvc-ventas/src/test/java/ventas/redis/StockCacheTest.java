package ventas.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class StockCacheTest {

    @Test
    void putReservaHold_redisOk_noPropagaExcepcion() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        doNothing().when(ops).set(anyString(), anyString(), any(Duration.class));

        StockCache cache = new StockCache(redis);
        cache.putReservaHold("res-1", "cli-1", Duration.ofMinutes(10));

        verify(ops).set("ventas:reserva:res-1", "cli-1", Duration.ofMinutes(10));
    }

    @Test
    void putReservaHold_redisLanzaExcepcion_noPropaga() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        doThrow(new RuntimeException("redis caído")).when(ops).set(anyString(), anyString(), any(Duration.class));

        StockCache cache = new StockCache(redis);
        // No debe lanzar excepción
        cache.putReservaHold("res-1", "cli-1", Duration.ofMinutes(10));
    }

    @Test
    void renewReservaHold_conClave_renuevaTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.hasKey("ventas:reserva:res-1")).thenReturn(true);

        StockCache cache = new StockCache(redis);
        cache.renewReservaHold("res-1", Duration.ofMinutes(10));

        verify(redis).expire("ventas:reserva:res-1", Duration.ofMinutes(10));
    }

    @Test
    void renewReservaHold_sinClave_estableceValorDefecto() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(redis.hasKey("ventas:reserva:res-1")).thenReturn(false);

        StockCache cache = new StockCache(redis);
        cache.renewReservaHold("res-1", Duration.ofMinutes(10));

        verify(ops).set("ventas:reserva:res-1", "1", Duration.ofMinutes(10));
        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void releaseReservaHold_eliminaClave() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        StockCache cache = new StockCache(redis);
        cache.releaseReservaHold("res-1");

        verify(redis).delete("ventas:reserva:res-1");
    }

    @Test
    void getIdempotentCompraId_conValor_devuelveOptional() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("ventas:idem:reserva:key-1")).thenReturn("compra-1");

        StockCache cache = new StockCache(redis);
        Optional<String> result = cache.getIdempotentCompraId("reserva:key-1");

        assertTrue(result.isPresent());
        assertEquals("compra-1", result.get());
    }

    @Test
    void getIdempotentCompraId_sinValor_devuelveVacio() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("ventas:idem:reserva:key-1")).thenReturn(null);

        StockCache cache = new StockCache(redis);
        Optional<String> result = cache.getIdempotentCompraId("reserva:key-1");

        assertFalse(result.isPresent());
    }

    @Test
    void getIdempotentCompraId_redisLanzaExcepcion_devuelveVacio() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenThrow(new RuntimeException("redis caído"));

        StockCache cache = new StockCache(redis);
        Optional<String> result = cache.getIdempotentCompraId("reserva:key-1");

        assertFalse(result.isPresent());
    }

    @Test
    void putIdempotentCompraId_guardaConTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        StockCache cache = new StockCache(redis);
        cache.putIdempotentCompraId("reserva:key-1", "compra-1", Duration.ofHours(24));

        verify(ops).set("ventas:idem:reserva:key-1", "compra-1", Duration.ofHours(24));
    }
}
