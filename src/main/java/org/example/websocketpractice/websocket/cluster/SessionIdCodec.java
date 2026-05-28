package org.example.websocketpractice.websocket.cluster;

import org.springframework.stereotype.Component;

/**
 * 로컬 WebSocket 세션 ID와 글로벌 세션 ID({@code {instanceId}:{wsSessionId}}) 사이의 변환을 전담하는 유일한 지점.
 * 글로벌 ID를 opaque String으로 취급하면 {@code SubscriptionRegistry}/{@code PriceBroadcaster}의 시그니처를
 * 무수정으로 유지하면서 분산 식별이 가능하다. wsSessionId(UUID)에는 구분자 {@code :}가 없으므로 충돌하지 않는다.
 */
@Component
public class SessionIdCodec {

    private static final String DELIMITER = ":";

    private final InstanceIdentity instanceIdentity;

    public SessionIdCodec(InstanceIdentity instanceIdentity) {
        this.instanceIdentity = instanceIdentity;
    }

    public String toGlobal(String localWsSessionId) {
        return instanceIdentity.id() + DELIMITER + localWsSessionId;
    }

    public String localPart(String globalSessionId) {
        int idx = globalSessionId.lastIndexOf(DELIMITER);
        if (idx < 0) {
            return globalSessionId;
        }
        return globalSessionId.substring(idx + 1);
    }

    public boolean isLocal(String globalSessionId) {
        return globalSessionId.startsWith(instanceIdentity.id() + DELIMITER);
    }
}
