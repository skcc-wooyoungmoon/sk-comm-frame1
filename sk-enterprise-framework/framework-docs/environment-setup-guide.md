# 개발 환경 셋업 가이드

> SK Enterprise Framework — 백엔드(Spring Boot) + 프론트엔드(React/Vite) 통합 개발 환경
> 최종 업데이트: 2026.08.13

이 문서를 순서대로 따라 하면 로컬에서 프레임워크를 빌드하고, 샘플 애플리케이션과
사용자 관리 프론트엔드를 함께 실행할 수 있습니다.

---

## 1. 사전 요구사항 (Prerequisites)

| 도구 | 버전 | 확인 명령 |
|------|------|-----------|
| JDK | **25** (LTS) | `java -version` |
| Maven | 3.9 이상 | `mvn -version` |
| Node.js | 20 이상 (22 권장) | `node -v` |
| npm | 10 이상 | `npm -v` |
| Git | 2.x | `git --version` |
| IDE | IntelliJ IDEA / VS Code | - |

### 1.1 JDK 설치 (JDK 25 LTS 필수)

- macOS: `brew install openjdk@25`
- Windows: [Adoptium Temurin 25](https://adoptium.net/) 설치
- Linux: `sudo apt install openjdk-25-jdk` (또는 Temurin 25 tarball)

`JAVA_HOME`을 JDK 25로 설정하고 `java -version`이 **25** 를 가리키는지 확인합니다.

> 이 프로젝트는 Java 25(LTS)로 빌드/실행됩니다. Spring Boot **3.5.6**을 사용하며,
> JDK 23+에서 애노테이션 프로세서(Lombok 등)가 기본 비활성화되는 변경에 대응해
> 루트 pom의 `maven-compiler-plugin`에 `annotationProcessorPaths`를 명시했습니다.
> JDK 25 미만에서는 빌드되지 않습니다.

### 1.2 Maven / Node 설치

- Maven: [maven.apache.org](https://maven.apache.org/download.cgi) 또는 `brew install maven`
- Node: [nodejs.org](https://nodejs.org/) LTS 또는 `nvm install 22`

---

## 2. 소스 클론 및 프로젝트 구조

```bash
git clone <repository-url>
cd sk-comm-frame1
```

```
sk-comm-frame1/
├── sk-enterprise-framework/          # ★ 메인 멀티모듈 프로젝트
│   ├── framework-core/               # 핵심 모듈
│   │   ├── framework-common/         # BaseEntity, ApiResponse, 예외 등 공통
│   │   ├── framework-transaction/    # ★ 거래 패턴(멱등/락/아웃박스/SAGA)
│   │   ├── framework-web/            # GlobalExceptionHandler 등 웹 공통
│   │   ├── framework-data/           # JPA/Repository 공통
│   │   ├── framework-security/       # JWT 인증/인가
│   │   ├── framework-cache/          # 캐시 추상화
│   │   └── framework-logging/        # 로깅/관측성
│   ├── framework-samples/
│   │   └── basic-crud-sample/        # 사용자/주문 샘플 애플리케이션(REST API)
│   ├── framework-starters/           # 통합 스타터
│   ├── framework-frontend/           # ★ 프론트엔드(React + Vite + TS)
│   │   └── user-admin/               # 사용자/권한 관리 SPA
│   └── framework-docs/               # 개발 문서(본 가이드 포함)
├── sk-framework_rule.md              # 프레임워크 개발 규칙
└── framework-creation-rules.md       # 프레임워크 생성 규칙
```

---

## 3. 백엔드 빌드 & 실행

### 3.1 전체 빌드 (로컬 저장소에 설치)

```bash
cd sk-enterprise-framework
mvn clean install -DskipTests
```

- 최초 빌드는 의존성 다운로드로 수 분 소요될 수 있습니다.
- `BUILD SUCCESS`와 함께 13개 모듈이 빌드되면 정상입니다.
- 라이브러리 모듈(core/*)은 `jar`로, 실행 모듈(basic-crud-sample)만 `repackage`되어 실행 가능한 fat-jar가 됩니다.

### 3.2 샘플 애플리케이션 실행

```bash
cd framework-samples/basic-crud-sample
mvn spring-boot:run
```

또는 fat-jar로:

```bash
java -jar target/basic-crud-sample-1.0.0-SNAPSHOT.jar
```

기동되면 다음이 열립니다.

| 항목 | URL |
|------|-----|
| REST API (사용자) | http://localhost:8080/api/users |
| REST API (주문) | http://localhost:8080/api/orders |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 콘솔 | http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:localdb`) |
| Actuator Health | http://localhost:8080/actuator/health |

부팅 로그에 `데모 사용자 4명을 초기화했습니다.`가 보이면 시드 데이터가 적재된 것입니다.

### 3.3 빠른 동작 확인

```bash
# 사용자 목록
curl http://localhost:8080/api/users

# 사용자 생성
curl -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"username":"홍길동","email":"hong@sk.com","phone":"010-1234-5678","role":"USER"}'

# 멱등 주문 (같은 orderNo로 두 번 호출해도 한 번만 생성됨)
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderNo":"ORD-1","customerId":"C1","productId":"P1","quantity":1,"amount":10000}'
```

---

## 4. 프론트엔드 빌드 & 실행

```bash
cd sk-enterprise-framework/framework-frontend/user-admin
npm install
npm run dev
```

- 개발 서버: http://localhost:5173
- API 프록시: `/api` 요청은 `vite.config.ts`의 프록시 설정으로 백엔드(8080)로 전달됩니다.
  (백엔드도 `WebConfig`에서 `http://localhost:5173` CORS를 허용합니다.)

> 백엔드(8080)와 프론트엔드(5173)를 **동시에** 실행해야 사용자 관리 화면이 데이터를 불러옵니다.

프로덕션 빌드:

```bash
npm run build      # dist/ 생성
npm run preview    # 빌드 결과 미리보기
```

---

## 5. 프로파일 & 환경 설정

`application.yml`은 `local`/`dev`/`prod` 프로파일로 분리되어 있습니다.

```bash
# 개발 DB(PostgreSQL) 프로파일로 실행
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 운영 프로파일 (환경변수로 DB 접속정보 주입)
DB_USER=... DB_PASSWORD=... java -jar target/*.jar --spring.profiles.active=prod
```

| 프로파일 | DB | 용도 |
|----------|----|----|
| local(기본) | H2 in-memory | 로컬 개발/데모 |
| dev | PostgreSQL(dev-db) | 개발 서버 |
| prod | PostgreSQL(prod-db) | 운영(풀 크기 상향) |

주요 커스텀 설정(`sk.framework.*`): 캐시 TTL, JWT, 외부 API, **아웃박스 릴레이(`transaction.outbox`)**.

---

## 6. IDE 설정 팁

### IntelliJ IDEA
1. `sk-enterprise-framework/pom.xml`을 프로젝트로 Open.
2. **Lombok 플러그인** 설치 및 `Settings > Build > Compiler > Annotation Processors > Enable` 체크.
3. Project SDK를 17+ 로 설정.
4. `BasicCrudSampleApplication`을 Run.

### VS Code
- 확장: *Extension Pack for Java*, *Spring Boot Extension Pack*, *Lombok Annotations Support*.
- 프론트엔드: *ESLint*, *Prettier*.

---

## 7. 자주 겪는 문제 (Troubleshooting)

| 증상 | 원인/해결 |
|------|-----------|
| `Unable to find main class`로 repackage 실패 | 라이브러리 모듈에서 발생 시, 최상위 pom의 `pluginManagement` 설정 확인. `mvn clean` 후 재빌드. |
| 이전 빌드의 빈 JAR가 재사용됨 | `mvn clean install`로 `clean`을 반드시 포함. |
| Lombok 심볼을 못 찾음 | IDE에서 Lombok 플러그인 + Annotation Processing 활성화. |
| 프론트에서 API 401/CORS 오류 | 백엔드 8080 실행 여부, `WebConfig` CORS 허용 오리진(5173) 확인. |
| 포트 충돌(8080/5173) | 다른 프로세스 종료 또는 포트 변경(`server.port`, `vite --port`). |
| H2 콘솔 접속 안 됨 | JDBC URL을 `jdbc:h2:mem:localdb`로, 사용자 `sa`/빈 비밀번호. |

---

## 8. 다음 단계

- 거래 패턴을 직접 써보려면 → [`transaction-pattern-guide.md`](transaction-pattern-guide.md)
- 기능을 처음부터 만들어보려면 → [`hands-on-tutorial.md`](hands-on-tutorial.md)
- 신규 입사자 온보딩 → [`new-developer-onboarding-guide.md`](new-developer-onboarding-guide.md)

---

**📞 문의**: 프레임워크팀 (framework-team@sk.com)
