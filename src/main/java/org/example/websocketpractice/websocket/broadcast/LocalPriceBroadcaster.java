package org.example.websocketpractice.websocket.broadcast;

import org.example.websocketpractice.stock.domain.StockPrice;
import org.example.websocketpractice.websocket.protocol.ClientCommandParser;
import org.example.websocketpractice.websocket.protocol.ServerEvent;
import org.example.websocketpractice.websocket.session.WebSocketSessionRegistry;
import org.example.websocketpractice.websocket.subscription.SubscriptionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;

@Component
public class LocalPriceBroadcaster implements PriceBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(LocalPriceBroadcaster.class);

    private final SubscriptionRegistry subscriptionRegistry;
    private final WebSocketSessionRegistry sessionRegistry;
    private final ClientCommandParser commandParser;

    public LocalPriceBroadcaster(
            SubscriptionRegistry subscriptionRegistry,
            WebSocketSessionRegistry sessionRegistry,
            ClientCommandParser commandParser
    ) {
        this.subscriptionRegistry = subscriptionRegistry;
        this.sessionRegistry = sessionRegistry;
        this.commandParser = commandParser;
    }

    @Override
    public void broadcastPrice(StockPrice price) {
        Set<String> sessionIds = subscriptionRegistry.sessionsOf(price.symbol());
        if (sessionIds.isEmpty()) {
            return;
        }
        ServerEvent.Priced event = new ServerEvent.Priced(
                price.symbol(),
                price.priceInWon(),
                price.fetchedAt().toEpochMilli()
        );
        String payload = commandParser.serialize(event);
        for (String sessionId : sessionIds) {
            push(sessionId, payload);
        }
    }

    @Override
    public void sendTo(String sessionId, ServerEvent event) {
        push(sessionId, commandParser.serialize(event));
    }

    private void push(String sessionId, String payload) {
        Optional<WebSocketSession> maybeSession = sessionRegistry.find(sessionId);
        if (maybeSession.isEmpty()) {
            return;
        }
        WebSocketSession session = maybeSession.get();
        if (!session.isOpen()) {
            return;
        }
        Lock lock = sessionRegistry.lockFor(sessionId);
        lock.lock();
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (IOException e) {
            log.warn("Failed to send message to session {}: {}", sessionId, e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
