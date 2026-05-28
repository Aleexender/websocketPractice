package org.example.websocketpractice.websocket.protocol;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ClientCommandParser {

    private final ObjectMapper objectMapper;

    public ClientCommandParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ClientCommand parse(String payload) {
        try {
            return objectMapper.readValue(payload, ClientCommand.class);
        } catch (JacksonException e) {
            throw new MalformedCommandException("Failed to parse client command", e);
        }
    }

    public String serialize(ServerEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize server event", e);
        }
    }
}
