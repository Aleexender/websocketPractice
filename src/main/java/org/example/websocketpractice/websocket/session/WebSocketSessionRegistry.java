package org.example.websocketpractice.websocket.session;

import org.example.websocketpractice.websocket.cluster.SessionIdCodec;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 로컬 WebSocket 세션과 세션별 송신 락을 보관한다. 저장 키는 글로벌 세션 ID({@code {instanceId}:{wsId}})로 통일하며,
 * {@code register}만 세션 객체에서 로컬 id를 꺼내 글로벌로 변환한다. {@code remove/find/lockFor}는 글로벌 ID를 받는다
 * (호출자가 {@link SessionIdCodec#toGlobal}로 변환해 전달). 이 규약 덕에 {@code sessionsOf}가 반환한 글로벌 ID를
 * 변환 없이 그대로 넘겨도 동작하므로 브로드캐스터를 수정할 필요가 없다.
 */
@Component
public class WebSocketSessionRegistry {

    private final SessionIdCodec codec;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Lock> sendLocks = new ConcurrentHashMap<>();

    public WebSocketSessionRegistry(SessionIdCodec codec) {
        this.codec = codec;
    }

    public void register(WebSocketSession session) {
        String globalId = codec.toGlobal(session.getId());
        sessions.put(globalId, session);
        sendLocks.put(globalId, new ReentrantLock());
    }

    public void remove(String globalSessionId) {
        sessions.remove(globalSessionId);
        sendLocks.remove(globalSessionId);
    }

    public Optional<WebSocketSession> find(String globalSessionId) {
        return Optional.ofNullable(sessions.get(globalSessionId));
    }

    public Lock lockFor(String globalSessionId) {
        return sendLocks.computeIfAbsent(globalSessionId, k -> new ReentrantLock());
    }
}
