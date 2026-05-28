package org.example.websocketpractice.websocket.subscription;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RedisSubscriptionRegistry}가 {@link InMemorySubscriptionRegistry}와 동일한 계약을 만족하는지
 * 검증하는 contract test. 두 구현은 같은 시나리오에서 동일하게 동작해야 한다(인터페이스 무수정 교체 보장).
 */
@Testcontainers
class RedisSubscriptionRegistryTest {

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private RedisSubscriptionRegistry registry;

    @BeforeAll
    static void startFactory() {
        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
    }

    @AfterAll
    static void stopFactory() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        template.getRequiredConnectionFactory().getConnection().serverCommands().flushAll();
        registry = new RedisSubscriptionRegistry(template);
    }

    @Test
    void subscribe_then_sessionsOf_returnsSession() {
        // when
        registry.subscribe("session-1", "005930");

        // then
        assertThat(registry.sessionsOf("005930")).containsExactly("session-1");
        assertThat(registry.activeSymbols()).containsExactly("005930");
        assertThat(registry.symbolCountOf("session-1")).isEqualTo(1);
    }

    @Test
    void unsubscribe_removesSession_andEvictsSymbol_whenLastSubscriber() {
        // given
        registry.subscribe("session-1", "005930");

        // when
        registry.unsubscribe("session-1", "005930");

        // then
        assertThat(registry.sessionsOf("005930")).isEmpty();
        assertThat(registry.activeSymbols()).isEmpty();
        assertThat(registry.symbolCountOf("session-1")).isZero();
    }

    @Test
    void removeSession_clearsAllBindings_whenSessionSubscribedToMultipleSymbols() {
        // given
        registry.subscribe("session-1", "005930");
        registry.subscribe("session-1", "000660");
        registry.subscribe("session-2", "005930");

        // when
        registry.removeSession("session-1");

        // then
        assertThat(registry.sessionsOf("005930")).containsExactly("session-2");
        assertThat(registry.sessionsOf("000660")).isEmpty();
        assertThat(registry.activeSymbols()).containsExactly("005930");
        assertThat(registry.symbolCountOf("session-1")).isZero();
    }

    @Test
    void concurrentSubscribeUnsubscribe_keepsConsistentState_whenManyThreadsAccess() throws InterruptedException {
        // given
        int threads = 16;
        int iterations = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            String sessionId = "session-" + t;
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        registry.subscribe(sessionId, "005930");
                        registry.unsubscribe(sessionId, "005930");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        // when
        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(registry.sessionsOf("005930")).isEmpty();
        assertThat(registry.activeSymbols()).isEmpty();
    }
}
