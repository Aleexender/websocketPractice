package org.example.websocketpractice.websocket.protocol;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientCommandParserTest {

    private final ClientCommandParser parser = new ClientCommandParser(new ObjectMapper());

    @Test
    void parse_returnsSubscribe_whenValidJson() {
        // given
        String payload = "{\"type\":\"Subscribe\",\"symbol\":\"005930\"}";

        // when
        ClientCommand command = parser.parse(payload);

        // then
        assertThat(command).isInstanceOf(ClientCommand.Subscribe.class);
        assertThat(((ClientCommand.Subscribe) command).symbol()).isEqualTo("005930");
    }

    @Test
    void parse_returnsUnsubscribe_whenValidJson() {
        // given
        String payload = "{\"type\":\"Unsubscribe\",\"symbol\":\"005930\"}";

        // when
        ClientCommand command = parser.parse(payload);

        // then
        assertThat(command).isInstanceOf(ClientCommand.Unsubscribe.class);
        assertThat(((ClientCommand.Unsubscribe) command).symbol()).isEqualTo("005930");
    }

    @Test
    void parse_returnsPing_whenTypeIsPing() {
        // given
        String payload = "{\"type\":\"Ping\"}";

        // when
        ClientCommand command = parser.parse(payload);

        // then
        assertThat(command).isInstanceOf(ClientCommand.Ping.class);
    }

    @Test
    void parse_throwsMalformed_whenUnknownType() {
        // given
        String payload = "{\"type\":\"Bogus\",\"symbol\":\"005930\"}";

        // then
        assertThatThrownBy(() -> parser.parse(payload))
                .isInstanceOf(MalformedCommandException.class);
    }

    @Test
    void parse_throwsMalformed_whenInvalidJson() {
        // given
        String payload = "not-json";

        // then
        assertThatThrownBy(() -> parser.parse(payload))
                .isInstanceOf(MalformedCommandException.class);
    }

    @Test
    void serialize_includesTypeField_forServerEvent() {
        // given
        ServerEvent event = new ServerEvent.Priced("005930", 71500L, 1L);

        // when
        String json = parser.serialize(event);

        // then
        assertThat(json).contains("\"type\":\"Priced\"");
        assertThat(json).contains("\"symbol\":\"005930\"");
        assertThat(json).contains("\"priceInWon\":71500");
    }
}
