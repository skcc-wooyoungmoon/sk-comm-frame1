# 🏗️ 차세대 프레임워크 생성 룰

이 문서는 차세대 시스템 구축을 위한 프레임워크 개발 시 준수해야 할 최신 Spring Boot 관례와 품질 속성 강화 지침을 정의합니다.

## [2025-08-21 업데이트] Spring Boot 관례 및 품질 속성 강화 지침

본 문서는 최신 Spring Boot 엔터프라이즈 표준에 따라 다음 정책을 반드시 적용합니다. 기존 내용과 상충 시 본 업데이트를 우선 적용합니다.

- API 스타일: .do URI 접미사 완전 폐지. RESTful URI와 HTTP Status 표준화. Controller는 ResponseEntity/DTO 반환, ApiResponse 래퍼 적용.
- 예외 처리: Controller 개별 try-catch 금지. @ControllerAdvice(GlobalExceptionHandler)로 일괄 처리. 표준 오류 응답 스키마 및 국제화 메시지 사용.
- 트랜잭션: 메소드명 접두사 기반 규칙 폐지. @Transactional 명시적 선언(클래스 readOnly=true, 쓰기 메소드 override).
- 데이터 접근: 공통 Mapper(god object) 패턴 폐지, 도메인별 Mapper/Repository 사용. MyBatis resultType은 egovMap 대신 명시적 타입(DTO/VO). JPA 선택 가능.
- 검증: @Valid/@Validated + Bean Validation. 요청 DTO와 응답 DTO 분리.
- 패키징: 도메인 기반 패키지 우선. URI depth는 권한/라우팅에만 활용.
- 설정/성능: HikariCP 튜닝, 캐시 추상화(@AutoCache), 프로파일(application-*.yml), Actuator/Micrometer/Prometheus, JSON 로그, 트레이스ID(MDC), 민감정보 마스킹.
- 문서화: OpenAPI(Swagger) 자동화, @Operation/@Schema 사용.

---

## 📋 목차

1. **프레임워크 아키텍처 설계 원칙**
2. **코드 구조 및 패키지 설계**
3. **Base 클���스 및 공통 컴포넌트**
4. **데이터베이스 연동 규칙**
5. **외부 연동 및 API 설계**
6. **설정 및 환경 관리**
7. **문서화 및 품질 기준**

---

## 1. 프레임워크 아키텍처 설계 원칙

### 계층 구조

- Presentation Layer: Controller (@RestController, ResponseEntity)
- Business Layer: Service (@Service, @Transactional)
- Persistence Layer: Mapper/Repository (MyBatis/JPA)
- Infrastructure Layer: DB, 외부 API, 배치 등

#### 계층간 의존성
- Controller → Service → Mapper/Repository
- 각 계층별 VO/DTO로 데이터 전달

---

## 2. 코드 구조 및 패키지 설계

### 패키지 구조 예시

src/main/java/com/sk/framework/{domain}/
- web/ (Controller)
- service/ (Service)
- dto/ (DTO/VO)
- mapper/ (MyBatis Mapper)

### 네이밍 규칙
- URI: RESTful, .do 미사용 (예: /sm/sma/smaa/notices)
- Controller: {Domain}Controller
- Service: {Domain}Service
- DTO/VO: {Domain}Dto, {Domain}Search, {Domain}Create 등
- Mapper: {Domain}Mapper

---

## 3. Base 클래스 및 공통 컴포넌트

### BaseController
- ResponseEntity 기반 표준 응답(ApiResponse, PageResponse) 헬퍼 제공
- ModelAndView 사용 지양

### BaseService
- 도메인별 Mapper/Repository 주입
- 공통 트랜잭션 정책만 공통화

### 예외 처리
- @ControllerAdvice에서 BizException, SystemException 등 일괄 처리

---

## 4. 데이터베이스 연동 규칙

### MyBatis Mapper
- 파일명: dsBz-{2depth}.{3depth}.{업무명}_SQL.xml
- namespace: {업무명}
- parameterType/resultType: VO/DTO 타입 명시
- 표준 주석 필수

