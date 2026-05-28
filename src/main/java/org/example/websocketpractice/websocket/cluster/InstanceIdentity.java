package org.example.websocketpractice.websocket.cluster;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * 이 JVM 인스턴스의 고유 식별자. 분산 환경에서 세션이 어느 인스턴스에 속하는지 구분하는 prefix로 쓰인다.
 * 단일/분산 모드 양쪽에서 항상 활성이며, 단일 모드에서도 prefix가 일관되게 붙어 무해하다.
 */
@Component
public class InstanceIdentity {

    private final String id;

    public InstanceIdentity() {
        this.id = resolve();
    }

    public String id() {
        return id;
    }

    private String resolve() {
        String override = System.getenv("INSTANCE_ID");
        if (override != null && !override.isBlank()) {
            return override;
        }
        return hostname() + "-" + shortUuid();
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    private String shortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
