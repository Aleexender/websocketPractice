# websocketPractice

웹소켓을 활용한 주식 앱 학습 프로젝트. 분산 서버 환경에서 웹소켓을 다루는 방법과,
유저수 급증 등 운영 환경에서 발생할 수 있는 이슈를 백엔드 관점에서 학습하는 것이 목적이다.

## Learning Goals

1. **분산 서버 환경에서의 웹소켓 운용**
   - 단일 서버가 아닌 다중 서버를 가정한 설계 (실제 다중 인스턴스 구동은 선택, 가정만 해도 무방)
   - 세션 분산, 메시지 브로커(예: Redis Pub/Sub, RabbitMQ 등)를 통한 인스턴스 간 통신
   - 특정 인스턴스에 연결된 클라이언트에게 다른 인스턴스에서 발생한 이벤트를 전달하는 구조 설계

2. **웹소켓 기반 주식 기능 구현**
   - 주식 단건 실시간 조회 (구독/해제)
   - 사용자가 지정한 가격에 도달하면 알림 전송 (가격 알림)
   - 매수/매도는 구현하지 않음 (웹소켓 학습 주제와 무관하다고 판단)

3. **운영 이슈를 고려한 설계**
   - 유저수 급증 시 발생할 수 있는 케이스 (커넥션 폭증, 브로드캐스트 비용, 백프레셔)
   - 서버 재시작/스케일 인·아웃 시 세션 유실 대응
   - 좀비 커넥션, heartbeat/ping-pong, 재연결 전략
   - 메시지 유실/중복 처리 정책

4. **DB 다중화 및 데이터 계층 분산**
   - 단일 DB 인스턴스를 SPOF로 보고, 읽기/쓰기 분리(Primary-Replica) 가정의 설계 (마스터 슬레이브 구조.)
   - 복제 지연(replication lag) 하에서 "read-your-own-writes" 같은 요구사항을 어떻게 만족시킬지
   - 장애 조치(failover) 시 커넥션 풀/트랜잭션/재시도 전략
   - 복제 지연(replication lag)도 고려한 데이터 일관성 모델 설계
   - 샤딩이 필요해지는 시점과 샤드 키 선정 기준 (학습용으로 시뮬레이션 수준)
   - DB 다중화와 캐시(Redis)·메시지 브로커가 결합될 때의 정합성 정책

## Focus & Scope

- **백엔드가 학습 대상**: 사용자는 백엔드 개발자이며, 백엔드 코드는 꼼꼼하게 검토/구현한다.
- **프론트엔드는 바이브 코딩**: 동작 검증 수준으로 간단히 만든다. 백엔드 검토만큼의 엄격함을 적용하지 않는다.
- 답변/리뷰의 무게중심은 항상 백엔드 (설계, 동시성, 장애 시나리오, 메시지 흐름).

## Tech Stack

- Spring Boot 4.0.6 / Java 17
- spring-boot-starter-webmvc, spring-boot-starter-jdbc
- MySQL (mysql-connector-j)
- Lombok
- JUnit 5

> 분산 환경 학습 진행에 따라 추가될 가능성이 있는 후보 (필요 시점에 도입):
> - `spring-boot-starter-websocket`
> - Redis (Pub/Sub, 세션/구독 상태 공유)
> - 메시지 브로커 (RabbitMQ/Kafka) - STOMP 외부 브로커 연동 등
> - MySQL Replication (Primary/Replica) 또는 ProxySQL/MaxScale 류 — 읽기/쓰기 분리 학습 시

## Architecture Principles

### 핵심 원칙
- OOP / SOLID 준수. 클래스간 의존성은 외부 주입 (생성자 주입).
- 인터페이스는 **필요한 부분만 노출**. (예: `StockReader`, `StockPublisher` 분리)
- 디미터 법칙 준수, Low Coupling / High Cohesion.
- Setter 금지. 엔티티는 작게 유지.
- `else` 사용 지양 (early return / guard clause 선호).
- 객체를 알아야 할 책임이 있는 객체에서만 해당 객체를 import.

### CQS 적용
- 읽기: `XxxQuery` (조회 전용, 부작용 없음, `@Transactional(readOnly = true)`)
- 쓰기: `XxxService` (상태 변경/트랜잭션, `@Transactional`)
- Controller는 Query 또는 Service만 의존. 한 컴포넌트에서 조회와 변경을 섞지 않는다.

### 웹소켓 레이어 가이드 (작업 진행에 따라 점진 보강)
- 세션 관리 책임과 구독 상태 관리 책임을 분리한다.
- 인스턴스 로컬 메모리에만 의존하는 구현은 **분산 가정 위반**으로 간주. 공유 저장소(Redis 등)나 브로커를 거치는 설계를 선호.
- 메시지 발행자(Publisher)와 세션에 푸시하는 디스패처(Dispatcher)를 분리해 단일 인스턴스 한정 로직과 클러스터 전체 로직의 경계를 명확히 한다.
- 가격 알림처럼 "상태 + 조건"이 결합된 기능은 도메인 객체(예: `PriceAlert`)로 모델링하고 조건 평가 책임을 도메인에 둔다.

