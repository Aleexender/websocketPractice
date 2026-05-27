package org.example.websocketpractice.websocket.subscription;

import org.springframework.stereotype.Component;

@Component
@Deprecated(since = "YAGNI 위반: 룰 1개(count >= 20)만 검증, 호출자 1곳(StockWebSocketHandler). 두 줄 인라인으로 충분. YAGNI.md #2 참고")
public class StockSubscriptionPolicy {

    public static final int MAX_SUBSCRIPTIONS_PER_SESSION = 20;

    public void ensureCanSubscribe(int currentCount) {
        if (currentCount >= MAX_SUBSCRIPTIONS_PER_SESSION) {
            throw new TooManySubscriptionsException(currentCount, MAX_SUBSCRIPTIONS_PER_SESSION);
        }
    }
}
