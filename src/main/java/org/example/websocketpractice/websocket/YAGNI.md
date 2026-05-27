# YAGNI 회고 메모 — PR 1 단일 인스턴스 WebSocket 구독 모델

## Context

PR 1(`단일 인스턴스 WebSocket 구독 모델`)에서 "분산 슬롯 약속" 명목으로 만든
인터페이스/예외/정책 클래스 중 일부가 과해 보인다.
**지금 당장 손대지 않고**, 어떤 클래스가 YAGNI 위반 후보인지 박제해
PR 2/3 시점에 의사결정 자료로 쓴다.

> 이 문서는 **의사결정 보류 상태의 메모**다. 코드 수정은 별도 작업으로 분리.

---

## 평가 기준

- **정당함**: 실제 동시성/안전 버그를 막거나, 명문화된 학습 목표(분산 교체)에 직결.
- **YAGNI 의심**: "혹시 모를 미래"를 위한 추상이고, 지금 구현체가 1개뿐이며,
  추출이 필요해지는 시점에 IDE refactor 한 번이면 끝나는 종류.
- **경계선**: 가치가 약간 있지만 인라인이 더 명료할 수도 있음.

---

## YAGNI 위반 후보 (정리 1순위)

### #1. `PriceFetchLoop` 인터페이스

- **파일**: `src/main/java/org/example/websocketpractice/websocket/broadcast/PriceFetchLoop.java`
- **현재**: 구현체는 `ScheduledPriceFetchLoop` 단 1개. 인터페이스의 유일한 정당화는 "테스트에서 직접 호출".
- **현실**: `ScheduledPriceFetchLoop`를 직접 주입받아도 `tick()`이 public이라 호출 가능.
- **단순화 시**: 인터페이스 삭제, 통합 테스트(`StockWebSocketIntegrationTest`)에서 `ScheduledPriceFetchLoop` 직접 `@Autowired`.
- **잃는 것**: 거의 없음. mock 가능성은 Mockito가 클래스 mock 가능.
- **권장**: **삭제 후보 1순위.**

### #2. `StockSubscriptionPolicy` 클래스

- **파일**: `src/main/java/org/example/websocketpractice/websocket/subscription/StockSubscriptionPolicy.java`
- **현재**: 룰 1개(`count >= 20`)만 검증. 호출자는 `StockWebSocketHandler` 단 1곳.
- **현실**: 핸들러에 `if (count >= 20) { sendErrorEvent(...); return; }` 두 줄로 끝남.
- **단순화 시**: 클래스 삭제, 핸들러 인라인. 상수 `MAX_SUBSCRIPTIONS_PER_SESSION`만 핸들러 안 또는 별도 `Constants`에 둠.
- **잃는 것**: "정책 객체"라는 자리. 단, 정책이 1개 더 늘 때 추출해도 늦지 않음.
- **권장**: **삭제 후보 2순위.** 단, "유저당 N개" 같은 정책이 곧 추가될 거면 유지.

### #3. `TooManySubscriptionsException`

- **파일**: `src/main/java/org/example/websocketpractice/websocket/subscription/TooManySubscriptionsException.java`
- **현재**: 한 메서드(`handleSubscribe`) 안에서 throw → 즉시 catch. **같은 함수 내 throw-catch는 사실상 goto.**
- **단순화 시**: 정책을 boolean 반환(`canSubscribe(count)`)으로 바꾸거나, 정책 자체와 함께 인라인.
- **잃는 것**: 없음.
- **권장**: **#2와 묶어서 정리.**

---

## 경계선 (유지 권장)

### #4. `MalformedCommandException`

- **파일**: `src/main/java/org/example/websocketpractice/websocket/protocol/MalformedCommandException.java`
- **현재**: `ClientCommandParser`에서 throw → `StockWebSocketHandler`에서 catch. **메서드/클래스 경계를 넘는 throw-catch라 #3과 다름.**
- **가치**: 외부 라이브러리(`tools.jackson.core.JacksonException`)를 도메인 예외로 감싸 핸들러가 Jackson을 모르게 함.
- **단순화 시**: 핸들러에서 `JacksonException`을 직접 catch. 핸들러가 Jackson 패키지를 import하게 됨.
- **권장**: **유지.** 라이브러리 누수 차단이라는 1줄짜리 가치가 있다.

### #5. `ServerEvent` 안 에러 코드 상수 5개