### 트랜잭션
- 클래스 @Transactional(readOnly = true), 쓰기 메소드만 @Transactional override
- 전파/격리 수준 명시

---

## 5. 외부 연동 및 API 설계

### API Client
- 단일 책임 원칙, Service에서 주입받아 사용
- 장애 대응: Circuit Breaker, Retry, Timeout 등 적용
- 에러 핸들링 및 Fallback 전략

---

## 6. 설정 및 환경 관리

### application.yml
- 프로파일별 분리(local/dev/prod)
- HikariCP, 캐시, 외부 API, 보안 등 환경별 설정
- JSON 로그, 트레이스ID(MDC), 민감정보 마스킹

---

## 7. 문서화 및 품질 기준

### OpenAPI(Swagger)
- @Operation, @Schema 사용, 자동 문서화

### 체크리스트
- [ ] 계층형 아키텍처 설계
- [ ] 도메인별 패키지/모듈 분리
- [ ] ResponseEntity 기반 Controller 구현
- [ ] DTO/VO 표준 정의 및 검증
- [ ] 도메인별 Mapper/Repository 구현
- [ ] 트랜잭션 정책 적용
- [ ] API Client 표준 구현
- [ ] 환경별 설정 분리
- [ ] 로깅/관측성/보안 정책 적용
- [ ] API 문서/샘플 코드/주석 템플릿 최신화

### 품질 기준
- 테스트 커버리지 80% 이상
- 순환 복잡도 10 이하
- 코드 중복도 5% 이하
- 정적 분석 Critical 이슈 0개
- API 응답 평균 200ms 이하
- OWASP Top 10 방어, 인증/인가 구현

---

## 샘플 가이드 (최신)

### Controller 예시
```java
@RestController
@RequestMapping("/sm/sma/smaa/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<PageResponse<NoticeDto>> list(@Valid NoticeSearch search) {
        return ResponseEntity.ok(noticeService.findAll(search));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody NoticeCreate req) {
        noticeService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }
}
```

### Service 예시
```java
@Service
@Transactional(readOnly = true)
public class NoticeService {
    private final NoticeRepository noticeRepository;

    public PageResponse<NoticeDto> findAll(NoticeSearch search) {
        // 비즈니스 로직 및 페이징 처리
    }

    @Transactional
    public void create(NoticeCreate req) {
        // 등록 로직
    }
}
```

### VO/DTO 예시
```java
@Data
public class NoticeCreate {
    @NotBlank(message = "{msg.common.required, 제목}")
    private String title;
    private String content;
}
```

### Mapper 예시
```xml
<select id="selectNoticeList" parameterType="NoticeSearch" resultType="NoticeDto">
    <![CDATA[
        SELECT NOTICE_ID, TITLE, CONTENT, REG_DT
        FROM TB_NOTICE
        WHERE 1=1
        <if test='title != null and title != ""'>
            AND TITLE LIKE '%' || #{title} || '%'
        </if>
        ORDER BY REG_DT DESC
    ]]>
</select>
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
 * @className    : NoticeController
 * @description  : 공지사항 관리 컨트롤러
 * @modification : 2025.08.21(홍길동) 최초생성
 * @author       : 홍길동
 * @date         : 2025.08.21
 * @version      : 1.0
 */
```

### 표준 주석 템플릿 (Mapper XML)
```xml
<!--
    쿼리명      : "selectNoticeList" (공지사항 목록 조회)
    설명        : 공지사항 목록을 페이징하여 조회합니다.
    parameterType: com.sk.framework.sm.sma.smaa.dto.NoticeSearch
    resultType   : NoticeDto
    수정일         수정자        수정내용
    ===========   ========    ===========================
    2025.08.21    홍길동        최초 생성
-->
```

---

이 문서는 차세대 프레임워크 개발 시 반드시 준수해야 할 최신 엔터프라이즈 규칙을 정의합니다.
모든 개발자는 이 규칙을 숙지하고 일관된 품질의 코드를 작성해야 합니다.

**📞 문의사항**: 프레임워크팀 (framework-team@company.com)
**📅 최종 업데이트**: 2025.08.21
**📌 버전**: 2.0
