# 거래(트랜잭션) 패턴 개발 가이드

> SK Enterprise Framework `framework-transaction` 모듈 기준
> 대상: 엔터프라이즈/금융 도메인에서 정합성이 중요한 거래를 개발하는 모든 개발자
> 최종 업데이트: 2026.08.13

이 문서는 **ACID, 멱등성(Idempotency), 동시성 제어(낙관적/비관적/분산 락), 트랜잭션 아웃박스, SAGA** 등
엔터프라이즈 거래 패턴을 프레임워크에서 어떻게 표준화했고, 실제 코드에서 어떻게 사용하는지를 설명합니다.

---

## 0. 한눈에 보기 — 언제 무엇을 쓰나

| 상황 | 패턴 | 프레임워크 도구 |
|------|------|----------------|
| 단일 DB 트랜잭션의 원자성 보장 | ACID / 트랜잭션 경계 | `@Transactional` (클래스 readOnly + 쓰기 override) |
| 결제/주문 등 "정확히 한 번" 처리, 중복요청 방지 | 멱등성 | `@Idempotent` |
| 동시 수정 충돌(같은 행을 여러 요청이 갱신) | 낙관적 락 + 재시도 | `@Version`(BaseEntity) + `@RetryOnConflict` |
| 재고 차감 등 강한 순서 보장이 필요한 갱신 | 비관적/분산 락 | `@DistributedLock`, `SELECT ... FOR UPDATE` |
| DB 커밋과 메시지 발행의 원자성(이중 쓰기 문제) | 트랜잭션 아웃박스 | `OutboxRecorder` + `OutboxRelay` |
| 여러 서비스에 걸친 분산 트랜잭션 | SAGA(보상 트랜잭션) | `SagaOrchestrator` + `SagaStep` |
| 커밋 성공 후에만 부수효과 실행(알림/이벤트) | 커밋 후 훅 | `TransactionSupport.runAfterCommit` |

모든 도구는 `com.sk.framework.transaction` 패키지에 있으며, `framework-transaction` 의존성만 추가하면
자동 설정(`TransactionAutoConfiguration`)으로 즉시 사용 가능합니다.

```xml
<dependency>
    <groupId>com.sk</groupId>
    <artifactId>framework-transaction</artifactId>
    <version>${project.version}</version>
</dependency>
```

---

## 1. ACID와 트랜잭션 경계

### 1.1 ACID 복습

- **Atomicity(원자성)**: 트랜잭션 내 모든 연산은 전부 성공하거나 전부 실패한다.
- **Consistency(일관성)**: 트랜잭션 전후로 제약조건/불변식이 유지된다.
- **Isolation(격리성)**: 동시에 실행되는 트랜잭션이 서로 간섭하지 않는다.
- **Durability(지속성)**: 커밋된 결과는 장애가 나도 보존된다.

### 1.2 프레임워크 트랜잭션 정책 (필수 규칙)

`BaseService`와 모든 도메인 서비스는 다음 정책을 따릅니다.

```java
@Service
@Transactional(readOnly = true)   // ① 클래스 기본은 읽기 전용
@RequiredArgsConstructor
public class UserService extends BaseService<User, Long> {

    @Transactional                // ② 쓰기 메소드만 override
    public UserDto createUser(UserCreateRequest req) { ... }
}
```

- 클래스 레벨 `readOnly = true`로 두면 조회 성능이 향상되고 실수로 인한 쓰기를 방지합니다.
- 쓰기 메소드는 `@Transactional`로 명시적으로 override 합니다. (메소드명 접두사 기반 AOP 규칙은 폐지)
- 전파(propagation)/격리(isolation) 수준은 요구사항에 따라 명시합니다.

### 1.3 격리 수준(Isolation)과 이상 현상

