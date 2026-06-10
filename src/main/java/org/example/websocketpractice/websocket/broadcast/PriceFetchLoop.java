package org.example.websocketpractice.websocket.broadcast;

/**
 * 활성 심볼에 대한 가격 폴링 루프 추상화. 테스트에서는 스케줄러 대신 {@link #tick()}를 직접 호출한다.
 */
public interface PriceFetchLoop {

    void tick();
}
