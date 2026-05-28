package org.example.websocketpractice.websocket.subscription;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code stock.ws.mode=distributed}일 때 Spring 컨텍스트가 {@link RedisSubscriptionRegistry}를 주입하고
 * {@link InMemorySubscriptionRegistry}를 비활성화하는지 검증한다. 인터페이스 무수정 + 구현체 교체 원칙이
 * 실제 DI 레벨에서 동작함을 확인한다.
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "stock.ws.mode=distributed",
        "kis.app-key=test-key",
        "kis.app-secret=test-secret"
})
class DistributedProfileWiringTest {

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private SubscriptionRegistry subscriptionRegistry;

    @Autowired
    private ApplicationContext context;

    @Test
    void distributedMode_wiresRedisRegistry_andDisablesInMemory() {
        // then
        assertThat(subscriptionRegistry).isInstanceOf(RedisSubscriptionRegistry.class);
        assertThat(context.getBeansOfType(InMemorySubscriptionRegistry.class)).isEmpty();
    }
}
