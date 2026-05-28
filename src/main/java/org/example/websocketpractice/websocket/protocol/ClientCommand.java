package org.example.websocketpractice.websocket.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClientCommand.Subscribe.class, name = "Subscribe"),
        @JsonSubTypes.Type(value = ClientCommand.Unsubscribe.class, name = "Unsubscribe"),
        @JsonSubTypes.Type(value = ClientCommand.Ping.class, name = "Ping")
})
public sealed interface ClientCommand
        permits ClientCommand.Subscribe, ClientCommand.Unsubscribe, ClientCommand.Ping {

    record Subscribe(String symbol) implements ClientCommand {
    }

    record Unsubscribe(String symbol) implements ClientCommand {
    }

    record Ping() implements ClientCommand {
    }
}
