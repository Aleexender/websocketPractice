package org.example.websocketpractice.websocket.handler;

import org.example.websocketpractice.stock.exception.InvalidStockSymbolException;
import org.example.websocketpractice.stock.query.StockPriceQuery;
import org.example.websocketpractice.websocket.broadcast.PriceBroadcaster;
import org.example.websocketpractice.websocket.cluster.SessionIdCodec;
import org.example.websocketpractice.websocket.protocol.ClientCommand;
import org.example.websocketpractice.websocket.protocol.ClientCommandParser;
import org.example.websocketpractice.websocket.protocol.MalformedCommandException;
import org.example.websocketpractice.websocket.protocol.ServerEvent;
import org.example.websocketpractice.websocket.session.WebSocketSessionRegistry;
import org.example.websocketpractice.websocket.subscription.StockSubscriptionPolicy;
import org.example.websocketpractice.websocket.subscription.SubscriptionRegistry;
import org.example.websocketpractice.websocket.subscription.TooManySubscriptionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class StockWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(StockWebSocketHandler.class);

    private final ClientCommandParser commandParser;
    private final SubscriptionRegistry subscriptionRegistry;
    private final WebSocketSessionRegistry sessionRegistry;
    private final PriceBroadcaster broadcaster;
    private final StockSubscriptionPolicy policy;
    private final StockPriceQuery priceQuery;
    private final SessionIdCodec codec;

    public StockWebSocketHandler(
            ClientCommandParser commandParser,
            SubscriptionRegistry subscriptionRegistry,
            WebSocketSessionRegistry sessionRegistry,
            PriceBroadcaster broadcaster,
            StockSubscriptionPolicy policy,
            StockPriceQuery priceQuery,
            SessionIdCodec codec
    ) {
        this.commandParser = commandParser;
        this.subscriptionRegistry = subscriptionRegistry;
        this.sessionRegistry = sessionRegistry;
        this.broadcaster = broadcaster;
        this.policy = policy;
        this.priceQuery = priceQuery;
        this.codec = codec;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRegistry.register(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = codec.toGlobal(session.getId());
        ClientCommand command;
        try {
            command = commandParser.parse(message.getPayload());
        } catch (MalformedCommandException e) {
            broadcaster.sendTo(sessionId, new ServerEvent.ErrorEvent(
                    ServerEvent.MALFORMED, "Cannot parse command", null));
            return;
        }
        dispatch(sessionId, command);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Transport error on session {}: {}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = codec.toGlobal(session.getId());
        subscriptionRegistry.removeSession(sessionId);
        sessionRegistry.remove(sessionId);
    }

    private void dispatch(String sessionId, ClientCommand command) {
        if (command instanceof ClientCommand.Subscribe sub) {
            handleSubscribe(sessionId, sub.symbol());
            return;
        }
        if (command instanceof ClientCommand.Unsubscribe unsub) {
            handleUnsubscribe(sessionId, unsub.symbol());
            return;
        }
        if (command instanceof ClientCommand.Ping) {
            broadcaster.sendTo(sessionId, new ServerEvent.Pong(System.currentTimeMillis()));
            return;
        }
        broadcaster.sendTo(sessionId, new ServerEvent.ErrorEvent(
                ServerEvent.UNKNOWN_COMMAND, "Unknown command", null));
    }

    private void handleSubscribe(String sessionId, String symbol) {
        try {
            priceQuery.validateSymbol(symbol);
        } catch (InvalidStockSymbolException e) {
            broadcaster.sendTo(sessionId, new ServerEvent.ErrorEvent(
                    ServerEvent.INVALID_SYMBOL, e.getMessage(), symbol));
            return;
        }
        try {
            policy.ensureCanSubscribe(subscriptionRegistry.symbolCountOf(sessionId));
        } catch (TooManySubscriptionsException e) {
            broadcaster.sendTo(sessionId, new ServerEvent.ErrorEvent(
                    ServerEvent.TOO_MANY_SUBSCRIPTIONS, e.getMessage(), symbol));
            return;
        }
        subscriptionRegistry.subscribe(sessionId, symbol);
        broadcaster.sendTo(sessionId, new ServerEvent.Subscribed(symbol));
    }

    private void handleUnsubscribe(String sessionId, String symbol) {
        subscriptionRegistry.unsubscribe(sessionId, symbol);
        broadcaster.sendTo(sessionId, new ServerEvent.Unsubscribed(symbol));
    }
}
