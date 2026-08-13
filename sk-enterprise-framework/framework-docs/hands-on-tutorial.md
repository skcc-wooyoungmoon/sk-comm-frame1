# 따라하기 튜토리얼 — 거래 패턴으로 기능 하나 만들기

> 목표: 프레임워크의 거래 패턴을 사용해 **"포인트 적립(Point Accrual)"** 기능을
> 도메인 설계부터 API·프론트 확인까지 처음부터 끝까지 직접 만들어 봅니다.
> 소요 시간: 약 40분 · 대상: 프레임워크를 처음 쓰는 개발자
> 최종 업데이트: 2026.08.13

이 튜토리얼은 [환경 셋업 가이드](environment-setup-guide.md)로 빌드가 되는 상태를 전제로 합니다.
포인트 적립은 대표적인 "정확히 한 번" 거래입니다. 중복 적립을 막기 위해 **멱등성**,
잔액 동시 갱신 충돌을 위해 **낙관적 락 + 재시도**, 적립 이벤트 전파를 위해 **아웃박스**를 모두 사용합니다.

---

## 0. 우리가 만들 것

- 엔티티: `PointAccount`(회원별 포인트 잔액, 낙관적 락)
- API:
  - `POST /api/points/accrue` — 포인트 적립(멱등)
  - `GET /api/points/{memberId}` — 잔액 조회
- 패턴: `@Idempotent` + `@RetryOnConflict` + `OutboxRecorder`

작업 위치: `framework-samples/basic-crud-sample/src/main/java/com/sk/sample/point/`

---

## 1단계 — 패키지 만들기

```bash
cd framework-samples/basic-crud-sample/src/main/java/com/sk/sample
mkdir -p point/entity point/repository point/dto point/service point/web
```

도메인 기반 패키지 구조(entity/repository/dto/service/web)는 프레임워크 표준입니다.

---

## 2단계 — 엔티티 (낙관적 락 자동 적용)

`point/entity/PointAccount.java`

```java
package com.sk.sample.point.entity;

import com.sk.framework.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_point_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_point_member", columnNames = "member_id"))
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class PointAccount extends BaseEntity {   // ← BaseEntity가 @Version(낙관적 락)을 이미 포함

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_account_id")
    private Long id;

    @Column(name = "member_id", nullable = false, length = 50)
    private String memberId;

    @Column(name = "balance", nullable = false)
    @Builder.Default
    private long balance = 0L;

    /** 도메인 메소드로만 잔액을 변경 → managed 엔티티 dirty checking으로 낙관적 락 유지 */
    public void accrue(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("적립 포인트는 0보다 커야 합니다.");
        this.balance += amount;
    }
}
```

> 핵심: `BaseEntity`를 상속하면 `version` 컬럼(낙관적 락)과 감사필드(createdAt 등)가 자동 적용됩니다.
> 잔액은 setter가 아니라 `accrue()` 도메인 메소드로만 바꿉니다.

---

## 3단계 — 리포지토리

`point/repository/PointAccountRepository.java`

```java
package com.sk.sample.point.repository;

import com.sk.sample.point.entity.PointAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {
    Optional<PointAccount> findByMemberId(String memberId);
}
```

---

## 4단계 — 요청/응답 DTO

`point/dto/AccrueRequest.java`

```java
package com.sk.sample.point.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccrueRequest {
    @NotBlank private String requestId;   // 멱등키(중복 적립 방지용 고유 요청 식별자)
    @NotBlank private String memberId;
    @Min(1)   private long amount;
}
```

`point/dto/PointBalanceDto.java`

```java
package com.sk.sample.point.dto;

import lombok.*;

@Getter @AllArgsConstructor @Builder
public class PointBalanceDto {
    private String memberId;
    private long balance;
}
```

> `requestId`가 멱등키입니다. 클라이언트는 재시도 시 **같은 requestId**를 보내 중복 적립을 막습니다.

---

## 5단계 — 서비스 (멱등 + 재시도 + 아웃박스)

`point/service/PointService.java`

```java
package com.sk.sample.point.service;

import com.sk.framework.transaction.annotation.Idempotent;
import com.sk.framework.transaction.annotation.RetryOnConflict;
import com.sk.framework.transaction.outbox.OutboxRecorder;
import com.sk.sample.point.dto.*;
import com.sk.sample.point.entity.PointAccount;
import com.sk.sample.point.repository.PointAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)          // ① 클래스 기본은 읽기 전용
@RequiredArgsConstructor
public class PointService {

    private final PointAccountRepository repository;
    private final OutboxRecorder outboxRecorder;

    /**
     * 포인트 적립.
     * - @Idempotent      : 동일 requestId 중복요청은 최초 결과 반환(정확히 한 번)
     * - @RetryOnConflict : 잔액 동시 갱신 충돌 시 자동 재시도
     * - @Transactional   : 잔액 갱신 + 아웃박스 이벤트 기록을 원자적으로 커밋
     */
    @Idempotent(key = "'point-accrue:' + #req.requestId")
    @RetryOnConflict(maxAttempts = 3)
    @Transactional                       // ② 쓰기 메소드만 override
    public PointBalanceDto accrue(AccrueRequest req) {
        PointAccount account = repository.findByMemberId(req.getMemberId())
                .orElseGet(() -> repository.save(
                        PointAccount.builder().memberId(req.getMemberId()).balance(0L).build()));

        account.accrue(req.getAmount());  // ③ 도메인 메소드로 변경 → 커밋 시 version 비교

        // ④ 잔액 변경과 같은 트랜잭션으로 이벤트 기록(이중 쓰기 방지)
        String payload = String.format("{\"memberId\":\"%s\",\"amount\":%d}",
                req.getMemberId(), req.getAmount());
        outboxRecorder.record("Point", req.getMemberId(), "PointAccrued", payload);

        return PointBalanceDto.builder()
                .memberId(account.getMemberId()).balance(account.getBalance()).build();
    }

    public PointBalanceDto getBalance(String memberId) {
        PointAccount account = repository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("포인트 계정이 없습니다: " + memberId));
        return PointBalanceDto.builder()
                .memberId(account.getMemberId()).balance(account.getBalance()).build();
    }
}
```

