package org.example.websocketpractice.websocket.broadcast;

/**
 * 활성 심볼에 대한 가격 폴링 루프 추상화. 테스트에서는 스케줄러 대신 {@link #tick()}를 직접 호출한다.
 */
@Deprecated(since = "YAGNI 위반: 구현체 1개(ScheduledPriceFetchLoop), tick()이 public이라 인터페이스 없이 직접 주입 가능. YAGNI.md #1 참고")
public interface PriceFetchLoop {

    void tick();
}
