package org.example.websocketpractice.websocket.subscription;

public class TooManySubscriptionsException extends RuntimeException {

    public TooManySubscriptionsException(int current, int max) {
        super("Subscription limit reached: current=" + current + ", max=" + max);
    }
}
