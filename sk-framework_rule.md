# 엔터프라이즈 프레임워크 통합 개발 가이드 (2025.08.21 최신 업데이트)

이 문서는 엔터프라이즈 시스템 구축 프로젝트의 개발 표준, 아키텍처, 개발 프로세스를 최신 Spring Boot 엔터프라이즈 정책에 따라 정의합니다.
모든 개발자는 본 문서를 기준으로 일관성 있고 효율적인 개발을 수행해야 합니다.

## ✅ [2025-08-21 업데이트] 프레임워크 Rule 준수 완료 현황

### 완료된 업데이트 항목
- ✅ GlobalExceptionHandler 구현 완료 (@ControllerAdvice 기반 일괄 예외 처리)
- ✅ BaseService 트랜잭션 정책 적용 완료 (클래스 readOnly=true, 쓰기 메소드 @Transactional override)
- ✅ ApiResponse/PageResponse 표준 응답 구조 구현 완료
- ✅ 샘플 코드 Rule 준수 업데이트 완료 (User 도메인 기반 CRUD)
- ✅ ResponseEntity 기반 Controller, Bean Validation, 도메인별 Repository 구현
- ✅ OpenAPI(Swagger) 문서화 적용 완료

### Rule 준수 검증 완료
- ✅ .do URI 사용 없음 - RESTful URI 적용 (/api/users)
- ✅ ModelAndView 미사용 - ResponseEntity<T> 반환
- ✅ Controller try-catch 미사용 - GlobalExceptionHandler로 일괄 처리
- ✅ @Transactional 명시적 선언 적용
- ✅ 공통 Mapper 미사용 - 도메인별 Repository 구현
- ✅ egovMap 미사용 - 명시적 DTO/VO 타입 사용
- ✅ @Valid/@Validated Bean Validation 적용
- ✅ 요청/응답 DTO 분리 구현
- ✅ 도메인 기반 패키지 구조 적용

## [2025-08-21 업데이트] Spring Boot 관례, 성능, 유연성, 유지보수성 개선 지침

다음 변경은 기존 규칙보다 우선 적용되며, 프레임워크에 완전히 반영되었습니다.

1) ✅ API 스타일 및 응답 표준 (완료)
- .do URI 접미사 완전 폐지, RESTful URI 사용. 의미 있는 리소스 기반 URI 예) /api/users, /auth/tokens
- Controller 반환: ModelAndView 대신 ResponseEntity<T> 또는 DTO 반환, ApiResponse 래퍼 적용
- 유효성 검증: @Validated/@Valid + Bean Validation, 실패 시 GlobalExceptionHandler에서 일괄 처리

✅ 구현 완료된 예시:
```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> getUsers(@Valid UserSearchRequest search) {
        Page<UserDto> userPage = userService.findUsers(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(userPage)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserCreateRequest req) {
        UserDto user = userService.createUser(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
    }
}
```

2) ✅ 예외 처리 (완료)
- Controller 단 try-catch 금지. 공통 @ControllerAdvice(GlobalExceptionHandler)에서 처리
- 도메인/비즈니스 예외는 커스텀 예���로 throw, 메시지는 국제화 지원

✅ 구현 완료된 GlobalExceptionHandler:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("ENTITY_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        // Bean Validation 오류를 표준 형식으로 반환
    }
}
```

3) ✅ 트랜잭션 (완료)
- 메소드명 접두사 기반 AOP 규칙 폐지. @Transactional을 클래스/메소드에 명시적으로 선언
- 기본: 클래스 @Transactional(readOnly = true), 쓰기 메소드만 @Transactional로 override
- 전파/격리 수준은 요구사항에 따라 명���

✅ 구현 완료된 BaseService:
```java
@Transactional(readOnly = true)
public abstract class BaseService<T extends BaseEntity, ID> {
    
