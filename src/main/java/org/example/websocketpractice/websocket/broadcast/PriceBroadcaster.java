package org.example.websocketpractice.websocket.broadcast;

import org.example.websocketpractice.stock.domain.StockPrice;
import org.example.websocketpractice.websocket.protocol.ServerEvent;

/**
 * 가격/이벤트를 구독자 세션에 전달하는 추상화. 단일 인스턴스 구현은 {@link LocalPriceBroadcaster}이며,
 * 분산 환경에서는 인스턴스 간 fan-out을 위해 Redis Pub/Sub 등 브로커 기반 구현으로 교체된다.
 *
 * <p>분산 환경에서 깨지는 지점은 websocket/SINGLE_INSTANCE_NOTES.md 참고.
 */
public interface PriceBroadcaster {

    void broadcastPrice(StockPrice price);

    void sendTo(String sessionId, ServerEvent event);
}
