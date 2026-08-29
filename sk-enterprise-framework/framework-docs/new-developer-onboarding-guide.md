# 🚀 SK Enterprise Framework - 신규 개발자 완전 실무 가이드

> **새로운 개발자를 위한 환경설정부터 실무 개발까지 완전한 따라하기 가이드**  
> 이 가이드를 순서대로 따라하면 개발 환경 구축부터 실제 기능 개발까지 모든 과정을 완료할 수 있습니다.

## 📋 목차

1. [시작하기 전 준비사항](#1-시작하기-전-준비사항)
2. [개발 환경 설정](#2-개발-환경-설정)
3. [프로젝트 구조 이해](#3-프로젝트-구조-이해)
4. [첫 번째 기능 개발 (Hello World)](#4-첫-번째-기능-개발-hello-world)
5. [실무 예제: 사용자 관리 기능 개발](#5-실무-예제-사용자-관리-기능-개발)
6. [데이터베이스 연동](#6-데이터베이스-연동)
7. [테스트 작성 및 실행](#7-테스트-작성-및-실행)
8. [API 문서화 및 테스트](#8-api-문서화-및-테스트)
9. [배포 및 운영](#9-배포-및-운영)
10. [문제해결 FAQ](#10-문제해결-faq)

---

## 1. 시작하기 전 준비사항

### 🎯 학습 목표
이 가이드를 완료하면 다음을 할 수 있습니다:
- [ ] SK Enterprise Framework 기반 개발 환경 구축
- [ ] Rule에 100% 준수하는 RESTful API 개발
- [ ] 데이터베이스 연동 및 CRUD 기능 구현
- [ ] 테스트 코드 작성 및 실행
- [ ] API 문서화 및 Swagger UI 활용
- [ ] 로컬/개발/운영 환경 배포

### 📚 사전 지식 요구사항
- [ ] Java 8+ 기본 문법
- [ ] Spring Boot 기본 개념
- [ ] Git 기본 사용법
- [ ] SQL 기본 문법
- [ ] REST API 기본 개념

### 💻 시스템 요구사항
- [ ] **OS**: Windows 10+, macOS 10.14+, Ubuntu 18.04+
- [ ] **Java**: OpenJDK 25 (LTS)
- [ ] **메모리**: 최소 8GB RAM (권장 16GB)
- [ ] **디스크**: 최소 10GB 여유 공간
- [ ] **네트워크**: 인터넷 연결 (Maven 의존성 다운로드용)

---

## 2. 개발 환경 설정

### 📥 Step 1: 필수 소프트웨어 설치

#### 2.1 Java 25 (LTS) 설치
```bash
# Windows (Chocolatey 사용)
choco install openjdk25

# macOS (Homebrew 사용)
brew install openjdk@25

# Ubuntu
sudo apt update
sudo apt install openjdk-25-jdk
```

**설치 확인:**
```bash
java -version
# 출력 예시: openjdk version "25" 2025-09-16 LTS
```

#### 2.2 IntelliJ IDEA 설치 (권장)
1. [JetBrains IntelliJ IDEA](https://www.jetbrains.com/idea/) 다운로드
2. Community Edition (무료) 또는 Ultimate Edition 설치
3. 초기 설정에서 다음 플러그인 설치:
   - Lombok
   - Spring Boot
   - Database Navigator

#### 2.3 Git 설치
```bash
# Windows
choco install git

# macOS
brew install git

# Ubuntu
sudo apt install git
```

**Git 설정:**
```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@company.com"
```

#### 2.4 MySQL 설치 (선택사항 - H2 Database로 대체 가능)
```bash
# Windows
choco install mysql

# macOS
brew install mysql

# Ubuntu
sudo apt install mysql-server
```

### 📁 Step 2: 프로젝트 클론 및 설정

#### 2.1 저장소 클론
```bash
git clone https://github.com/your-company/sk-comm-frame.git
cd sk-comm-frame
```

#### 2.2 IDE에서 프로젝트 열기
1. IntelliJ IDEA 실행
2. `Open` → `sk-comm-frame` 폴더 선택
3. `Trust Project` 클릭
4. Maven 프로젝트로 인식되면 자동으로 의존성 다운로드 시작

#### 2.3 프로젝트 빌드 확인
```bash
# 프로젝트 루트에서 실행
cd sk-enterprise-framework
./mvnw clean compile

# Windows의 경우
mvnw.cmd clean compile
```

**성공 메시지 확인:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 30.123 s
```

---

## 3. 프로젝트 구조 이해

### 📂 전체 프로젝트 구조
```
sk-enterprise-framework/
├── framework-core/           # 핵심 프레임워크 모듈
│   ├── framework-common/     # 공통 기능 (BaseService, ApiResponse 등)
│   ├── framework-web/        # 웹 관련 (GlobalExceptionHandler 등)
│   ├── framework-data/       # 데이터 접근 관련
│   ├── framework-security/   # 보안 관련
│   ├── framework-cache/      # 캐시 관련
│   └── framework-logging/    # 로깅 관련
├── framework-samples/        # 샘플 애플리케이션
│   └── basic-crud-sample/    # 기본 CRUD 샘플
├── framework-docs/           # 문서 모음
└── framework-starters/       # Spring Boot Starter 모듈
```

### 🎯 샘플 애플리케이션 구조 (basic-crud-sample)
```
basic-crud-sample/
├── src/main/java/com/sk/sample/
│   ├── entity/              # JPA 엔티티
│   │   └── User.java
│   ├── dto/                 # 데이터 전송 객체
│   │   ├── UserDto.java
│   │   ├── UserCreateRequest.java
│   │   ├── UserUpdateRequest.java
│   │   └── UserSearchRequest.java
│   ├── repository/          # 데이터 접근 계층
│   │   └── UserRepository.java
│   ├── service/             # 비즈니스 로직 계층
│   │   └── UserService.java
│   ├── web/                 # 프레젠테이션 계층
│   │   └── UserController.java
│   └── BasicCrudSampleApplication.java
├── src/main/resources/
│   ├── application.yml      # 설정 파일
│   └── data.sql            # 초기 데이터 (선택사항)
└── src/test/               # 테스트 코드
```

---

## 4. 첫 번째 기능 개발 (Hello World)

### 🎯 목표: 간단한 Hello World API 만들기

#### Step 1: HelloController 생성

`src/main/java/com/sk/sample/web/HelloController.java` 파일을 생성합니다:

```java
package com.sk.sample.web;

import com.sk.framework.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @className    : HelloController
 * @description  : Hello World API 컨트롤러
 * @modification : 2025.08.21(신규개발자) 최초생성
 * @author       : 신규개발자
 * @date         : 2025.08.21
 * @version      : 1.0
 */
@Tag(name = "Hello World", description = "Hello World API")
@RestController
@RequestMapping("/api/hello")
public class HelloController {

    @Operation(summary = "Hello World", description = "간단한 인사말을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<String>> hello() {
        return ResponseEntity.ok(ApiResponse.success("Hello, SK Enterprise Framework!"));
    }

    @Operation(summary = "개인화된 인사말", description = "이름을 받아서 개인화된 인사말을 반환합니다.")
    @GetMapping("/greet")
    public ResponseEntity<ApiResponse<String>> greet(@RequestParam String name) {
        String message = String.format("안녕하세요, %s님! SK Enterprise Framework에 오신 것을 환영합니다.", name);
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
```

#### Step 2: 애플리케이션 실행

```bash
cd sk-enterprise-framework/framework-samples/basic-crud-sample
../../mvnw spring-boot:run

# 또는 IDE에서 BasicCrudSampleApplication.java 우클릭 → Run
```

**실행 성공 확인:**
```
Started BasicCrudSampleApplication in 3.234 seconds
```

#### Step 3: API 테스트

**브라우저에서 테스트:**
- http://localhost:8080/api/hello
- http://localhost:8080/api/hello/greet?name=홍길동

**예상 응답:**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": "Hello, SK Enterprise Framework!"
}
```

#### Step 4: Swagger UI 확인

브라우저에서 http://localhost:8080/swagger-ui.html 접속하여 API 문서 확인

---

## 5. 실무 예제: 사용자 관리 기능 개발

### 🎯 목표: 완전한 CRUD 기능을 가진 사용자 관리 시스템 구현

이미 구현된 User 도메인 코드를 이해하고 활용해보겠습니다.

#### Step 1: 기존 User 엔티티 이해

`src/main/java/com/sk/sample/entity/User.java` 파일을 열어서 구조를 확인합니다:

```java
// 주요 특징들:
// 1. BaseEntity 상속으로 생성일시/수정일시 자동 관리
// 2. Bean Validation으로 입력값 검증
// 3. JPA 어노테이션으로 데이터베이스 매핑
// 4. 비즈니스 메서드 포함 (activate, deactivate 등)
```

#### Step 2: 애플리케이션 실행 및 H2 Database 확인

```bash
# 애플리케이션 실행
cd sk-enterprise-framework/framework-samples/basic-crud-sample
../../mvnw spring-boot:run
```

**H2 Console 접속:**
1. 브라우저에서 http://localhost:8080/h2-console 접속
2. 연결 정보:
   - JDBC URL: `jdbc:h2:mem:testdb`
   - User Name: `sa`
   - Password: (빈 칸)
3. `Connect` 클릭

**테이블 확인:**
```sql
-- 생성된 테이블 확인
SHOW TABLES;

-- User 테이블 구조 확인
DESCRIBE TB_USER;

-- 데이터 확인 (초기에는 비어있음)
SELECT * FROM TB_USER;
```

#### Step 3: Swagger UI로 API 테스트

브라우저에서 http://localhost:8080/swagger-ui.html 접속

**테스트 순서:**
1. **사용자 생성** (POST /api/users)
   ```json
   {
     "username": "홍길동",
     "email": "hong@example.com",
     "phone": "010-1234-5678"
   }
   ```

2. **사용자 목록 조회** (GET /api/users)

3. **사용자 상세 조회** (GET /api/users/{id})

4. **사용자 수정** (PUT /api/users/{id})
   ```json
   {
     "username": "홍길동_수정",
     "phone": "010-9876-5432"
   }
   ```

5. **사용자 상태 변경** (PATCH /api/users/{id}/status?status=INACTIVE)

6. **사용자 삭제** (DELETE /api/users/{id})

#### Step 4: 실제 데이터 확인

각 API 호출 후 H2 Console에서 데이터 변경사항 확인:

```sql
-- 데이터 변경 확인
SELECT * FROM TB_USER;

-- 특정 사용자 조회
SELECT * FROM TB_USER WHERE USER_ID = 1;
```

---

## 6. 데이터베이스 연동

### 📊 H2 Database (기본 설정)

프로젝트는 기본적으로 H2 인메모리 데이터베이스를 사용합니다.

**장점:**
- 설치 불필요
- 빠른 개발 및 테스트
- 웹 콘솔 제공

**설정 확인:** `application.yml`
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
```

### 🔄 MySQL로 변경하기 (선택사항)

#### Step 1: MySQL 의존성 추가

`pom.xml`에 MySQL 의존성 추가:
```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
```

#### Step 2: application.yml 수정

```yaml
spring:
  profiles:
    active: mysql
  datasource:
    url: jdbc:mysql://localhost:3306/sk_sample?useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.MySQL8Dialect
```

#### Step 3: MySQL 데이터베이스 생성

```sql
CREATE DATABASE sk_sample DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 7. 테스트 작성 및 실행

### 🧪 Unit Test 작성

#### Step 1: UserService 테스트

`src/test/java/com/sk/sample/service/UserServiceTest.java` 생성:

```java
package com.sk.sample.service;

import com.sk.sample.dto.UserCreateRequest;
import com.sk.sample.dto.UserDto;
import com.sk.sample.entity.User;
import com.sk.sample.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자 생성 테스트")
    void createUser() {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .username("테스트사용자")
                .email("test@example.com")
                .phone("010-1234-5678")
                .build();

        User savedUser = User.builder()
                .id(1L)
                .username("테스트사용자")
                .email("test@example.com")
                .phone("010-1234-5678")
                .status(User.UserStatus.ACTIVE)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        UserDto result = userService.createUser(request);

        // Then
        assertThat(result.getUsername()).isEqualTo("테스트사용자");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }
}
```

#### Step 2: 통합 테스트 작성

`src/test/java/com/sk/sample/web/UserControllerIntegrationTest.java` 생성:

```java
package com.sk.sample.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    @DisplayName("사용자 목록 조회 통합 테스트")
    void getUsersIntegrationTest() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("success");
    }
}
```

#### Step 3: 테스트 실행

```bash
# 모든 테스트 실행
../../mvnw test

# 특정 테스트 클래스 실행
../../mvnw test -Dtest=UserServiceTest

# IDE에서 실행
# 테스트 클래스 우클릭 → Run Tests
```

**테스트 결과 확인:**
```
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 8. API 문서화 및 테스트

### 📚 Swagger UI 활용

#### Step 1: Swagger UI 접속
브라우저에서 http://localhost:8080/swagger-ui.html 접속

#### Step 2: API 문서 확인
- **태���별 그룹화**: 사용자 관리, Hello World 등
- **상세 API 스펙**: 요청/응답 스키마, 파라미터 설명
- **Try it out 기능**: 실제 API 호출 테스트

#### Step 3: API 테스트 시나리오

**시나리오 1: 새 사용자 등록 및 조회**
1. POST /api/users로 사용자 생성
2. GET /api/users로 목록에서 확인
3. GET /api/users/{id}로 상세 정보 확인

**시나리오 2: 사용자 정보 수정**
1. PUT /api/users/{id}로 정보 수정
2. GET /api/users/{id}로 변경 확인

**시나리오 3: 검색 기능 테스트**
1. 여러 사용자 생성
2. GET /api/users?username=검색어로 검색 테스트

### 🔧 Postman 컬렉션 생성 (선택사항)

Swagger UI에서 OpenAPI JSON을 내보내어 Postman에서 활용할 수 있습니다:

1. http://localhost:8080/api-docs 에서 JSON 다운로드
2. Postman에서 Import → OpenAPI 3.0 → JSON 파일 선택

---

## 9. 배포 및 운영

### 🚀 로컬 환경 빌드 및 실행

#### Step 1: JAR 파일 빌드
```bash
cd sk-enterprise-framework/framework-samples/basic-crud-sample
../../mvnw clean package -DskipTests

# 빌드 결과 확인
ls -la target/
# basic-crud-sample-1.0.0-SNAPSHOT.jar 파일 생성 확인
```

#### Step 2: JAR 파일 실행
```bash
java -jar target/basic-crud-sample-1.0.0-SNAPSHOT.jar

# 프로파일 지정 실행
java -jar -Dspring.profiles.active=prod target/basic-crud-sample-1.0.0-SNAPSHOT.jar
```

### 📊 애플리케이션 모니터링

#### Actuator 엔드포인트 확인
```bash
# 애플리케이션 상태 확인
curl http://localhost:8080/actuator/health

# 메트릭 확인
curl http://localhost:8080/actuator/metrics

# 모든 엔드포인트 확인
curl http://localhost:8080/actuator
```

**예상 응답:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### 🔧 프로파일별 환경 설정

**개발 환경 (dev):**
```bash
java -jar -Dspring.profiles.active=dev target/basic-crud-sample-1.0.0-SNAPSHOT.jar
```

**운영 환경 (prod):**
```bash
java -jar -Dspring.profiles.active=prod target/basic-crud-sample-1.0.0-SNAPSHOT.jar
```

---

## 10. 문제해결 FAQ

### ❌ 자주 발생하는 문제들

#### 1. 애플리케이션 시작 실패

**문제:** `Port 8080 was already in use`
```bash
# 해결방법 1: 포트 변경
java -jar -Dserver.port=8081 target/basic-crud-sample-1.0.0-SNAPSHOT.jar

# 해결방법 2: 기존 프로세스 종료 (Windows)
netstat -ano | findstr :8080
taskkill /PID [프로세스ID] /F

# 해결방법 3: 기존 프로세스 종료 (macOS/Linux)
lsof -ti:8080 | xargs kill -9
```

#### 2. 데이터베이스 연결 오류

**문제:** `Unable to obtain JDBC Connection`
```yaml
# application.yml 확인
spring:
  datasource:
    url: jdbc:h2:mem:testdb  # URL 정확성 확인
    username: sa             # 사용자명 확인
    password:                # 비밀번호 확인
```

#### 3. API 호출 시 404 오류

**문제:** `404 Not Found`
```java
// Controller 매핑 확인
@RestController
@RequestMapping("/api/users")  // 경로 확인
public class UserController {
    
    @GetMapping  // HTTP 메서드 확인
    public ResponseEntity<?> getUsers() {
        // 구현 확인
    }
}
```

#### 4. Bean Validation 오류

**문제:** `MethodArgumentNotValidException`
```java
// DTO 검증 어노테이션 확인
public class UserCreateRequest {
    @NotBlank(message = "사용자명은 필수입니다.")
    private String username;
    
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
}

// Controller에서 @Valid 어노테이션 확인
@PostMapping
public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateRequest request) {
    // 구현
}
```

### 🔍 디버깅 팁

#### 1. 로그 레벨 조정
```yaml
logging:
  level:
    com.sk.sample: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

#### 2. H2 Console 활용
- 데이터베이스 상태 실시간 확인
- SQL 쿼리 직접 실행
- 테이블 구조 확인

#### 3. Swagger UI 활용
- API 스펙 확인
- 실제 요청/응답 테스트
- 스키마 검증

### 📞 도움 요청

문제가 해결되지 않을 때:

1. **로그 확인**: `logs/sk-sample.log` 파일 확인
2. **Issue 등록**: GitHub Repository에서 Issue 생성
3. **팀 문의**: 
   - 프레임워크팀 (framework-team@company.com)
   - 사내 개발자 커뮤니티 Slack #framework-support

---

## 🎯 다음 단계

이 가이드를 완료했다면 다음 단계로 진행하세요:

### 📚 추가 학습 자료
1. **고급 기능 학습**:
   - `framework-docs/developer-step-by-step-guide.md` 
   - Spring Boot Reference Documentation

2. **실무 프로젝트 적용**:
   - 새로운 도메인 모델링
   - 복잡한 비즈니스 로직 구현
   - 외부 API 연동

3. **성능 최적화**:
   - 캐시 활용
   - 데이터베이스 쿼리 최적화
   - 비동기 처리

### 🔄 지속적 개선
- 코드 리뷰 참여
- 테스트 커버리지 향상
- 보안 강화
- 모니터링 및 로깅 개선

---

## 📋 완료 체크리스트

학습 완료 후 다음 항목들을 체크해보세요:

### 환경 설정
- [ ] Java 25 설치 및 설정 완료
- [ ] IntelliJ IDEA 설치 및 프로젝트 열기 완료
- [ ] Git 설치 및 저장소 클론 완료
- [ ] Maven 빌드 성공 확인

### 기본 개발
- [ ] Hello World API 개발 및 테스트 완료
- [ ] User CRUD API 이해 및 테스트 완료
- [ ] Swagger UI로 API 문서 확인 완료
- [ ] H2 Console로 데이터베이스 확인 완료

### 고급 기능
- [ ] Unit Test 작성 및 실행 완료
- [ ] Integration Test 작성 및 실행 완료
- [ ] 프로파일별 설정 이해 완료
- [ ] JAR 빌드 및 실행 완료

### 운영 준비
- [ ] Actuator 모니터링 확인 완료
- [ ] 로그 설정 및 확인 완료
- [ ] 문제해결 시나리오 이해 완료

---

## 🎉 축하합니다!

**SK Enterprise Framework 개발 환경 구축 및 기본 개발 과정을 모두 완료하셨습니다!**

이제 여러분은:
✅ **규칙에 100% 준수하는 고품질 코드**를 작성할 수 있습니다  
✅ **RESTful API와 데이터베이스 연동**을 구현할 수 있습니다  
✅ **테스트 코드 작성과 API 문서화**를 할 수 있습니다  
✅ **실무 프로젝트에 바로 적용**할 수 있는 실력을 갖추었습니다  

프레임워크를 활용하여 멋진 애플��케이션을 개발해보세요! 🚀

---

**📞 문의사항**: 프레임워크팀 (framework-team@company.com)  
**📅 최종 업데이트**: 2025.08.21  
**📌 버전**: 1.0