    // 읽기 메소드들은 클래스 레벨 readOnly=true 적용
    public Page<T> findAll(Pageable pageable) { ... }
    public T findById(ID id) { ... }
    
    // 쓰기 메소드만 @Transactional로 override
    @Transactional
    public T save(T entity) { ... }
    
    @Transactional
    public void deleteById(ID id) { ... }
}
```

4) ✅ 데이터 접근 (완료)
- 공통 Mapper(god object) 패턴 폐지, 도메인별 Mapper/Repository 사용
- MyBatis resultType은 egovMap 대신 명시적 DTO/VO 타입. JPA 선택 가능

✅ 구현 완료된 도메인별 Repository:
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE (:username IS NULL OR u.username LIKE %:username%)")
    List<User> findBySearchConditions(@Param("username") String username);
}
```

5) ✅ 패키징 및 명명 (완료)
- 기능(도메인) 기반 패키지 구성. URI depth는 권한/라우팅에만 활용
- 클래스/메소드 네이밍 기존 규칙 유지, URI의 .do 삭제

✅ 구현 완료된 패키지 구조:
```
com/sk/sample/
├── entity/     # User.java
├── dto/        # UserDto.java, UserCreateRequest.java, UserUpdateRequest.java
├── repository/ # UserRepository.java
├── service/    # UserService.java
└── web/        # UserController.java
```

6) ✅ 설정/성능/운영 (완료)
- HikariCP 커넥션풀, 캐시 추상화(@AutoCache), 프로파일별 yml, Actuator/Micrometer/Prometheus
- JSON 로그, 트레이스ID(MDC), 민감정보 마스킹, OWASP Top 10 방어

7) ✅ 문서화/가이드 (완료)
- OpenAPI(Swagger) 자동화, @Operation/@Schema 사용, 샘플코드/주석 템플릿 최신화

✅ 구현 완료된 OpenAPI 문서화:
```java
@Tag(name = "사용자 관리", description = "사용자 CRUD API")
@RestController
public class UserController {
    
    @Operation(summary = "사용자 목록 조회", description = "검색 조건과 페이징을 통해 사용자 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> getUsers(
            @Parameter(description = "검색 조건") UserSearchRequest searchRequest) {
        // 구현 내용
    }
}
```

---

## 1. 프레임워크 개요 및 아키텍처

### ✅ 계층형 아키텍처 (Rule 준수 완료)
- Presentation Layer: Controller (@RestController, ResponseEntity) ✅
- Business Layer: Service (@Service, @Transactional) ✅
- Persistence Layer: Repository (JPA) ✅
- Infrastructure Layer: DB, 외부 API, 배치 등 ✅

### ✅ 의존성 규칙 (완료)
- Controller → Service → Repository ✅
- 각 계층별 DTO로 데이터 전달 ✅
- GlobalExceptionHandler로 예외 일괄 처리 ✅

---

## 2. ✅ 개발 표준 (Rule 준수 완료)

### ✅ 폴더 구조 (구현 완료)
```
src/main/java/com/sk/sample/{domain}/
├── entity/     # 엔티티 클래스
├── dto/        # DTO/VO 클래스  
├── repository/ # Repository 인터페이스
├── service/    # Service 클래스
└── web/        # Controller 클래스
```

### ✅ 명명 규칙 (적용 완료)
- URI: RESTful, .do 미사용 ✅ (예: /api/users)
- Controller: {Domain}Controller ✅ (UserController)
- Service: {Domain}Service ✅ (UserService)
- DTO/VO: {Domain}Dto, {Domain}CreateRequest, {Domain}UpdateRequest ✅
- Repository: {Domain}Repository ✅ (UserRepository)

### ✅ 코딩 스타일 (적용 완료)
- Controller: ResponseEntity 기반, ModelAndView 사용 지양 ✅
- 예외 처리: @ControllerAdvice에서 일괄 처리 ✅
- 트랜잭션: @Transactional 명시적 선언 ✅
- 표준 주석 템플릿 필수 ✅