| 격리 수준 | Dirty Read | Non-repeatable Read | Phantom Read | 비고 |
|-----------|:---:|:---:|:---:|------|
| READ_UNCOMMITTED | O | O | O | 사용 금지 |
| READ_COMMITTED | X | O | O | 대부분 DB 기본값(PostgreSQL) |
| REPEATABLE_READ | X | X | O | MySQL InnoDB 기본값 |
| SERIALIZABLE | X | X | X | 가장 안전, 성능 비용 큼 |

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void settle(Long accountId) { ... }
```

> 실무 원칙: 기본은 READ_COMMITTED로 두고, 정합성이 중요한 특정 구간만 상향합니다.
> 격리 수준만으로 동시성 문제를 다 막으려 하지 말고, **낙관적 락**을 1차 방어선으로 사용하세요.

### 1.4 전파(Propagation) 핵심

| 전파 | 의미 | 대표 사용처 |
|------|------|------------|
| REQUIRED(기본) | 트랜잭션 있으면 참여, 없으면 생성 | 일반 서비스 메소드 |
| REQUIRES_NEW | 항상 새 트랜잭션(기존은 일시중단) | 감사로그/실패해도 별도 커밋 |
| MANDATORY | 반드시 기존 트랜잭션 안에서 호출 | `OutboxRecorder.record` (아웃박스 원자성 강제) |
| NESTED | 세이브포인트 기반 부분 롤백 | 부분 실패 허용 배치 |

---

## 2. 멱등성 (Idempotency) — `@Idempotent`

### 2.1 왜 필요한가

네트워크 재시도, 사용자의 더블클릭, 메시지 재전달(at-least-once) 때문에 **같은 요청이 여러 번** 도착할 수 있습니다.
결제/주문/포인트 적립처럼 부작용이 있는 거래는 "정확히 한 번(exactly-once)"처럼 동작해야 합니다.

### 2.2 사용법

```java
@DistributedLock(key = "'order-product:' + #request.productId")
@Idempotent(key = "'order:' + #request.orderNo")   // 멱등키 = 주문번호
@Transactional
public OrderDto placeOrder(PlaceOrderRequest request) {
    // 이 블록은 동일 orderNo에 대해 딱 한 번만 실행됨
}
```

동작 방식:

1. 멱등키로 저장소를 조회 → **완료 이력이 있으면 최초 결과를 그대로 반환**(재실행 없음)
2. 없으면 `IN_PROGRESS`를 원자적으로 선점(put-if-absent)
3. 비즈니스 수행 후 결과를 `COMPLETED`로 저장(TTL 동안 보관)
4. 진행 중(IN_PROGRESS)인 동일 키 요청은 `409 Conflict`(`IdempotencyConflictException`)로 차단

### 2.3 멱등키 결정 방법 2가지

- **SpEL 키**: `@Idempotent(key = "'order:' + #request.orderNo")` — 비즈니스 식별자를 키로.
- **HTTP 헤더 키**: SpEL을 생략하면 `Idempotency-Key` 헤더 값을 사용.
  샘플의 `IdempotencyKeyFilter`가 헤더를 `IdempotencyKeyHolder`(ThreadLocal)에 담아줍니다.

```bash
curl -X POST /api/orders -H 'Idempotency-Key: 3f9c...' -d '{...}'
```

### 2.4 저장소 교체 (운영)

기본 구현 `InMemoryIdempotencyStore`는 단일 JVM용입니다. 다중 인스턴스에서는
`IdempotencyStore`를 **Redis/DB 구현**으로 교체하세요. (프레임워크는 `@ConditionalOnMissingBean`이라 빈만 등록하면 대체됩니다.)

```java
@Bean
public IdempotencyStore idempotencyStore(RedisTemplate<String,Object> redis) {
    return new RedisIdempotencyStore(redis);
}
```

### 2.5 2중 방어선 원칙

멱등 어드바이스는 1차 방어선입니다. **DB 유니크 제약**(예: `order_no` unique)을 2차 방어선으로 반드시 두세요.
분산 환경에서 저장소 장애가 나도 유니크 제약이 최종 정합성을 지켜줍니다.

---

## 3. 동시성 제어 (Concurrency Control)

### 3.1 낙관적 락 (Optimistic Lock) — `@Version` + `@RetryOnConflict`

충돌이 드물다고 가정하고, 커밋 시점에 버전을 비교하여 충돌을 감지합니다.
`BaseEntity`에 `@Version` 컬럼이 내장되어 모든 엔티티가 자동으로 낙관적 락을 지원합니다.

```java
// BaseEntity
@Version
@Column(name = "version")
private Long version;
```

충돌 시 `OptimisticLockingFailureException`이 발생하며, `@RetryOnConflict`가 지수 백오프로 자동 재시도합니다.

```java
@RetryOnConflict(maxAttempts = 3, backoffMillis = 50, multiplier = 2.0)
@Transactional
public UserDto changeUserStatus(Long id, String status) {
    User user = findById(id);       // 조회 시 version 로딩
    user.changeStatus(...);         // managed 엔티티 변경(dirty checking)
    return convertToDto(user);      // 커밋 시 version 비교 → 충돌 시 재시도
}
```

> ⚠️ 중요: `@RetryOnConflict`는 트랜잭션보다 **바깥**에서 동작해야 각 재시도가 새 트랜잭션이 됩니다.
> 어드바이스 order를 트랜잭션보다 앞(20)에 두어 이를 보장합니다. 재시도 대상 메소드가 self-invocation이면
> 프록시를 타지 않으니, 반드시 **다른 빈의 public 메소드**로 호출하세요.

> ⚠️ 엔티티 수정은 setter 재조립이 아니라 **managed 엔티티의 도메인 메소드**로 하세요.
> 빌더로 재조립하면서 version을 누락하면 낙관적 락이 깨집니다. (샘플 `User.updateProfile` 참고)

### 3.2 비관적 락 (Pessimistic Lock)

충돌이 잦고 재시도 비용이 큰 경우, 조회 시점에 DB 행 잠금을 겁니다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Stock s WHERE s.productId = :pid")
Optional<Stock> findForUpdate(@Param("pid") String productId);
```

