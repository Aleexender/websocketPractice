package org.example.websocketpractice.websocket.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ServerEvent.Priced.class, name = "Priced"),
        @JsonSubTypes.Type(value = ServerEvent.Subscribed.class, name = "Subscribed"),
        @JsonSubTypes.Type(value = ServerEvent.Unsubscribed.class, name = "Unsubscribed"),
        @JsonSubTypes.Type(value = ServerEvent.ErrorEvent.class, name = "ErrorEvent"),
        @JsonSubTypes.Type(value = ServerEvent.Pong.class, name = "Pong")
})
public sealed interface ServerEvent
        permits ServerEvent.Priced,
                ServerEvent.Subscribed,
                ServerEvent.Unsubscribed,
                ServerEvent.ErrorEvent,
                ServerEvent.Pong {

    String INVALID_SYMBOL = "INVALID_SYMBOL";
    String PRICE_UNAVAILABLE = "PRICE_UNAVAILABLE";
    String UNKNOWN_COMMAND = "UNKNOWN_COMMAND";
    String MALFORMED = "MALFORMED";
    String TOO_MANY_SUBSCRIPTIONS = "TOO_MANY_SUBSCRIPTIONS";

    record Priced(String symbol, long priceInWon, long timestamp) implements ServerEvent {
    }

    record Subscribed(String symbol) implements ServerEvent {
    }

    record Unsubscribed(String symbol) implements ServerEvent {
    }

    record ErrorEvent(String code, String message, String symbol) implements ServerEvent {
    }

    record Pong(long timestamp) implements ServerEvent {
    }
}
