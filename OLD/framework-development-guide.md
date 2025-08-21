# 프레임워크 개발 룰 & 프롬프트 가이드

## 📋 목차
1. [프레임워크 아키텍처 설계](#1-프레임워크-아키텍처-설계)
2. [폴더 구조 표준](#2-폴더-구조-표준)
3. [핵심 기능 모듈](#3-핵심-기능-모듈)
4. [개발 룰 & 컨벤션](#4-개발-룰--컨벤션)
5. [프레임워크 생성 프롬프트](#5-프레임워크-생성-프롬프트)
6. [코드 생성 템플릿](#6-코드-생성-템플릿)
7. [확장성 가이드](#7-확장성-가이드)
8. [테스트 전략](#8-테스트-전략)
9. [문서화 표준](#9-문서화-표준)
10. [배포 및 버전 관리](#10-배포-및-버전-관리)

---

## 1. 프레임워크 아키텍처 설계

### 1.1 계층형 아키텍처
```
┌─────────────────────────────────────┐
│           Presentation Layer        │  ← API, Web Interface
├─────────────────────────────────────┤
│           Application Layer         │  ← Business Logic, Services
├─────────────────────────────────────┤
│           Domain Layer              │  ← Core Business Rules
├─────────────────────────────────────┤
│           Infrastructure Layer      │  ← Data Access, External APIs
└─────────────────────────────────────┘
```

### 1.2 모듈형 구조
```
Framework Core
├── Authentication Module
├── Authorization Module  
├── Data Access Module
├── Validation Module
├── Logging Module
├── Cache Module
├── Security Module
└── Configuration Module
```

---

## 2. 폴더 구조 표준

### 2.1 루트 프로젝트 구조
```
framework-project/
├── framework-core/              # 핵심 프레임워크
├── framework-starters/          # Spring Boot Starters
├── framework-samples/           # 샘플 프로젝트
├── framework-docs/              # 문서
├── framework-tools/             # 개발 도구
├── pom.xml                      # 루트 POM
├── README.md
├── CHANGELOG.md
└── LICENSE
```

### 2.2 Core 모듈 구조
```
framework-core/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/company/framework/
│   │   │       ├── core/                # 핵심 기능
│   │   │       │   ├── annotation/      # 커스텀 어노테이션
│   │   │       │   ├── config/          # 자동 설정
│   │   │       │   ├── exception/       # 예외 처리
│   │   │       │   ├── util/            # 유틸리티
│   │   │       │   └── validation/      # 검증
│   │   │       ├── security/            # 보안 모듈
│   │   │       │   ├── authentication/
│   │   │       │   ├── authorization/
│   │   │       │   └── encryption/
│   │   │       ├── data/                # 데이터 접근
│   │   │       │   ├── repository/
│   │   │       │   ├── entity/
│   │   │       │   └── converter/
│   │   │       ├── web/                 # 웹 기능
│   │   │       │   ├── controller/
│   │   │       │   ├── filter/
│   │   │       │   └── interceptor/
│   │   │       ├── cache/               # 캐시 모듈
│   │   │       ├── logging/             # 로깅 모듈
│   │   │       └── monitoring/          # 모니터링
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── spring.factories     # 자동 설정
│   │       ├── config/
│   │       └── templates/
│   └── test/
└── pom.xml
```

### 2.3 Starter 모듈 구조
```
framework-starters/
├── framework-starter-web/       # 웹 스타터
├── framework-starter-data/      # 데이터 스타터
├── framework-starter-security/  # 보안 스타터
├── framework-starter-cache/     # 캐시 스타터
└── framework-starter-all/       # 전체 스타터
```

---

## 3. 핵심 기능 모듈

### 3.1 자동 설정 (AutoConfiguration)
```java
@Configuration
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(FrameworkDataProperties.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class FrameworkDataAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public FrameworkDataManager frameworkDataManager(
            FrameworkDataProperties properties) {
        return new FrameworkDataManager(properties);
    }
}
```

### 3.2 커스텀 어노테이션
```java
// 자동 CRUD 생성
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoCrud {
    String path() default "";
    boolean enableCreate() default true;
    boolean enableRead() default true;
    boolean enableUpdate() default true;
    boolean enableDelete() default true;
}

// 자동 검증
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoValidation {
    String message() default "";
    Class<?>[] groups() default {};
}

// 자동 캐시
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoCache {
    String key() default "";
    int expireMinutes() default 60;
}
```

### 3.3 Base 클래스들
```java
// Base Entity
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;
}

// Base Controller
@RestController
public abstract class BaseController<T extends BaseEntity, ID> {
    
    protected abstract BaseService<T, ID> getService();
    
    @GetMapping
    public ResponseEntity<List<T>> findAll() {
        return ResponseEntity.ok(getService().findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<T> findById(@PathVariable ID id) {
        return ResponseEntity.ok(getService().findById(id));
    }
    
    @PostMapping
    public ResponseEntity<T> create(@RequestBody T entity) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(getService().save(entity));
    }
}

// Base Service
public abstract class BaseService<T extends BaseEntity, ID> {
    
    protected abstract JpaRepository<T, ID> getRepository();
    
    public List<T> findAll() {
        return getRepository().findAll();
    }
    
    public T findById(ID id) {
        return getRepository().findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found: " + id));
    }
    
    public T save(T entity) {
        return getRepository().save(entity);
    }
    
    public void deleteById(ID id) {
        getRepository().deleteById(id);
    }
}
```

---

## 4. 개발 룰 & 컨벤션

### 4.1 명명 규칙

#### 패키지 명명
```
com.company.framework.{module}.{submodule}
예: com.company.framework.security.authentication
```

#### 클래스 명명
```
- Configuration: {Module}AutoConfiguration
- Properties: {Module}Properties  
- Starter: {Module}Starter
- Exception: {Module}Exception
- Utils: {Module}Utils
- Manager: {Module}Manager
```

#### 어노테이션 명명
```
- Enable{Feature}: @EnableFrameworkSecurity
- Auto{Action}: @AutoCrud, @AutoCache
- Framework{Purpose}: @FrameworkComponent
```

### 4.2 코드 작성 룰

#### 1. 단일 책임 원칙 (SRP)
```java
// 좋은 예: 각 클래스가 하나의 책임만 가짐
public class UserAuthenticationService {
    public boolean authenticate(String username, String password) { }
}

public class UserAuthorizationService {
    public boolean authorize(User user, String resource) { }
}
```

#### 2. 의존성 주입 원칙
```java
// 인터페이스 기반 의존성 주입
@Component
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
}
```

#### 3. 설정 외부화
```java
@ConfigurationProperties(prefix = "framework.security")
@Data
public class SecurityProperties {
    private boolean enabled = true;
    private String secretKey;
    private int tokenExpirationHours = 24;
    private List<String> allowedOrigins = new ArrayList<>();
}
```

### 4.3 에러 처리 전략

#### 프레임워크 예외 계층
```java
// 최상위 프레임워크 예외
public abstract class FrameworkException extends RuntimeException {
    private final String errorCode;
    
    protected FrameworkException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

// 모듈별 예외
public class SecurityException extends FrameworkException {
    public SecurityException(String message) {
        super("SECURITY_ERROR", message);
    }
}

public class DataException extends FrameworkException {
    public DataException(String message) {
        super("DATA_ERROR", message);
    }
}
```

---

## 5. 프레임워크 생성 프롬프트

### 5.1 기본 프레임워크 생성 프롬프트
```
당신은 Enterprise급 Spring Boot 프레임워크 개발자입니다.

다음 요구사항에 맞는 프레임워크 모듈을 생성해주세요:

## 기본 정보
- 모듈명: {MODULE_NAME}
- 기능: {FEATURE_DESCRIPTION}  
- 의존성: {DEPENDENCIES}

## 생성 규칙
1. 다음 구조를 따라 생성:
   - AutoConfiguration 클래스
   - Properties 클래스
   - 핵심 기능 클래스들
   - 테스트 코드

2. 명명 규칙 준수:
   - 클래스명: {Module}AutoConfiguration
   - 패키지: com.company.framework.{module}
   - Properties: {Module}Properties

3. 필수 어노테이션:
   - @Configuration
   - @ConditionalOnClass
   - @EnableConfigurationProperties
   - @AutoConfigureAfter

4. 확장 가능하도록 인터페이스 기반 설계

## 추가 요구사항
{ADDITIONAL_REQUIREMENTS}

위 규칙에 따라 완전한 프레임워크 모듈을 생성해주세요.
```

### 5.2 CRUD 자동 생성 프롬프트
```
Spring Boot 프레임워크에서 @AutoCrud 어노테이션을 사용한 
자동 CRUD API 생성 기능을 구현해주세요.

## 요구사항
1. Entity 클래스에 @AutoCrud 어노테이션 추가시 자동으로 다음 생성:
   - Controller (CRUD API 엔드포인트)
   - Service (비즈니스 로직)
   - Repository (데이터 접근)

2. 어노테이션 옵션:
   - path: API 경로 지정
   - enableCreate, enableRead, enableUpdate, enableDelete: 기능 활성화

3. 생성되는 API:
   - GET /{path} : 전체 조회
   - GET /{path}/{id} : 단건 조회  
   - POST /{path} : 생성
   - PUT /{path}/{id} : 수정
   - DELETE /{path}/{id} : 삭제

4. 구현 방식:
   - AnnotationProcessor 또는 BeanPostProcessor 사용
   - 런타임 Bean 등록
   - AOP 기반 처리

완전한 구현 코드를 제공해주세요.
```

### 5.3 보안 모듈 생성 프롬프트
```
Enterprise 급 Spring Boot 보안 프레임워크 모듈을 개발해주세요.

## 핵심 기능
1. JWT 기반 인증/인가
2. Role 기반 권한 관리
3. API 레벨 보안 설정
4. 자동 보안 검증

## 구성 요소
1. AutoConfiguration:
   - SecurityAutoConfiguration
   - JwtAutoConfiguration

2. Properties:
   - SecurityProperties (JWT 설정, 허용 도메인 등)

3. 핵심 클래스:
   - JwtTokenProvider
   - SecurityManager
   - AuthenticationFilter
   - AuthorizationAspect

4. 어노테이션:
   - @RequireRole("ADMIN")
   - @PublicApi
   - @SecureApi

## 사용 예시
```java
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
public class AdminController {
    
    @GetMapping("/users")
    @RequireRole("SUPER_ADMIN") 
    public List<User> getUsers() { }
    
    @PostMapping("/users")
    public User createUser(@RequestBody User user) { }
}
```

위 요구사항에 맞는 완전한 보안 모듈을 생성해주세요.
```

---

## 6. 코드 생성 템플릿

### 6.1 AutoConfiguration 템플릿
```java
@Configuration
@ConditionalOnClass({${MAIN_CLASS}.class})
@EnableConfigurationProperties(${MODULE}Properties.class)
@AutoConfigureAfter({${AFTER_CLASSES}})
public class ${MODULE}AutoConfiguration {
    
    private final ${MODULE}Properties properties;
    
    public ${MODULE}AutoConfiguration(${MODULE}Properties properties) {
        this.properties = properties;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ${MODULE}Manager ${MODULE_LOWER}Manager() {
        return new ${MODULE}Manager(properties);
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "${PREFIX}", name = "enabled", havingValue = "true")
    public ${MODULE}Processor ${MODULE_LOWER}Processor() {
        return new ${MODULE}Processor();
    }
}
```

### 6.2 Properties 템플릿
```java
@ConfigurationProperties(prefix = "${PREFIX}")
@Data
public class ${MODULE}Properties {
    
    /**
     * Enable ${MODULE} functionality
     */
    private boolean enabled = true;
    
    /**
     * ${MODULE} configuration settings
     */
    private String ${PROPERTY1};
    private int ${PROPERTY2} = ${DEFAULT_VALUE};
    private List<String> ${PROPERTY3} = new ArrayList<>();
    
    /**
     * Nested configuration
     */
    private ${NESTED_CLASS} ${NESTED_PROPERTY} = new ${NESTED_CLASS}();
    
    @Data
    public static class ${NESTED_CLASS} {
        private String property1;
        private boolean property2 = false;
    }
}
```

### 6.3 Starter POM 템플릿
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.company</groupId>
        <artifactId>framework-starters</artifactId>
        <version>${framework.version}</version>
    </parent>
    
    <artifactId>framework-starter-${MODULE}</artifactId>
    <name>Framework Starter ${MODULE}</name>
    <description>Starter for Framework ${MODULE}</description>
    
    <dependencies>
        <!-- Framework Core -->
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>framework-${MODULE}</artifactId>
        </dependency>
        
        <!-- Spring Boot Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        
        <!-- Optional Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-${OPTIONAL}</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

---

## 7. 확장성 가이드

### 7.1 플러그인 시스템
```java
// 플러그인 인터페이스
public interface FrameworkPlugin {
    String getName();
    String getVersion();
    void initialize(FrameworkContext context);
    void destroy();
}

// 플러그인 매니저
@Component
public class PluginManager {
    
    private final Map<String, FrameworkPlugin> plugins = new ConcurrentHashMap<>();
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        loadPlugins();
    }
    
    private void loadPlugins() {
        // 클래스패스에서 플러그인 자동 발견 및 로드
        ServiceLoader<FrameworkPlugin> loader = 
            ServiceLoader.load(FrameworkPlugin.class);
        
        for (FrameworkPlugin plugin : loader) {
            registerPlugin(plugin);
        }
    }
}
```

### 7.2 확장 포인트 정의
```java
// 확장 포인트 인터페이스
public interface ExtensionPoint<T> {
    String getName();
    Class<T> getType();
    T getDefault();
}

// 확장 레지스트리
@Component
public class ExtensionRegistry {
    
    private final Map<String, List<Object>> extensions = new ConcurrentHashMap<>();
    
    public <T> void register(ExtensionPoint<T> point, T extension) {
        extensions.computeIfAbsent(point.getName(), k -> new ArrayList<>())
                 .add(extension);
    }
    
    public <T> List<T> getExtensions(ExtensionPoint<T> point) {
        return extensions.getOrDefault(point.getName(), Collections.emptyList())
                        .stream()
                        .map(point.getType()::cast)
                        .collect(Collectors.toList());
    }
}
```

---

## 8. 테스트 전략

### 8.1 AutoConfiguration 테스트
```java
@TestConfiguration
public class TestAutoConfiguration {
    
    @MockBean
    private DataSource dataSource;
    
    @MockBean  
    private RedisTemplate<String, Object> redisTemplate;
}

@SpringBootTest
@Import(TestAutoConfiguration.class)
class FrameworkAutoConfigurationTest {
    
    @Autowired
    private ApplicationContext context;
    
    @Test
    void shouldCreateFrameworkBeans() {
        assertThat(context.getBean(FrameworkManager.class)).isNotNull();
        assertThat(context.getBean(FrameworkProcessor.class)).isNotNull();
    }
    
    @Test
    void shouldRespectConditionalAnnotations() {
        // ConditionalOnProperty 테스트
        assertThat(context.containsBean("conditionalBean")).isFalse();
    }
}
```

### 8.2 통합 테스트 템플릿
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "framework.security.enabled=true",
    "framework.cache.enabled=false"
})
class FrameworkIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldProvideSecureEndpoints() throws Exception {
        mockMvc.perform(get("/api/secure"))
               .andExpect(status().isUnauthorized());
    }
}
```

---

## 9. 문서화 표준

### 9.1 README 템플릿
```markdown
# Framework ${MODULE} Starter

## 개요
${MODULE} 기능을 Spring Boot 애플리케이션에 쉽게 통합할 수 있는 스타터입니다.

## 주요 기능
- ${FEATURE_1}
- ${FEATURE_2} 
- ${FEATURE_3}

## 빠른 시작

### 1. 의존성 추가
```xml
<dependency>
    <groupId>com.company</groupId>
    <artifactId>framework-starter-${MODULE}</artifactId>
    <version>${VERSION}</version>
</dependency>
```

### 2. 설정
```yaml
framework:
  ${MODULE}:
    enabled: true
    ${PROPERTY}: ${VALUE}
```

### 3. 사용 예시
```java
@Service
public class MyService {
    
    @Autowired
    private ${MODULE}Manager ${MODULE_LOWER}Manager;
    
    public void doSomething() {
        ${MODULE_LOWER}Manager.process();
    }
}
```

## 설정 옵션
| 속성 | 기본값 | 설명 |
|------|--------|------|
| framework.${MODULE}.enabled | true | 기능 활성화 여부 |
| framework.${MODULE}.${PROPERTY} | ${DEFAULT} | ${DESCRIPTION} |

## 고급 설정
### 커스텀 구현체 등록
### 확장 포인트 활용
### 플러그인 개발

## 문제 해결
### 자주 묻는 질문
### 알려진 이슈

## 라이센스
MIT License
```

### 9.2 JavaDoc 표준
```java
/**
 * Framework ${MODULE} 자동 설정 클래스
 * 
 * <p>${MODULE} 기능을 Spring Boot 애플리케이션에 자동으로 설정합니다.</p>
 * 
 * <p>다음 조건이 만족될 때 활성화됩니다:</p>
 * <ul>
 *   <li>{@link ${MAIN_CLASS}}가 클래스패스에 존재</li>
 *   <li>{@code framework.${MODULE}.enabled=true} 설정</li>
 * </ul>
 * 
 * @author Framework Team
 * @since 1.0.0
 * @see ${MODULE}Properties
 * @see ${MODULE}Manager
 */
@Configuration
@ConditionalOnClass(${MAIN_CLASS}.class)
public class ${MODULE}AutoConfiguration {
    
    /**
     * ${MODULE} 매니저 빈을 생성합니다.
     * 
     * @param properties ${MODULE} 설정 속성
     * @return ${MODULE}Manager 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    public ${MODULE}Manager ${MODULE_LOWER}Manager(${MODULE}Properties properties) {
        return new ${MODULE}Manager(properties);
    }
}
```

---

## 10. 배포 및 버전 관리

### 10.1 버전 관리 전략
```
Major.Minor.Patch-Qualifier

예시:
- 1.0.0 : 첫 번째 정식 릴리즈
- 1.1.0 : 새로운 기능 추가
- 1.1.1 : 버그 수정
- 2.0.0 : Breaking Changes
- 1.2.0-SNAPSHOT : 개발 버전
- 1.2.0-RC1 : 릴리즈 후보
```

### 10.2 릴리즈 체크리스트

#### 개발 단계
- [ ] 기능 구현 완료
- [ ] 단위 테스트 작성 및 통과
- [ ] 통합 테스트 작성 및 통과
- [ ] 코드 리뷰 완료
- [ ] 문서 작성 완료

#### 릴리즈 준비
- [ ] 버전 번호 업데이트
- [ ] CHANGELOG.md 업데이트
- [ ] 의존성 최신화
- [ ] 보안 취약점 검사
- [ ] 성능 테스트 수행

#### 릴리즈 배포
- [ ] Maven Central 배포
- [ ] GitHub Release 생성
- [ ] 문서 사이트 업데이트
- [ ] 샘플 프로젝트 업데이트
- [ ] 릴리즈 노트 공지

### 10.3 Maven 배포 설정
```xml
<distributionManagement>
    <repository>
        <id>central</id>
        <name>Maven Central Repository</name>
        <url>https://oss.sonatype.org/service/local/staging/deploy/maven2</url>
    </repository>
    <snapshotRepository>
        <id>central-snapshots</id>
        <name>Maven Central Snapshot Repository</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </snapshotRepository>
</distributionManagement>
```

---

## 🚀 프레임워크 개발 프로세스

### 1. 기획 단계
1. **요구사항 분석** → 기능 명세서 작성
2. **아키텍처 설계** → 모듈 구조 및 의존성 정의
3. **API 설계** → 사용자 인터페이스 정의
4. **기술 스택 선정** → 사용 기술 및 라이브러리 선택

### 2. 개발 단계
1. **Core 모듈 개발** → 핵심 기능 구현
2. **AutoConfiguration 개발** → 자동 설정 구현
3. **Starter 모듈 개발** → 사용자 편의성 제공
4. **테스트 코드 작성** → 품질 보장

### 3. 검증 단계
1. **샘플 프로젝트 개발** → 실제 사용성 검증
2. **성능 테스트** → 성능 기준 충족 확인
3. **보안 검토** → 보안 취약점 점검
4. **사용성 테스트** → 개발자 경험 개선

### 4. 배포 단계
1. **문서 작성** → 사용자 가이드 및 API 문서
2. **버전 관리** → 릴리즈 노트 및 마이그레이션 가이드
3. **배포 및 모니터링** → 사용 현황 및 피드백 수집
4. **지속적 개선** → 사용자 피드백 반영

---

## 📚 참고 자료

- [Spring Boot Auto-Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)
- [Creating Your Own Starter](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration.custom-starter)
- [Condition Annotations](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration.condition-annotations)
- [Configuration Metadata](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html)

---

이 가이드를 활용하여 체계적이고 확장 가능한 Enterprise급 Spring Boot 프레임워크를 개발할 수 있습니다. 각 단계별 템플릿과 프롬프트를 사용하여 일관성 있는 고품질 프레임워크를 만들어보세요!