- **파일**: `src/main/java/org/example/websocketpractice/websocket/protocol/ServerEvent.java`
- **현재**: `INVALID_SYMBOL`, `PRICE_UNAVAILABLE`, `UNKNOWN_COMMAND`, `MALFORMED`, `TOO_MANY_SUBSCRIPTIONS` 5개가 interface 안 String 상수.
- **대안 A**: enum `ErrorCode`로 추출. 타입 안전 ↑. 단 JSON 직렬화 시 enum name 자동 변환.
- **대안 B**: 호출 측(핸들러/루프)에 인라인 문자열. 단 오타 위험.
- **권장**: **그대로.** 상수 5개는 추적 가능한 양.

### #6. `ClientCommandParser` 분리

- **파일**: `src/main/java/org/example/websocketpractice/websocket/protocol/ClientCommandParser.java`
- **현재**: `ObjectMapper` 래퍼. 양방향 변환(`parse`, `serialize`) + Jackson 예외 → `MalformedCommandException` 변환.
- **단순화 시**: 핸들러/브로드캐스터에 `ObjectMapper` 직접 주입. Parser 클래스 삭제.
- **잃는 것**: 예외 변환 자리 분산, Jackson import가 여러 곳에 퍼짐.
- **권장**: **유지.** 캡슐화 가치 + #4와 한 쌍.

---

## 정당화되는 추상 (참고용 — 그대로 유지)

| 항목 | 이유 |
|---|---|
| `ClientCommand` / `ServerEvent` sealed + record | CLAUDE.md 컨벤션. 패턴 매칭 안전성. 비용 거의 0. |
| `InMemorySubscriptionRegistry` 양방향 인덱스 | 세션 종료 시 cleanup 비용 절감. 실제로 쓰이는 자료구조. |
| `WebSocketSessionRegistry`의 세션별 `ReentrantLock` | Spring 문서가 명시 금지하는 동시 `sendMessage` 버그를 실제로 차단. |
| `SubscriptionRegistry` 인터페이스 | PR 3 Redis 교체 슬롯. 학습 목표에 명문화. (#8 트리거 참고) |
| `PriceBroadcaster` 인터페이스 | 동일 이유. fan-out 분산 슬롯. (#7, #8 참고) |

---

## 메타 이슈 (PR 2 종료 시점 재평가)

### #7. `PriceBroadcaster`의 두 메서드 본질 다름

- **현재**: `broadcastPrice(StockPrice)`와 `sendTo(sessionId, ServerEvent)` 2개 메서드가 한 인터페이스에 묶임.
- **관찰**: 단건 응답(`Subscribed`, `Pong`, `ErrorEvent`)이 모두 `sendTo`로 흐른다.
  분산 구현(Redis Pub/Sub)에서는 `broadcastPrice`는 채널 발행이고 `sendTo`는 인스턴스 로컬 직접 push다. **두 메서드의 본질이 다름.**
- **대안**: PR 3 시점에 `PriceBroadcaster`(fan-out)와 `SessionSender`(단건) 인터페이스로 분리 검토.
- **권장**: 지금은 그대로. PR 3에서 다시 본다.

### #8. 인터페이스 슬롯 vs PR 3 도달 가능성

- **현실**: 학습 프로젝트의 절반 정도는 "다음 PR에서 한다"고 해놓고 안 간다.
- **만약 PR 3 미도달**: `SubscriptionRegistry`, `PriceBroadcaster` 인터페이스는 그냥 비용으로 남음.
- **트리거**: PR 2 끝 시점에 PR 3 착수 여부 재평가. **착수 의지 없으면 인터페이스 2개 폐기 후 구현 클래스만 남기기.**

---

## 정리 시 권장 우선순위 (실행은 별도 결정)

1. **즉시 정리 권장** (잃는 것 없음): #1 `PriceFetchLoop` 삭제, #2 + #3 `StockSubscriptionPolicy` / `TooManySubscriptionsException` 인라인화.
2. **유지 권장**: #4, #5, #6.
3. **PR 2 종료 시점 재평가**: #7, #8 (분산 슬롯 자체의 운명).

---

## Verification (정리 작업이 실제로 진행될 경우)

1. `./gradlew build` — 단위 + 통합 테스트 그린 유지.
2. 통합 테스트(`StockWebSocketIntegrationTest`)에서 `PriceFetchLoop` 주입을 `ScheduledPriceFetchLoop`로 교체 후 동일 동작 확인.
3. `Subscribe` 21회 시 여전히 `ErrorEvent(TOO_MANY_SUBSCRIPTIONS)`가 응답되는지 — 정책 인라인 후 회귀 없음 확인.

---

## 의사결정 보류 — 다음 액션

- **본 PR**: 코드 수정 없음. 이 메모로 박제.
- **PR 2 착수 전**: 이 문서 다시 보고 #1, #2, #3 정리 PR을 따로 만들지 결정.
- **PR 3 착수 시점**: #7, #8 재평가. 인터페이스 슬롯 유지/제거 최종 결정.
