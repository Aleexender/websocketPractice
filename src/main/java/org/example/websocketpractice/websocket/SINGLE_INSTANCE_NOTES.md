# 단일 인스턴스 한정 — 분산 가정이 깨지는 지점들

PR 1은 **의도적으로 단일 인스턴스에서만 올바르게 동작하는 WebSocket 구독 모델**이다.
인터페이스 경계만 분산 친화적으로 잡아두고, 인메모리 구현으로 시작한다.

아래 5개 지점은 인스턴스가 2개 이상으로 늘어나는 순간 즉시 깨진다.
PR 2 이후에서 인터페이스 구현체만 갈아끼우면서 한 지점씩 푼다.

---

## 1. `InMemorySubscriptionRegistry` — 구독 상태가 인스턴스 로컬

**문제**: 클라이언트가 인스턴스 A에 구독하면, 인스턴스 B는 그 구독을 모른다.
B가 같은 심볼을 폴링해도 A의 세션에 push할 수단이 없다.

**해결 방향**: `SubscriptionRegistry`를 Redis Set/Hash 기반 `RedisSubscriptionRegistry`로 교체.
- `symbol → sessionIds` 인덱스를 Redis Set에 보관
- `sessionId → symbols` 역인덱스도 같이 (cleanup 비용 절감)
- 세션 ID는 인스턴스 prefix를 붙여 어느 인스턴스 소속인지 식별 가능하게.

---

## 2. `activeSymbols()` 중복 폴링 — 인스턴스 N개면 KIS rate limit 초과

**문제**: 모든 인스턴스가 자기 `activeSymbols()`를 직접 폴링한다.
인스턴스 N개에 같은 심볼이 동시에 구독돼 있으면, 동일 심볼이 1초당 N번 KIS를 두드린다.
KIS rate limit을 빠르게 초과한다.

**해결 방향**: 폴링 책임을 분산 락 또는 리더 일렉션으로 단일화.
- 옵션 A: Redis 분산 락(`SET NX EX`)으로 매 tick마다 한 인스턴스만 폴링.
- 옵션 B: 심볼별 샤딩 — 심볼 해시로 책임 인스턴스 결정.
- 폴링 결과는 Redis Pub/Sub 채널로 fan-out, 각 인스턴스의 `PriceBroadcaster`가 자기 로컬 세션에만 push.

---

## 3. `LocalPriceBroadcaster.push` — sessionId가 다른 인스턴스 소속일 수 있음

**문제**: `sessionRegistry.find(sessionId)`는 자기 인스턴스 세션만 안다.
분산 환경에서 sessionId는 다른 인스턴스 소속일 수 있고, 그 경우 메시지는 그냥 사라진다.

**해결 방향**:
- `PriceBroadcaster`를 두 계층으로 나눈다.
  1. **Publisher**(어느 인스턴스든 호출 가능) → Redis Pub/Sub 채널에 발행.
  2. **Dispatcher**(인스턴스마다 1개) → 채널을 구독, 자기 로컬 세션에만 push.
- `subscriptionRegistry.sessionsOf(symbol)` 결과 중 자기 인스턴스 prefix가 붙은 것만 처리.

---

## 4. `WebSocketSessionRegistry.lockFor` — 세션별 락이 인스턴스 로컬

**문제**: 세션별 `ReentrantLock`은 같은 JVM 내 동시 `sendMessage`를 직렬화한다.
하지만 sticky session이 깨지거나 세션 마이그레이션이 발생하면 의미가 없다.

**전제**: WebSocket은 본질적으로 stateful이라 보통 sticky하게 라우팅된다 (LB 설정 권장).
이 락은 sticky 가정 하에서만 의미를 가진다. **분산 락으로 확장할 필요는 없다** —
다른 인스턴스가 같은 세션에 동시에 쓰는 상황은 sticky가 깨졌다는 더 큰 문제의 증상이다.

**해결 방향**: PR 2/3에서 sticky session 설정을 인프라 가이드로 명시하고,
session affinity가 깨지는 케이스(롤링 배포 중 세션 종료 → 재연결)를 별도로 다룬다.

---

## 5. `StockSubscriptionPolicy` — 세션당 카운터는 OK, 유저당으로 확장하면 분산 카운터 필요

**현재**: `MAX_SUBSCRIPTIONS_PER_SESSION = 20`. 세션 단위는 인스턴스 로컬이라 분산 무관.

**확장 시 문제**: 인증 도입 후 "유저당 최대 N개" 정책이 추가되면, 한 유저가 여러 인스턴스에
연결된 경우 인스턴스 로컬 카운터로는 불가능.

**해결 방향**: Redis `INCR`/`DECR`로 유저별 카운터 관리. 구독/해제 시 원자 증감.
세션 종료 시 재계산이 어렵기 때문에 cleanup 시 카운터 보정 또는 TTL/하트비트 기반 만료 정책 필요.

---

## 부수 이슈 (PR 2에서 우선 다룰 것)

- **Heartbeat**: 현재 Ping/Pong은 클라이언트 요청 응답형. 서버 측에서 일정 시간 침묵 시 close 미구현.
- **Backpressure**: `session.sendMessage`는 동기. 느린 클라이언트가 폴링 루프 전체를 지연시킬 수 있음 → `ConcurrentWebSocketSessionDecorator` 또는 큐 도입.
- **재연결 시 last-event-id**: 메시지 손실/중복 정책 없음.

---

## 인터페이스 교체 채점표 (PR 3 자체 검증)

`SubscriptionRegistry`, `PriceBroadcaster`, `PriceFetchLoop` 인터페이스 시그니처를 **무수정**한 채
Redis 구현으로 교체하면서 위 1~3번 문제가 풀려야 한다.
인터페이스에 손이 가야 한다면, 그 부분이 분산 친화적이지 못했다는 신호.
