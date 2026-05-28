package org.example.websocketpractice.websocket.subscription;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySubscriptionRegistryTest {

    @Test
    void subscribe_then_sessionsOf_returnsSession() {
        // given
        InMemorySubscriptionRegistry registry = new InMemorySubscriptionRegistry();

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
        InMemorySubscriptionRegistry registry = new InMemorySubscriptionRegistry();
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
        InMemorySubscriptionRegistry registry = new InMemorySubscriptionRegistry();
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
        InMemorySubscriptionRegistry registry = new InMemorySubscriptionRegistry();
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