---

## 3. 핵심 공통 모듈 가이드

### BaseController
- ResponseEntity 기반 표준 응답(ApiResponse, PageResponse) 헬퍼 제공

### BaseService
- 도메인별 Mapper/Repository 주입
- 공통 트랜잭션 정책만 공통화

### 예외 처리
- @ControllerAdvice에서 BizException, SystemException 등 일괄 처리

---

## 4. AI 활용 개발 프로세스

### 1단계: 프롬프트 작성
- 업무, 주요 기능, 패키지 경로, ��래스명, URI 등 명확히 기술

### 2단계: 코드 생성 및 검토
- 명명 규칙, 패키지 구조, 기본 로직 등 표준 준수 여부 확인

### 3단계: 상세 로직 구현
- VO/DTO, SQL, Service/Controller 코드가 표준에 맞게 동작하는지 최종 확인

---

## 5. 샘플/템플릿 (최신)

### Controller 예시
```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> getUsers(@Valid UserSearchRequest search) {
        Page<UserDto> userPage = userService.findUsers(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(userPage)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserCreateRequest req) {
        UserDto user = userService.createUser(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
    }
}
```

### Service 예시
```java
@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public PageResponse<UserDto> findUsers(UserSearchRequest search, Pageable pageable) {
        // 비즈니스 로직 및 페이징 처리
    }

    @Transactional
    public UserDto createUser(UserCreateRequest req) {
        // 등록 로직
    }
}
```

### VO/DTO 예시
```java
@Data
public class UserCreateRequest {
    @NotBlank(message = "{msg.common.required, 제목}")
    private String title;
    private String content;
}
```

### Repository 예시
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE (:username IS NULL OR u.username LIKE %:username%)")
    List<User> findBySearchConditions(@Param("username") String username);
}
```

### 예외 처리 예시
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException e) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(e.getErrorCode(), e.getMessage()));
    }
    // 기타 예외 처리
}
```

### application.yml 예시
```yaml
spring:
  profiles:
    active: local
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
  mybatis:
    mapper-locations: classpath:mapper/**/*_SQL.xml
    type-aliases-package: com.sk.framework.**.dto
logging:
  level:
    com.sk.framework: DEBUG
    org.springframework.web: INFO
sk:
  framework:
    cache:
      enabled: true
      default-ttl: 3600
    external:
      customer:
        api:
          url: ${CUSTOMER_API_URL}
          timeout: 5000
```

### 표준 주석 템플릿 (Java)
```java
/**
 * @className    : UserController
 * @description  : 사용자 관리 컨트롤러
 * @modification : 2025.08.21(홍길동) 최초생성
 * @author       : 홍길동
 * @date         : 2025.08.21
 * @version      : 1.0
 */
```

### 표준 주석 템플릿 (Mapper XML)
```xml
<!--
    쿼리명      : "selectUserList" (사용자 목록 조회)
    설명        : 사용자 목록을 페이징하여 조회합니다.
    parameterType: com.sk.framework.sample.dto.UserSearchRequest
    resultType   : UserDto
    수정일         수정자        수정내용
    ===========   ========    ===========================
    2025.08.21    홍길동        최초 생성
-->
```

---

## 6. 품질 기준
- 테스트 커버리지 80% 이상
- 순환 복잡도 10 이하
- 코드 중복도 5% 이하
- 정적 분석 Critical 이슈 0개
- API 응답 평균 200ms 이하
- OWASP Top 10 방어, 인증/인가 구현

---

이 문서는 최신 엔터프라이즈 Spring Boot 정책에 따라 모든 샘플, 규칙, 템플릿, 체크리스트, 품질 기준을 일관적으로 최신화합니다.

**📞 문의사항**: 프레임워크팀 (framework-team@company.com)
**📅 최종 업데이트**: 2025.08.21
**📌 버전**: 2.0 (Rule 준수 완료)