> 어드바이스 적용 순서: `@Idempotent`(바깥) → `@RetryOnConflict` → `@Transactional`(안쪽).
> 재시도는 트랜잭션 바깥에서 일어나 매번 새 트랜잭션으로 수행되고, 멱등은 그보다 더 바깥에서 중복을 거릅니다.

---

## 6단계 — 컨트롤러

`point/web/PointController.java`

```java
package com.sk.sample.point.web;

import com.sk.framework.common.dto.ApiResponse;
import com.sk.sample.point.dto.*;
import com.sk.sample.point.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @PostMapping("/accrue")
    public ResponseEntity<ApiResponse<PointBalanceDto>> accrue(@Valid @RequestBody AccrueRequest req) {
        return ResponseEntity.ok(ApiResponse.success("적립 완료", pointService.accrue(req)));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<PointBalanceDto>> balance(@PathVariable String memberId) {
        return ResponseEntity.ok(ApiResponse.success(pointService.getBalance(memberId)));
    }
}
```

> Controller에는 try-catch를 두지 않습니다. 예외는 `GlobalExceptionHandler`가 표준 응답으로 변환합니다.
> (`IllegalArgumentException`→400, 멱등/동시성 충돌→409)

---

## 7단계 — 빌드 & 실행

```bash
cd framework-samples/basic-crud-sample
mvn -q -pl . -am install -DskipTests   # 또는 상위에서 mvn clean install
mvn spring-boot:run
```

---

## 8단계 — 동작 검증 (핵심!)

### 8.1 첫 적립

```bash
curl -X POST http://localhost:8080/api/points/accrue \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"REQ-1","memberId":"M100","amount":500}'
# → balance: 500
```

### 8.2 멱등성 검증 — 같은 requestId 재요청

```bash
curl -X POST http://localhost:8080/api/points/accrue \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"REQ-1","memberId":"M100","amount":500}'
# → balance: 500  (❗ 다시 적립되지 않음 = 정확히 한 번)
```

### 8.3 새 requestId로 추가 적립

```bash
curl -X POST http://localhost:8080/api/points/accrue \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"REQ-2","memberId":"M100","amount":300}'
# → balance: 800
```

### 8.4 잔액 조회

```bash
curl http://localhost:8080/api/points/M100
# → {"memberId":"M100","balance":800}
```

### 8.5 아웃박스 이벤트 발행 확인

애플리케이션 로그에서 릴레이가 이벤트를 발행하는지 확인합니다(약 5초 주기).

```
[OUTBOX-PUBLISH] type=PointAccrued, aggregate=Point#M100, payload={"memberId":"M100","amount":500}
```

### 8.6 동시성 검증(선택) — 잔액 동시 적립

같은 회원에 서로 다른 requestId로 여러 요청을 동시에 보내면, 낙관적 락 충돌이 발생해도
`@RetryOnConflict`가 자동 재시도하여 **모든 적립이 유실 없이 반영**됩니다.

```bash
for i in $(seq 1 20); do
  curl -s -X POST http://localhost:8080/api/points/accrue \
    -H 'Content-Type: application/json' \
    -d "{\"requestId\":\"C-$i\",\"memberId\":\"M200\",\"amount\":10}" >/dev/null &
done; wait
curl http://localhost:8080/api/points/M200   # → balance: 200 (10 × 20, 유실 없음)
```

---

## 9단계 — Swagger로 확인

http://localhost:8080/swagger-ui.html 에서 방금 만든 `포인트` API가 자동 문서화되어 있습니다.
`@Operation`/`@Tag`를 붙이면 설명이 더 풍부해집니다(사용자/주문 컨트롤러 참고).

---

## 10단계 — 프론트엔드에서 확인(선택)

`framework-frontend/user-admin`을 실행하면 사용자/권한 관리 화면을 볼 수 있습니다.
포인트 화면을 추가하려면 `src/api/`에 API 함수, `src/pages/`에 페이지를 추가하고 라우터에 등록하세요.
(사용자 관리 화면의 `usersApi.ts`, `UserListPage.tsx`가 좋은 템플릿입니다.)

---

## 정리 — 배운 것

| 요구사항 | 사용한 패턴 | 어노테이션/도구 |
|----------|-------------|-----------------|
| 중복 적립 방지 | 멱등성 | `@Idempotent(key=requestId)` |
| 동시 잔액 갱신 유실 방지 | 낙관적 락 + 재시도 | `@Version`(내장) + `@RetryOnConflict` |
| 적립 이벤트 원자적 발행 | 트랜잭션 아웃박스 | `OutboxRecorder` + `OutboxRelay` |
| 예외의 표준 응답화 | 전역 예외 처리 | `GlobalExceptionHandler` |
| 트랜잭션 경계 | ACID | `@Transactional`(readOnly 기본 + 쓰기 override) |

### 다음에 해볼 것
- 포인트 **차감(사용)** 을 추가하고, 잔액 부족 시 비즈니스 예외를 던져 보세요.
- `MessagePublisher`를 Kafka 구현으로 교체해 실제 이벤트를 발행해 보세요.
- 적립+차감+주문을 묶어 `SagaOrchestrator`로 보상 트랜잭션을 구성해 보세요.

---

**📞 문의**: 프레임워크팀 (framework-team@sk.com)
