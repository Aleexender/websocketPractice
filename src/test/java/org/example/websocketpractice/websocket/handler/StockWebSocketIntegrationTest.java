package org.example.websocketpractice.websocket.handler;

import org.example.websocketpractice.stock.client.StockPriceClient;
import org.example.websocketpractice.stock.domain.StockPrice;
import org.example.websocketpractice.websocket.broadcast.PriceFetchLoop;
import org.example.websocketpractice.websocket.subscription.SubscriptionRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "stock.ws.poll-interval-ms=600000",
        "kis.app-key=test-key",
        "kis.app-secret=test-secret"
})
class StockWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PriceFetchLoop priceFetchLoop;

    @Autowired
    private SubscriptionRegistry subscriptionRegistry;

    @MockitoBean
    private StockPriceClient stockPriceClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebSocketSession session;

    @AfterEach
    void closeSession() throws Exception {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    @Test
    void subscribe_receivesPricedEvent_afterTick() throws Exception {
        // given
        when(stockPriceClient.fetchCurrentPrice("005930"))
                .thenReturn(new StockPrice("005930", 71500L, Instant.ofEpochMilli(1234L)));
        CollectingHandler handler = new CollectingHandler();
        session = connect(handler);

        // when
        session.sendMessage(new TextMessage(toJson(Map.of("type", "Subscribe", "symbol", "005930"))));
        Map<String, Object> ack = handler.takeAs(objectMapper);
        priceFetchLoop.tick();
        Map<String, Object> priced = handler.takeAs(objectMapper);

        // then
        assertThat(ack.get("type")).isEqualTo("Subscribed");
        assertThat(ack.get("symbol")).isEqualTo("005930");
        assertThat(priced.get("type")).isEqualTo("Priced");
        assertThat(priced.get("symbol")).isEqualTo("005930");
        assertThat(((Number) priced.get("priceInWon")).longValue()).isEqualTo(71500L);
    }

    @Test
    void invalidSymbol_returnsErrorEvent_doesNotClose() throws Exception {
        // given
        CollectingHandler handler = new CollectingHandler();
        session = connect(handler);

        // when
        session.sendMessage(new TextMessage(toJson(Map.of("type", "Subscribe", "symbol", "ABC"))));
        Map<String, Object> error = handler.takeAs(objectMapper);

        // then
        assertThat(error.get("type")).isEqualTo("ErrorEvent");
        assertThat(error.get("code")).isEqualTo("INVALID_SYMBOL");
        assertThat(session.isOpen()).isTrue();
    }

    @Test
    void disconnect_cleansSubscription_whenSessionClosed() throws Exception {
        // given
        CollectingHandler handler = new CollectingHandler();
        session = connect(handler);
        session.sendMessage(new TextMessage(toJson(Map.of("type", "Subscribe", "symbol", "005930"))));
        handler.takeAs(objectMapper);
        assertThat(subscriptionRegistry.activeSymbols()).containsExactly("005930");

        // when
        session.close(CloseStatus.NORMAL);

        // then
        waitUntil(() -> subscriptionRegistry.activeSymbols().isEmpty(), 2000);
        assertThat(subscriptionRegistry.activeSymbols()).isEmpty();
    }

    private WebSocketSession connect(WebSocketHandler handler) throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        return client.execute(handler, null, URI.create("ws://localhost:" + port + "/ws/stocks"))
                .get(3, TimeUnit.SECONDS);
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void waitUntil(java.util.function.BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
    }

    private static class CollectingHandler extends AbstractWebSocketHandler {

        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

        @Override
        protected void handleTextMessage(WebSocketSession s, TextMessage message) {
            messages.add(message.getPayload());
        }

        Map<String, Object> takeAs(ObjectMapper mapper) throws Exception {
            String payload = messages.poll(3, TimeUnit.SECONDS);
            if (payload == null) {
                throw new AssertionError("Timed out waiting for message");
            }
            return mapper.readValue(payload, new tools.jackson.core.type.TypeReference<>() {
            });
        }
    }
}
