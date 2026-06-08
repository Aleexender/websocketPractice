package org.example.websocketpractice.websocket.subscription;

import org.springframework.stereotype.Component;

@Component
public class StockSubscriptionPolicy {

    public static final int MAX_SUBSCRIPTIONS_PER_SESSION = 20;

    public void ensureCanSubscribe(int currentCount) {
        if (currentCount >= MAX_SUBSCRIPTIONS_PER_SESSION) {
            throw new TooManySubscriptionsException(currentCount, MAX_SUBSCRIPTIONS_PER_SESSION);
        }
    }
}
