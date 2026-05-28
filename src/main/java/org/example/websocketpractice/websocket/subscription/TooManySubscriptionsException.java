package org.example.websocketpractice.websocket.subscription;

@Deprecated(since = "YAGNI 위반: 같은 메서드(handleSubscribe) 내 throw → 즉시 catch. throw-catch가 사실상 goto. YAGNI.md #3 참고")
public class TooManySubscriptionsException extends RuntimeException {

    public TooManySubscriptionsException(int current, int max) {
        super("Subscription limit reached: current=" + current + ", max=" + max);
    }
}
