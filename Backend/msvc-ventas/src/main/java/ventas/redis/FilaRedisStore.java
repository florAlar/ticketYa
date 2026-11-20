package ventas.redis;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import ventas.exception.FilaException;

/**
 * Store de la fila virtual. Todo el estado vive en Redis (D1); las decisiones atómicas se toman
 * en scripts Lua y cualquier fallo de Redis se traduce en {@code 503 FILA_NO_DISPONIBLE} (D3).
 */
@Component
public class FilaRedisStore {

    // KEYS: zset, seq, tanda, usados, índice de eventos
    // ARGV: userId, ttlSegundos, idEvento -> {posicion, creado, tanda} | {-1} si el turno ya se usó
    private static final RedisScript<List> SCRIPT_INGRESAR = new DefaultRedisScript<>("""
            if redis.call('SISMEMBER', KEYS[4], ARGV[1]) == 1 then return {-1} end
            local pos = redis.call('ZSCORE', KEYS[1], ARGV[1])
            local creado = 0
            local n
            if pos then
              n = tonumber(pos)
            else
              n = redis.call('INCR', KEYS[2])
              redis.call('ZADD', KEYS[1], n, ARGV[1])
              creado = 1
            end
            redis.call('SET', KEYS[3], 0, 'NX')
            for i = 1, 4 do redis.call('EXPIRE', KEYS[i], ARGV[2]) end
            redis.call('SADD', KEYS[5], ARGV[3])
            return {n, creado, redis.call('GET', KEYS[3])}
            """, List.class);

    // KEYS: tanda, zset, seq, usados · ARGV: hastaPosicion, ttlSegundos -> nueva tanda | -1 si retrocede
    private static final RedisScript<Long> SCRIPT_HABILITAR = new DefaultRedisScript<>("""
            local actual = tonumber(redis.call('GET', KEYS[1]) or '0')
            local pedido = tonumber(ARGV[1])
            if pedido < actual then return -1 end
            redis.call('SET', KEYS[1], pedido)
            for i = 1, 4 do redis.call('EXPIRE', KEYS[i], ARGV[2]) end
            return pedido
            """, Long.class);