### 데이터 계층 가이드 (DB 다중화 가정)
- **읽기/쓰기 라우팅**: `XxxQuery`는 Replica를 기본 가정, `XxxService`는 Primary를 가정한다.
  Spring `AbstractRoutingDataSource` + `@Transactional(readOnly = true)`로 라우팅하는 패턴을 우선 검토.
- **복제 지연 인지**: 쓰기 직후 즉시 같은 데이터를 읽어야 하는 플로우(예: 가격 알림 등록 후 즉시 평가 대상에 포함)에서는
  Replica를 거치지 않거나, write-through 캐시/세션 sticky 같은 보완책을 함께 설계.
- **트랜잭션 경계**: 분산 트랜잭션(2PC)은 피한다. 여러 저장소(DB + Redis + Broker)에 걸친 일관성은
  Outbox / Idempotent consumer / Saga 같은 패턴으로 해소.
- **커넥션 풀**: 인스턴스 수 × 풀 크기로 DB 커넥션이 결정된다는 점을 항상 의식. 스케일 아웃 시 DB 측 `max_connections`와의
  관계를 함께 계산.
- **장애 조치**: Primary 장애 시 진행 중 트랜잭션은 실패한다는 전제. 재시도 가능한 멱등 연산과 불가능한 연산을 코드 레벨에서 구분.
- **샤딩은 마지막 카드**: 먼저 인덱스/쿼리/캐시/Replica로 풀고, 그래도 안 되면 샤딩. 샤드 키는 도메인의 자연 키
  (예: 사용자 ID, 종목 코드)에서 후보를 찾는다.
- **스키마 변경**: 무중단 운영 가정 — `ALTER TABLE` 풀 락을 피하는 변경 절차(online DDL, 듀얼 라이트 후 컷오버 등)를 의식.
- **웹소켓과의 결합점**: 가격 알림 조건은 DB(쓰기 빈도 낮음) + 인메모리/Redis(평가 빈도 높음)로 이원화 가능.
  쓰기 시 캐시 무효화/갱신 경로를 명시적으로 설계.

## Conventions

### DTO
- Java `sealed interface` + `record` 구조 사용.

```java
public sealed interface StockEvent permits StockEvent.Priced, StockEvent.AlertTriggered {
  record Priced(
    String symbol,
    long priceInWon,
    long timestamp
  ) implements StockEvent {}

  record AlertTriggered(
    long alertId,
    String symbol,
    long priceInWon
  ) implements StockEvent {}
}
```

### Entity
- `@Column`을 반드시 명시.
- 필드 순서: bit/boolean → String/Number → Enum → Embedded → 단수 연관관계 → 복수 연관관계.
- 연관관계는 가급적 id 필드로 느슨한 결합.
- `new java.util.ArrayList<>()`보다 `new ArrayList<>()` 선호 (필요 import만).

### Exception
- 검증은 `Precondition` 류 유틸이 도입되면 일관 사용. 도입 전에는 의미 있는 도메인 예외를 직접 정의하고 일반 `RuntimeException` 남발을 피한다.

### Test
- JUnit 5. 테스트 클래스 `*Test`, 메서드명은 `대상_결과_원인` 패턴.
- 웹소켓/분산 시나리오는 단위 테스트만으로 부족하므로, 동시성/멀티 인스턴스 가정의 통합 테스트도 점진 도입.

## Build & Run

- Build: `./gradlew build`
- Test: `./gradlew test`
- Run: `./gradlew bootRun`

## Working Style Notes (for Claude)

- 답변은 한국어.
- 백엔드 코드 작성/리뷰 시: 설계 트레이드오프, 분산 환경에서의 실패 시나리오, 동시성, 메모리/커넥션 비용까지 함께 짚는다.
- 프론트 코드 작성/수정 시: 동작에 필요한 최소한으로 빠르게. 컨벤션 검토에 시간을 많이 쓰지 않는다.
- "분산 가정"이라는 제약을 잊지 말 것. 단일 인스턴스에서만 동작하는 솔루션을 제안할 때는 **반드시** 분산 환경에서의 한계와 대안을 함께 설명한다.
- "분산 가정"은 웹소켓뿐 아니라 **DB에도 동일하게 적용**. 단일 DB 가정의 솔루션을 제안할 때는 읽기/쓰기 분리·복제 지연·장애 조치
  관점에서의 한계를 반드시 함께 언급한다.
- 추측으로 라이브러리/의존성을 추가하지 않는다. 도입이 필요하면 이유와 대안을 먼저 제시하고 확인 후 진행.
