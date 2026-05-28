package org.example.websocketpractice.websocket.broadcast;

import org.example.websocketpractice.stock.domain.StockPrice;
import org.example.websocketpractice.stock.exception.StockPriceUnavailableException;
import org.example.websocketpractice.stock.query.StockPriceQuery;
import org.example.websocketpractice.websocket.protocol.ServerEvent;
import org.example.websocketpractice.websocket.subscription.SubscriptionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ScheduledPriceFetchLoop implements PriceFetchLoop {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPriceFetchLoop.class);

    private final SubscriptionRegistry subscriptionRegistry;
    private final StockPriceQuery priceQuery;
    private final PriceBroadcaster broadcaster;

    public ScheduledPriceFetchLoop(
            SubscriptionRegistry subscriptionRegistry,
            StockPriceQuery priceQuery,
            PriceBroadcaster broadcaster
    ) {
        this.subscriptionRegistry = subscriptionRegistry;
        this.priceQuery = priceQuery;
        this.broadcaster = broadcaster;
    }

    @Override
    @Scheduled(fixedDelayString = "${stock.ws.poll-interval-ms:1000}")
    public void tick() {
        Set<String> symbols = subscriptionRegistry.activeSymbols();
        if (symbols.isEmpty()) {
            return;
        }
        for (String symbol : symbols) {
            fetchAndBroadcast(symbol);
        }
    }

    private void fetchAndBroadcast(String symbol) {
        try {
            StockPrice price = priceQuery.currentPrice(symbol);
            broadcaster.broadcastPrice(price);
        } catch (StockPriceUnavailableException e) {
            log.warn("Price unavailable for {}: {}", symbol, e.getMessage());
            notifyError(symbol, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Unexpected error while fetching {}: {}", symbol, e.getMessage(), e);
            notifyError(symbol, "Unexpected error");
        }
    }

    private void notifyError(String symbol, String message) {
        Set<String> sessions = subscriptionRegistry.sessionsOf(symbol);
        if (sessions.isEmpty()) {
            return;
        }
        ServerEvent.ErrorEvent event = new ServerEvent.ErrorEvent(
                ServerEvent.PRICE_UNAVAILABLE, message, symbol);
        for (String sessionId : sessions) {
            broadcaster.sendTo(sessionId, event);
        }
    }
}