`SELECT ... FOR UPDATE`로 매핑되어 트랜잭션 종료까지 다른 트랜잭션의 갱신을 차단합니다.
데드락과 대기시간에 유의하고, 잠금 범위를 최소화하세요.

### 3.3 분산 락 (Distributed Lock) — `@DistributedLock`

다중 인스턴스에서 동일 자원(예: 특정 상품 재고)에 대한 동시 처리를 애플리케이션 레벨에서 직렬화합니다.

```java
@DistributedLock(key = "'stock:' + #productId", waitTime = 5, leaseTime = 10, timeUnit = SECONDS)
public void decreaseStock(String productId, int qty) { ... }
```

- `waitTime`: 락 획득 대기 시간(초과 시 `409` `DistributedLockException`)
- `leaseTime`: 자동 해제 시간(데드락 방지)
- 기본 구현 `InMemoryDistributedLockManager`는 단일 JVM용 → 운영은 **Redis(Redisson)** 구현으로 교체.

```java
@Bean
public DistributedLockManager distributedLockManager(RedissonClient redisson) {
    return new RedissonDistributedLockManager(redisson);
}
```

### 3.4 낙관적 vs 비관적 vs 분산 락 선택 기준

| 기준 | 낙관적 락 | 비관적 락 | 분산 락 |
|------|-----------|-----------|---------|
| 충돌 빈도 | 낮음 | 높음 | 자원 경합 큼 |
| 성능 | 좋음(락 없음) | 대기 발생 | 네트워크 비용 |
| 범위 | 단일 행 | 단일 행/범위 | 임의 키(여러 행/외부자원) |
| 실패 처리 | 재시도 | 대기/타임아웃 | 대기/타임아웃 |

---

## 4. 트랜잭션 아웃박스 (Transactional Outbox)

### 4.1 해결하는 문제 — 이중 쓰기(dual-write)

"DB에 주문 저장" + "메시지 브로커에 이벤트 발행"을 각각 하면, 하나만 성공하고 다른 하나가 실패할 때
데이터 불일치가 발생합니다(분산 트랜잭션 없이 두 시스템을 동시에 커밋할 수 없음).

**해법**: 이벤트를 같은 DB 트랜잭션 안에서 `아웃박스 테이블`에 저장(원자적) → 별도 릴레이가 이를 읽어 발행.

### 4.2 사용법 (3단계)

```java
// 1) 비즈니스 변경 + 이벤트 기록을 같은 @Transactional 안에서
@Transactional
public OrderDto placeOrder(PlaceOrderRequest req) {
    Order saved = orderRepository.save(order);
    outboxRecorder.record("Order", saved.getOrderNo(), "OrderPlaced", toJson(saved)); // MANDATORY 전파
    return OrderDto.from(saved);
}
```

```java
// 2) 릴레이가 주기적으로 PENDING 이벤트를 발행 (프레임워크 제공 - OutboxRelay)
//    @Scheduled(fixedDelay = sk.framework.transaction.outbox.poll-interval-ms)

// 3) 실제 발행은 MessagePublisher 구현으로 위임 (기본: 로그, 운영: Kafka 등)
@Bean
public MessagePublisher messagePublisher(KafkaTemplate<String,String> kafka) {
    return event -> kafka.send(event.getEventType(), event.getPayload());
}
```

### 4.3 전달 보장과 소비자 규칙

- 아웃박스 릴레이는 **at-least-once**(최소 한 번) 전달입니다 → 같은 이벤트가 중복 발행될 수 있습니다.
- 따라서 **소비자는 반드시 멱등 처리**(§2)를 전제로 설계해야 합니다.
- 발행 실패는 `retry_count`를 올리고, 임계치(`max-retry`) 초과 시 `FAILED`로 격리 → 모니터링/수동 개입.

### 4.4 활성화 방법

