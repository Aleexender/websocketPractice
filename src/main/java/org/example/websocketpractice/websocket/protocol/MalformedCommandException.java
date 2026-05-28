package org.example.websocketpractice.websocket.protocol;

public class MalformedCommandException extends RuntimeException {

    public MalformedCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
