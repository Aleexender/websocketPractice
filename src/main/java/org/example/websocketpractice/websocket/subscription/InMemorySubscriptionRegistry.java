package org.example.websocketpractice.websocket.subscription;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "stock.ws.mode", havingValue = "single", matchIfMissing = true)
public class InMemorySubscriptionRegistry implements SubscriptionRegistry {

    private final ConcurrentHashMap<String, Set<String>> symbolToSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> sessionToSymbols = new ConcurrentHashMap<>();

    @Override
    public void subscribe(String sessionId, String symbol) {
        symbolToSessions.compute(symbol, (key, sessions) -> {
            Set<String> next = (sessions == null) ? ConcurrentHashMap.newKeySet() : sessions;
            next.add(sessionId);
            return next;
        });
        sessionToSymbols.compute(sessionId, (key, symbols) -> {
            Set<String> next = (symbols == null) ? ConcurrentHashMap.newKeySet() : symbols;
            next.add(symbol);
            return next;
        });
    }

    @Override
    public void unsubscribe(String sessionId, String symbol) {
        symbolToSessions.computeIfPresent(symbol, (key, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
        sessionToSymbols.computeIfPresent(sessionId, (key, symbols) -> {
            symbols.remove(symbol);
            return symbols.isEmpty() ? null : symbols;
        });
    }

    @Override
    public void removeSession(String sessionId) {
        Set<String> symbols = sessionToSymbols.remove(sessionId);
        if (symbols == null) {
            return;
        }
        for (String symbol : symbols) {
            symbolToSessions.computeIfPresent(symbol, (key, sessions) -> {
                sessions.remove(sessionId);
                return sessions.isEmpty() ? null : sessions;
            });
        }
    }

    @Override
    public Set<String> sessionsOf(String symbol) {
        Set<String> sessions = symbolToSessions.get(symbol);
        return (sessions == null) ? Set.of() : Set.copyOf(sessions);
    }

    @Override
    public Set<String> activeSymbols() {
        return Set.copyOf(symbolToSessions.keySet());
    }

    @Override
    public int symbolCountOf(String sessionId) {
        Set<String> symbols = sessionToSymbols.get(sessionId);
        return (symbols == null) ? 0 : symbols.size();
    }
}