    // KEYS: lock · ARGV: idInstancia -> 1 si se liberó, 0 si ya no era de esta instancia
    private static final RedisScript<Long> SCRIPT_UNLOCK = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end
            return 0
            """, Long.class);

    // KEYS: zset, tanda, token, usados · ARGV: userId, tokenNuevo, ttlTokenSegundos
    // Retorno: {codigo, posicion, tanda, token, ttlRestante}; código 1 ok | -1 usado | -2 sin ingreso | -3 fuera
    private static final RedisScript<List> SCRIPT_VALIDAR = new DefaultRedisScript<>("""
            if redis.call('SISMEMBER', KEYS[4], ARGV[1]) == 1 then return {-1} end
            local pos = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if not pos then return {-2} end
            pos = tonumber(pos)
            local tanda = tonumber(redis.call('GET', KEYS[2]) or '0')
            if pos > tanda then return {-3, pos, tanda} end
            local t = redis.call('GET', KEYS[3])
            if not t then
              redis.call('SET', KEYS[3], ARGV[2], 'EX', ARGV[3])
              t = ARGV[2]
            end
            return {1, pos, tanda, t, redis.call('TTL', KEYS[3])}
            """, List.class);

    // KEYS: token, usados, zset · ARGV: token, userId -> 1 consumido | 0 inexistente/vencido/ajeno
    private static final RedisScript<Long> SCRIPT_CONSUMIR_TOKEN = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              redis.call('DEL', KEYS[1])
              redis.call('SADD', KEYS[2], ARGV[2])
              local ttl = redis.call('PTTL', KEYS[3])
              if ttl > 0 then redis.call('PEXPIRE', KEYS[2], ttl) end
              return 1
            end
            return 0
            """, Long.class);

    // KEYS: token, usados · ARGV: token, userId, ttlTokenSegundos -> 1 (rollback de la reserva)
    private static final RedisScript<Long> SCRIPT_RESTAURAR_TOKEN = new DefaultRedisScript<>("""
            redis.call('SREM', KEYS[2], ARGV[2])
            redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final String prefix;
    private final long ttlSegundos;
    private final long tokenTtlSegundos;
    private final Duration lockTtl;

    public FilaRedisStore(
            StringRedisTemplate redis,
            @Value("${ticketya.fila.key-prefix:fila}") String prefix,
            @Value("${ticketya.fila.ttl-hours:24}") long ttlHours,
            @Value("${ticketya.fila.token-ttl-minutes:10}") long tokenTtlMinutes,
            @Value("${ticketya.fila.auto.lock-ms:8000}") long lockMs) {
        this.redis = redis;
        this.prefix = prefix;
        this.ttlSegundos = ttlHours * 3600;
        this.tokenTtlSegundos = tokenTtlMinutes * 60;
        this.lockTtl = Duration.ofMillis(lockMs);
    }

    public record Ingreso(long posicion, boolean creado, long tanda) {}

    /** posicion == null cuando el cliente no ingresó. */
    public record Posicion(Long posicion, long tanda, boolean conToken, boolean usado) {}

    /** total = ZCARD - SCARD(usados); tanda = última posición habilitada. */
    public record Estado(long totalEnFila, long tanda) {}

    /** Resumen para el job: último turno, tanda y habilitados que aún no usaron su turno. */
    public record Resumen(long seq, long tanda, long activos) {}

    /** Resultado de una validación: turno, tanda vigente, token (reutilizado o nuevo) y caducidad. */
    public record Validacion(long posicion, long tanda, String token, java.time.Instant expiraEn) {}

    public String zsetKey(String idEvento) {
        return prefix + ":" + idEvento;
    }

    public String seqKey(String idEvento) {
        return zsetKey(idEvento) + ":seq";
    }

    public String tandaKey(String idEvento) {
        return zsetKey(idEvento) + ":tanda";
    }

    public String usadosKey(String idEvento) {
        return zsetKey(idEvento) + ":usados";
    }

    public String tokenKey(String idEvento, String userId) {
        return zsetKey(idEvento) + ":token:" + userId;
    }

    public String eventosKey() {
        return prefix + ":eventos";
    }

    public String lockKey() {
        return prefix + ":auto:lock";
    }

    public long ttlSegundos() {
        return ttlSegundos;
    }

    public long tokenTtlSegundos() {
        return tokenTtlSegundos;
    }

    /** Idempotente: devuelve la posición existente y si fue creada en esta llamada. */
    public Ingreso ingresar(String idEvento, String userId) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> out = (List<Object>) redis.execute(SCRIPT_INGRESAR,
                    List.of(zsetKey(idEvento), seqKey(idEvento), tandaKey(idEvento),
                            usadosKey(idEvento), eventosKey()),
                    userId, String.valueOf(ttlSegundos), idEvento);
            long posicion = ((Number) out.get(0)).longValue();
            if (posicion < 0) {
                throw FilaException.turnoUsado();
            }
            long tanda = Long.parseLong(String.valueOf(out.get(2)));
            return new Ingreso(posicion, ((Number) out.get(1)).longValue() == 1, tanda);
        } catch (DataAccessException ex) {
            throw FilaException.noDisponible(ex);
        }
    }

    /** Lectura no atómica, suficiente para mostrar datos al cliente. */
    public Posicion posicion(String idEvento, String userId) {
        try {
            Double score = redis.opsForZSet().score(zsetKey(idEvento), userId);
            String tanda = redis.opsForValue().get(tandaKey(idEvento));
            boolean conToken = Boolean.TRUE.equals(redis.hasKey(tokenKey(idEvento, userId)));
            boolean usado = Boolean.TRUE.equals(redis.opsForSet().isMember(usadosKey(idEvento), userId));
            return new Posicion(score == null ? null : score.longValue(),
                    tanda == null ? 0 : Long.parseLong(tanda), conToken, usado);
        } catch (DataAccessException ex) {
            throw FilaException.noDisponible(ex);
        }
    }

    /** Lectura no atómica y pública: no expone identidades. */
    public Estado estado(String idEvento) {
        try {
            Long enFila = redis.opsForZSet().size(zsetKey(idEvento));
            Long usados = redis.opsForSet().size(usadosKey(idEvento));
            String tanda = redis.opsForValue().get(tandaKey(idEvento));
            return new Estado(Math.max(0, (enFila == null ? 0 : enFila) - (usados == null ? 0 : usados)),
                    tanda == null ? 0 : Long.parseLong(tanda));
        } catch (DataAccessException ex) {
            throw FilaException.noDisponible(ex);
        }
    }

    /** Fija la tanda; la tanda nunca retrocede. Devuelve -1 si se pidió un valor menor. */
    public long habilitar(String idEvento, long hastaPosicion) {
        try {
            Long nueva = redis.execute(SCRIPT_HABILITAR,
                    List.of(tandaKey(idEvento), zsetKey(idEvento), seqKey(idEvento), usadosKey(idEvento)),
                    String.valueOf(hastaPosicion), String.valueOf(ttlSegundos));
            return nueva == null ? -1 : nueva;
        } catch (DataAccessException ex) {
            throw FilaException.noDisponible(ex);
        }
    }

    /** Lecturas del job, no atómicas entre sí; las decisiones se toman en `habilitar`. */
    public Resumen resumen(String idEvento) {
        try {
            String seq = redis.opsForValue().get(seqKey(idEvento));
            String tanda = redis.opsForValue().get(tandaKey(idEvento));
            long t = tanda == null ? 0 : Long.parseLong(tanda);
            Long habilitados = redis.opsForZSet().count(zsetKey(idEvento), 0, t);
            Long usados = redis.opsForSet().size(usadosKey(idEvento));
            return new Resumen(seq == null ? 0 : Long.parseLong(seq), t,
                    Math.max(0, (habilitados == null ? 0 : habilitados) - (usados == null ? 0 : usados)));
        } catch (DataAccessException ex) {
            throw FilaException.noDisponible(ex);
        }
    }

    /** Índice de eventos con fila; el job lo limpia cuando el ZSET ya no existe. */
    public Set<String> eventos() {
        try {
            Set<String> miembros = redis.opsForSet().members(eventosKey());
            return miembros == null ? Set.of() : miembros;
        } catch (DataAccessException ex) {
            throw FilaException.noDisponible(ex);
        }
    }

    public void removerEvento(String idEvento) {
        try {
            redis.opsForSet().remove(eventosKey(), idEvento);
        } catch (DataAccessException ex) {
            throw FilaException.noDisponible(ex);
        }
    }

    public boolean existeEvento(String idEvento) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(zsetKey(idEvento)));
        } catch (DataAccessException ex) {
            throw FilaException.noDisponible(ex);
        }
    }

    /** Lock distribuido del job: solo la instancia que lo obtiene abre tandas en este tick. */
    public boolean adquirirLock(String idInstancia) {
        try {
            return Boolean.TRUE.equals(
                    redis.opsForValue().setIfAbsent(lockKey(), idInstancia, lockTtl));
        } catch (DataAccessException ex) {
            throw FilaException.noDisponible(ex);
        }
    }

    public void liberarLock(String idInstancia) {
        try {
            redis.execute(SCRIPT_UNLOCK, List.of(lockKey()), idInstancia);
        } catch (DataAccessException ex) {
            throw FilaException.noDisponible(ex);
        }
    }

    /**
     * Emite (o reutiliza) el token de un cliente habilitado. El script es atómico: dos validaciones
     * concurrentes con el mismo turno devuelven el mismo token mientras esté vigente.
     */
    public Validacion validar(String idEvento, String userId, String tokenNuevo) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> out = (List<Object>) redis.execute(SCRIPT_VALIDAR,
                    List.of(zsetKey(idEvento), tandaKey(idEvento), tokenKey(idEvento, userId),
                            usadosKey(idEvento)),
                    userId, tokenNuevo, String.valueOf(tokenTtlSegundos));
            long codigo = ((Number) out.get(0)).longValue();
            if (codigo == -1) {
                throw FilaException.turnoUsado();
            }
            if (codigo == -2) {
                throw FilaException.noIngresado();
            }
            if (codigo == -3) {
                throw FilaException.fueraDeTanda(
                        ((Number) out.get(1)).longValue(), ((Number) out.get(2)).longValue());
            }
            long ttl = ((Number) out.get(4)).longValue();
            if (ttl < 0) {
                ttl = tokenTtlSegundos;
            }
            return new Validacion(((Number) out.get(1)).longValue(), ((Number) out.get(2)).longValue(),
                    String.valueOf(out.get(3)), java.time.Instant.now().plusSeconds(ttl));
        } catch (DataAccessException ex) {
            throw FilaException.noDisponible(ex);
        }
    }

    /** Compare-and-delete atómico: un token solo puede consumirse una vez (CA-3.6). */
    public boolean consumirToken(String idEvento, String userId, String token) {
        try {
            Long consumido = redis.execute(SCRIPT_CONSUMIR_TOKEN,
                    List.of(tokenKey(idEvento, userId), usadosKey(idEvento), zsetKey(idEvento)),
                    token, userId);
            return consumido != null && consumido == 1L;
        } catch (DataAccessException ex) {
            throw FilaException.noDisponible(ex);
        }
    }

    /** Rollback: devuelve el turno y reemite el token con TTL completo. */
    public void restaurarToken(String idEvento, String userId, String token) {
        try {
            redis.execute(SCRIPT_RESTAURAR_TOKEN,
                    List.of(tokenKey(idEvento, userId), usadosKey(idEvento)),
                    token, userId, String.valueOf(tokenTtlSegundos));
        } catch (DataAccessException ex) {
            throw FilaException.noDisponible(ex);
        }
    }
}
