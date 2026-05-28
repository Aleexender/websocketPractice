package org.example.websocketpractice.websocket.session;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class WebSocketSessionRegistry {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Lock> sendLocks = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        sessions.put(session.getId(), session);
        sendLocks.put(session.getId(), new ReentrantLock());
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
        sendLocks.remove(sessionId);
    }

    public Optional<WebSocketSession> find(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public Lock lockFor(String sessionId) {
        return sendLocks.computeIfAbsent(sessionId, k -> new ReentrantLock());
    }
}