애플리케이션의 엔티티/리포지토리 스캔 경로에 아웃박스 패키지를 포함하면 릴레이가 자동 활성화됩니다.

```java
@EntityScan(basePackages = {"com.sk.sample", "com.sk.framework.transaction.outbox"})
@EnableJpaRepositories(basePackages = {"com.sk.sample", "com.sk.framework.transaction.outbox"})
```

```yaml
sk:
  framework:
    transaction:
      outbox:
        enabled: true
        poll-interval-ms: 5000
        batch-size: 100
        max-retry: 5
```

---

## 5. SAGA — 분산 트랜잭션(보상 트랜잭션)

### 5.1 개념

여러 서비스(주문·재고·결제)에 걸친 트랜잭션은 2PC 대신 **로컬 트랜잭션의 연쇄 + 실패 시 역순 보상**으로
최종 일관성을 확보합니다.

### 5.2 오케스트레이션 방식 사용법

```java
SagaContext ctx = new SagaContext("order-" + orderNo);
ctx.put("orderNo", orderNo);

new SagaOrchestrator()
    .step(new ReserveStockStep(stockService))    // 실패 시 재고 예약 취소로 보상
    .step(new DebitPaymentStep(paymentService))  // 실패 시 결제 취소로 보상
    .step(new CreateShipmentStep(shipService))
    .execute(ctx);   // 중간 실패 시 성공한 단계들을 역순으로 compensate()
```

```java
public class ReserveStockStep implements SagaStep {
    public String name() { return "ReserveStock"; }
    public void execute(SagaContext ctx) { stockService.reserve(ctx.getRequired("orderNo")); }
    public void compensate(SagaContext ctx) { stockService.release(ctx.getRequired("orderNo")); }
}
```

### 5.3 설계 규칙

- **보상은 멱등**하게 구현(여러 번 호출돼도 안전).
- 보상 자체가 실패하면 심각 상황 → 로그/알림으로 반드시 노출(수동 개입).
- 각 단계는 자체 로컬 트랜잭션으로 수행(단계 간에는 트랜잭션을 공유하지 않음).

---

## 6. 커밋 후 부수효과 — `TransactionSupport.runAfterCommit`

이메일 발송, 이벤트 발행 같은 부수효과는 **커밋이 확정된 뒤에만** 실행해야 합니다.
트랜잭션 중간에 실행하면 롤백 시 "발송했는데 데이터는 없는" 불일치가 생깁니다.

```java
@Transactional
public void confirm(Long id) {
    Reservation r = repo.findById(id).orElseThrow();
    r.confirm();
    transactionSupport.runAfterCommit(() -> notificationClient.send(r.getUserId())); // 커밋 후에만
}
```

프로그래밍 방식 트랜잭션도 지원합니다.

```java
String result = transactionSupport.runInTransaction(() -> { ...; return "ok"; });
```

---

## 7. 안티패턴 체크리스트

- [ ] Controller에서 `@Transactional` 사용 (❌ 서비스 계층에 두세요)
- [ ] 같은 클래스 내부 호출로 `@Transactional`/`@Idempotent`/`@RetryOnConflict` 기대 (❌ self-invocation은 프록시 미적용)
- [ ] 낙관적 락 대상 엔티티를 빌더로 재조립하며 version 누락 (❌ 도메인 메소드로 수정)
- [ ] 아웃박스 없이 "DB 저장 + MQ 발행"을 순차 실행 (❌ 이중 쓰기 위험)
- [ ] 멱등 저장소만 믿고 DB 유니크 제약 생략 (❌ 2중 방어선 필요)
- [ ] `runAfterCommit` 없이 트랜잭션 중간에 외부 알림 발송 (❌ 롤백 시 불일치)
- [ ] 분산 락/멱등 저장소를 InMemory 그대로 운영 배포 (❌ 다중 인스턴스에서 무력화)

---

## 8. 참고 코드 위치

| 항목 | 경로 |
|------|------|
| 트랜잭션 모듈 | `framework-core/framework-transaction/src/main/java/com/sk/framework/transaction/` |
| 멱등성 | `.../idempotency/`, `annotation/Idempotent.java` |
| 재시도 | `.../concurrency/`, `annotation/RetryOnConflict.java` |
| 분산 락 | `.../lock/`, `annotation/DistributedLock.java` |
| 아웃박스 | `.../outbox/` |
| SAGA | `.../saga/` |
| 커밋 후 훅 | `.../support/TransactionSupport.java` |
| 종합 시연(주문) | `framework-samples/basic-crud-sample/.../order/service/OrderService.java` |

---

**📞 문의**: 프레임워크팀 (framework-team@sk.com)
