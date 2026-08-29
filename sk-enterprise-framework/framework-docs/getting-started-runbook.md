# 실행 런북 (Getting Started Runbook)

> 내 PC에서 처음부터 실행하는 순서. 로컬(H2) 빠른 실행 → 풀스택(PostgreSQL+Redis+Kafka) Docker 실행.
> 최종 업데이트: 2026.08.14

## 0. 사전 준비 (버전 확인)
```bash
java -version    # 25 필수 (24 이하면 빌드 안 됨)
mvn -version     # 3.9+
node -v          # 20+ (프론트)
docker version   # (Docker 실행 시)
```
JDK 25 설치: macOS `brew install openjdk@25` · Windows [Temurin 25](https://adoptium.net) · Ubuntu `sudo apt install openjdk-25-jdk`
설치 후 `JAVA_HOME`을 25로 지정하고 `java -version`이 `25`인지 확인.

---

## 1. 로컬 빠른 실행 (H2, 인프라 불필요)
```bash
cd sk-enterprise-framework/framework-samples/basic-crud-sample
mvn spring-boot:run
```
- Swagger: http://localhost:8080/swagger-ui.html
- 로그인: `admin@sk.com` / `password` (그 외 `manager@sk.com`, `user1@sk.com`)
- 이 모드는 거래 패턴이 **기본값(InMemory 멱등/락, 로그 아웃박스)**으로 동작 → Redis/Kafka 불필요.

프론트엔드(다른 터미널, 백엔드 켠 채로):
```bash
cd sk-enterprise-framework/framework-frontend/user-admin
npm install && npm run dev      # http://localhost:5173
```

---

## 2. 풀스택 Docker 실행 (PostgreSQL + Redis + Kafka + 앱 + 프론트) ⭐

> Docker Desktop을 먼저 실행하세요. 아래 한 줄이면 5개 컨테이너가 뜨고,
> 거래 패턴이 **Redis(멱등/분산락) + Kafka(아웃박스)**로 전환되어 동작합니다.

```bash
cd sk-enterprise-framework
docker compose up --build
```

기동되는 서비스:
| 서비스 | 포트 | 역할 |
|--------|------|------|
| postgres | 5432 | 운영 DB(Flyway 스키마 적용) |
| redis | 6379 | 멱등 저장소 + 분산 락 |
| kafka | 9092 | 아웃박스 이벤트 발행(KRaft, Zookeeper 불필요) |
| app | 8080 | 백엔드(dev 프로파일) |
| frontend | 5173 | 관리자 콘솔(Nginx) |

접속:
- 앱: http://localhost:8080/swagger-ui.html
- 프론트: http://localhost:5173 (`admin@sk.com` / `password`)

### 동작 확인 (Redis/Kafka가 실제로 쓰이는지)
```bash
# 로그인 → 토큰
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@sk.com","password":"password"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')

# 멱등 주문 2회(같은 orderNo) → 같은 id 반환(= Redis 멱등 저장소 동작)
curl -s -X POST http://localhost:8080/api/orders -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"orderNo":"D-1","customerId":"C","productId":"P","quantity":1,"amount":100}'
curl -s -X POST http://localhost:8080/api/orders -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"orderNo":"D-1","customerId":"C","productId":"P","quantity":1,"amount":100}'
# → 두 응답의 "id"가 동일하면 멱등 정상
```

```bash
# 아웃박스 → Kafka 발행 확인: 앱 로그에 [KAFKA-PUBLISH] 라인
docker compose logs app | grep KAFKA-PUBLISH

# Kafka 토픽 생성 확인(이벤트 타입별 토픽 sk.outbox.OrderPlaced 등)
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

# Redis 키 확인(멱등: idem:*, 락: lock:*)
docker compose exec redis redis-cli KEYS 'idem:*'
```

```bash
# Redis 락 소비(선택): 상품 구매(분산 락 경유) — 재고 차감
curl -s -X POST http://localhost:8080/api/products -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"name":"굿즈","price":1000,"stock":5}'
curl -s -X POST http://localhost:8080/api/products/1/purchase -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"quantity":2}'
```

### 종료 / 초기화
```bash
docker compose down        # 컨테이너 종료(DB 데이터 유지)
docker compose down -v     # 볼륨까지 삭제(DB 초기화)
```

---

## 3. 무엇이 어떻게 전환되나 (원리)
- 기본값(프로퍼티 미설정): `InMemory` 멱등/락 + `Logging` 아웃박스 → 인프라 없이 동작.
- Docker compose는 `app` 컨테이너에 아래 환경변수를 주입해 운영 구현으로 전환:
  ```
  SK_FRAMEWORK_TRANSACTION_IDEMPOTENCY_STORE=redis
  SK_FRAMEWORK_TRANSACTION_LOCK_TYPE=redis
  SK_FRAMEWORK_TRANSACTION_OUTBOX_PUBLISHER=kafka
  SPRING_DATA_REDIS_HOST=redis
  SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
  ```
- 코드는 `@ConditionalOnClass + 프로퍼티`로 구현을 선택합니다.(가이드 `transaction-pattern-guide.md §7-1`)

---

## 4. 자주 겪는 문제
| 증상 | 해결 |
|------|------|
| `release version 25 not supported` | JDK 25 미활성. `java -version`/`JAVA_HOME` 확인 |
| Lombok `cannot find symbol builder()` | JDK 24 이하 사용 중 → 25로 교체 |
| `docker compose` 에러 | Docker Desktop 실행 여부 확인 |
| kafka 컨테이너 unhealthy로 app이 안 뜸 | 최초 기동 시 kafka 준비에 20~40초 소요. 잠시 대기(retries 설정됨) |
| 포트 충돌(5432/6379/9092/8080/5173) | 기존 프로세스 종료 또는 compose 포트 매핑 변경 |
| 로컬 `mvn spring-boot:run`에서 모니터링 DOWN | 정상 — 로컬은 Redis 미기동. Redis 헬스 지표는 기본 비활성화됨 |

---

**문의**: 프레임워크팀 (framework-team@sk.com)
