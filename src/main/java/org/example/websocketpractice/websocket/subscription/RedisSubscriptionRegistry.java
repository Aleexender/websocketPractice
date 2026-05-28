package org.example.websocketpractice.websocket.subscription;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 구독 상태를 Redis 공유 저장소에 보관하는 분산 구현. {@code stock.ws.mode=distributed}일 때만 활성화된다.
 *
 * <p>자료구조: symbol→sessionIds({@code subs:symbol:*}), sessionId→symbols({@code subs:session:*} 역인덱스),
 * 활성 심볼 인덱스({@code subs:symbols:index}). 역인덱스로 {@code removeSession} cleanup을 O(구독 수)로 유지한다.
 * 다단계 변경(subscribe/unsubscribe/removeSession)은 정/역/인덱스 불일치를 막기 위해 Lua로 원자 실행한다.
 *
 * <p>단일 Redis 가정. Redis Cluster로 확장 시 동적 키(symbol 키)의 cross-slot 문제는 hash tag로 해소해야 한다.
 */
@Component
@ConditionalOnProperty(name = "stock.ws.mode", havingValue = "distributed")
public class RedisSubscriptionRegistry implements SubscriptionRegistry {

    private static final String SYMBOL_KEY_PREFIX = "subs:symbol:";
    private static final String SESSION_KEY_PREFIX = "subs:session:";
    private static final String SYMBOLS_INDEX = "subs:symbols:index";

    private static final RedisScript<Long> SUBSCRIBE = RedisScript.of("""
            redis.call('SADD', KEYS[1], ARGV[1])
            redis.call('SADD', KEYS[2], ARGV[2])
            redis.call('SADD', KEYS[3], ARGV[2])
            return 1
            """, Long.class);

    private static final RedisScript<Long> UNSUBSCRIBE = RedisScript.of("""
            redis.call('SREM', KEYS[1], ARGV[1])
            redis.call('SREM', KEYS[2], ARGV[2])
            if redis.call('SCARD', KEYS[1]) == 0 then
                redis.call('SREM', KEYS[3], ARGV[2])
            end
            return 1
            """, Long.class);

    private static final RedisScript<Long> REMOVE_SESSION = RedisScript.of("""
            local symbols = redis.call('SMEMBERS', KEYS[1])
            for _, sym in ipairs(symbols) do
                local symKey = ARGV[2] .. sym
                redis.call('SREM', symKey, ARGV[1])
                if redis.call('SCARD', symKey) == 0 then
                    redis.call('SREM', KEYS[2], sym)
                end
            end
            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisSubscriptionRegistry(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void subscribe(String sessionId, String symbol) {
        redis.execute(SUBSCRIBE,
                List.of(symbolKey(symbol), sessionKey(sessionId), SYMBOLS_INDEX),
                sessionId, symbol);
    }

    @Override
    public void unsubscribe(String sessionId, String symbol) {
        redis.execute(UNSUBSCRIBE,
                List.of(symbolKey(symbol), sessionKey(sessionId), SYMBOLS_INDEX),
                sessionId, symbol);
    }

    @Override
    public void removeSession(String sessionId) {
        redis.execute(REMOVE_SESSION,
                List.of(sessionKey(sessionId), SYMBOLS_INDEX),
                sessionId, SYMBOL_KEY_PREFIX);
    }

    @Override
    public Set<String> sessionsOf(String symbol) {
        Set<String> sessions = redis.opsForSet().members(symbolKey(symbol));
        return (sessions == null) ? Set.of() : Set.copyOf(sessions);
    }

    @Override
    public Set<String> activeSymbols() {
        Set<String> symbols = redis.opsForSet().members(SYMBOLS_INDEX);
        return (symbols == null) ? Set.of() : Set.copyOf(symbols);
    }

    @Override
    public int symbolCountOf(String sessionId) {
        Long count = redis.opsForSet().size(sessionKey(sessionId));
        return (count == null) ? 0 : count.intValue();
    }

    private String symbolKey(String symbol) {
        return SYMBOL_KEY_PREFIX + symbol;
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }
}
