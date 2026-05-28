package org.example.websocketpractice.websocket.subscription;

import java.util.Set;

/**
 * 구독 상태를 관리하는 추상화. 단일 인스턴스 한정 동작은 {@link InMemorySubscriptionRegistry}가 담당하며,
 * 분산 환경에서는 Redis 등 공유 저장소 기반 구현으로 교체된다.
 *
 * <p>분산 환경에서 깨지는 지점은 websocket/SINGLE_INSTANCE_NOTES.md 참고.
 */
public interface SubscriptionRegistry {

    void subscribe(String sessionId, String symbol);

    void unsubscribe(String sessionId, String symbol);

    void removeSession(String sessionId);

    Set<String> sessionsOf(String symbol);

    Set<String> activeSymbols();

    int symbolCountOf(String sessionId);
}
