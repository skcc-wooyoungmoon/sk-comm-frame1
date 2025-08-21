# 🚀 SK Enterprise Framework - 개발자용 Step-by-Step 개발 가이드

> **2025.08.21 최신 Rule 기반으로 완전히 업데이트된 실무 개발 가이드**  
> 이 가이드를 따라하면 Rule에 100% 준수하는 고품질 애플리케이션을 개발할 수 있습니다.

## 📋 목차

1. [시작하기 전 체크리스트](#1-시작하기-전-체크리스트)
2. [프로젝트 구조 이해](#2-프로젝트-구조-이해)
3. [Step-by-Step 개발 절차](#3-step-by-step-개발-절차)
4. [실무 예제: 공지사항 관리 기능 개발](#4-실무-예제-공지사항-관리-기능-개발)
5. [테스트 및 검증](#5-테스트-및-검증)
6. [배포 및 운영](#6-배포-및-운영)
7. [문제해결 가이드](#7-문제해결-가이드)

---

## 1. 시작하기 전 체크리스트

### ✅ 필수 확인 사항
- [ ] Java 17 이상 설치
- [ ] IDE 설정 (IntelliJ IDEA 권장)
- [ ] SK Enterprise Framework 프로젝트 클론
- [ ] 최신 Rule 문서 숙지 (2025.08.21 버전)
- [ ] 데이터베이스 연결 정보 확인

### ✅ Rule 핵심 원칙 숙지
- ❌ `.do` URI 사용 금지 → ✅ RESTful URI 사용
- ❌ `ModelAndView` 반환 금지 → ✅ `ResponseEntity<T>` 사용
- ❌ Controller `try-catch` 금지 → ✅ `GlobalExceptionHandler` 활용
- ❌ 공통 Mapper 금지 → ✅ 도메인별 Repository 구현
- ❌ `egovMap` 금지 → ✅ 명시적 DTO/VO 타입 사용

---

## 2. 프로젝트 구조 이해

### 📁 표준 패키지 구조
```
src/main/java/com/sk/framework/{domain}/
├── entity/           # JPA 엔티티
│   └── Notice.java
├── dto/              # 데이터 전송 객체
│   ├── NoticeDto.java
│   ├── NoticeCreateRequest.java
│   ├── NoticeUpdateRequest.java
│   └── NoticeSearchRequest.java
├── repository/       # 데이터 접근 계층
│   └── NoticeRepository.java
├── service/          # 비즈니스 로직 계층
│   └── NoticeService.java
└── web/              # 프레젠테이션 계층
    └── NoticeController.java
```

### 🏗️ 계층별 역할
- **Entity**: 데이터베이스 테이블과 매핑되는 JPA 엔티티
- **DTO**: 계층 간 데이터 전송을 위한 객체 (요청/응답 분리)
- **Repository**: 데이터베이스 접근을 담당 (JPA Repository)
- **Service**: 비즈니스 로직을 담당 (트랜잭션 관리)
- **Controller**: HTTP 요청/응답 처리 (RESTful API)

---

## 3. Step-by-Step 개발 절차

### 📝 Step 1: 요구사항 분석 및 설계

#### 1.1 도메인 모델링
```
업무 도메인: 공지사항 관리
주요 기능:
- 공지사항 목록 조회 (검색, 페이징)
- 공지사항 상세 조회
- 공지사항 등록
- 공지사항 수정
- 공지사항 삭제
```

#### 1.2 API 설계 (RESTful)
```
GET    /api/notices           # 목록 조회
GET    /api/notices/{id}      # 상세 조회  
POST   /api/notices           # 등록
PUT    /api/notices/{id}      # 수정
DELETE /api/notices/{id}      # 삭제
```

### 🗃️ Step 2: Entity 작성

```java
package com.sk.framework.notice.entity;

import com.sk.framework.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * @className    : Notice
 * @description  : 공지사항 엔티티
 * @modification : 2025.08.21(개발자명) 최초생성
 * @author       : 개발자명
 * @date         : 2025.08.21
 * @version      : 1.0
 */
@Entity
@Table(name = "tb_notice")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다.")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private NoticeStatus status = NoticeStatus.ACTIVE;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    public enum NoticeStatus {
        ACTIVE, INACTIVE
    }

    // 비즈니스 메서드
    public void incrementViewCount() {
        this.viewCount++;
    }

    public void activate() {
        this.status = NoticeStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = NoticeStatus.INACTIVE;
    }
}
```

### 📦 Step 3: DTO 작성 (요청/응답 분리)

#### 3.1 응답용 DTO
```java
package com.sk.framework.notice.dto;

import lombok.*;

/**
 * @className    : NoticeDto
 * @description  : 공지사항 응답 DTO
 * @modification : 2025.08.21(개발자명) 최초생성
 * @author       : 개발자명
 * @date         : 2025.08.21
 * @version      : 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeDto {
    
    private Long id;
    private String title;
    private String content;
    private String status;
    private Long viewCount;
    private String createdAt;
    private String updatedAt;
    private String createdBy;
}
```

#### 3.2 요청용 DTO들
```java
// 생성 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeCreateRequest {
    
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다.")
    private String title;
    
    private String content;
}

// 수정 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeUpdateRequest {
    
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다.")
    private String title;
    
    private String content;
}

// 검색 조건 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeSearchRequest {
    
    private String title;
    private String content;
    private String status;
    private String dateFrom;
    private String dateTo;
}
```

### 🗄️ Step 4: Repository 구현 (도메인별)

```java
package com.sk.framework.notice.repository;

import com.sk.framework.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @className    : NoticeRepository
 * @description  : 공지사항 Repository - Rule에 따른 도메인별 Repository
 * @modification : 2025.08.21(개발자명) 최초생성
 * @author       : 개발자명
 * @date         : 2025.08.21
 * @version      : 1.0
 */
@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * 제목으로 검색
     */
    List<Notice> findByTitleContaining(String title);

    /**
     * 상태별 조회
     */
    List<Notice> findByStatus(Notice.NoticeStatus status);

    /**
     * 복합 검색 조건
     */
    @Query("SELECT n FROM Notice n WHERE " +
           "(:title IS NULL OR n.title LIKE %:title%) AND " +
           "(:content IS NULL OR n.content LIKE %:content%) AND " +
           "(:status IS NULL OR n.status = :status) AND " +
           "(:dateFrom IS NULL OR n.createdAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR n.createdAt <= :dateTo)")
    Page<Notice> findBySearchConditions(@Param("title") String title,
                                       @Param("content") String content,
                                       @Param("status") Notice.NoticeStatus status,
                                       @Param("dateFrom") LocalDateTime dateFrom,
                                       @Param("dateTo") LocalDateTime dateTo,
                                       Pageable pageable);
}
```

### 🔧 Step 5: Service 구현 (트랜잭션 Rule 적용)

```java
package com.sk.framework.notice.service;

import com.sk.framework.common.service.BaseService;
import com.sk.framework.notice.dto.*;
import com.sk.framework.notice.entity.Notice;
import com.sk.framework.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @className    : NoticeService
 * @description  : 공지사항 서비스 - Rule에 따른 트랜잭션 정책 적용
 * @modification : 2025.08.21(개발자명) 최초생성
 * @author       : 개발자명
 * @date         : 2025.08.21
 * @version      : 1.0
 */
@Service
@Transactional(readOnly = true)  // Rule: 클래스 레벨 readOnly=true
@RequiredArgsConstructor
public class NoticeService extends BaseService<Notice, Long> {

    private final NoticeRepository noticeRepository;

    @Override
    protected JpaRepository<Notice, Long> getRepository() {
        return noticeRepository;
    }

    @Override
    protected String getEntityName() {
        return "Notice";
    }

    /**
     * 검색 조건에 따른 공지사항 목록 조회
     */
    public Page<NoticeDto> findNotices(NoticeSearchRequest searchRequest, Pageable pageable) {
        
        Notice.NoticeStatus status = searchRequest.getStatus() != null ? 
            Notice.NoticeStatus.valueOf(searchRequest.getStatus()) : null;
            
        LocalDateTime dateFrom = parseDateTime(searchRequest.getDateFrom());
        LocalDateTime dateTo = parseDateTime(searchRequest.getDateTo());
        
        Page<Notice> noticePage = noticeRepository.findBySearchConditions(
            searchRequest.getTitle(),
            searchRequest.getContent(),
            status,
            dateFrom,
            dateTo,
            pageable
        );
        
        return noticePage.map(this::convertToDto);
    }

    /**
     * 공지사항 상세 조회 (조회수 증가)
     * Rule: 쓰기 메서드는 @Transactional로 override
     */
    @Transactional
    public NoticeDto findNoticeById(Long id) {
        Notice notice = findById(id);
        notice.incrementViewCount();  // 조회수 증가
        noticeRepository.save(notice);
        return convertToDto(notice);
    }

    /**
     * 공지사항 생성
     * Rule: 쓰기 메서드는 @Transactional로 override
     */
    @Transactional
    public NoticeDto createNotice(NoticeCreateRequest request) {
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .status(Notice.NoticeStatus.ACTIVE)
                .viewCount(0L)
                .build();

        Notice savedNotice = noticeRepository.save(notice);
        return convertToDto(savedNotice);
    }

    /**
     * 공지사항 수정
     * Rule: 쓰기 메서드는 @Transactional로 override
     */
    @Transactional
    public NoticeDto updateNotice(Long id, NoticeUpdateRequest request) {
        Notice existingNotice = findById(id);

        Notice.NoticeBuilder builder = existingNotice.toBuilder();
        
        if (request.getTitle() != null) {
            builder.title(request.getTitle());
        }
        if (request.getContent() != null) {
            builder.content(request.getContent());
        }

        Notice updatedNotice = noticeRepository.save(builder.build());
        return convertToDto(updatedNotice);
    }

    /**
     * 공지사항 상태 변경
     * Rule: 쓰기 메서드는 @Transactional로 override
     */
    @Transactional
    public void changeNoticeStatus(Long id, String status) {
        Notice notice = findById(id);
        Notice.NoticeStatus newStatus = Notice.NoticeStatus.valueOf(status.toUpperCase());
        
        if (newStatus == Notice.NoticeStatus.ACTIVE) {
            notice.activate();
        } else {
            notice.deactivate();
        }
        
        noticeRepository.save(notice);
    }

    /**
     * Entity를 DTO로 변환
     */
    private NoticeDto convertToDto(Notice notice) {
        return NoticeDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .status(notice.getStatus().name())
                .viewCount(notice.getViewCount())
                .createdAt(formatDateTime(notice.getCreatedAt()))
                .updatedAt(formatDateTime(notice.getUpdatedAt()))
                .createdBy(notice.getCreatedBy())
                .build();
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
```

### 🌐 Step 6: Controller 구현 (RESTful + Rule 준수)

```java
package com.sk.framework.notice.web;

import com.sk.framework.common.dto.ApiResponse;
import com.sk.framework.common.dto.PageResponse;
import com.sk.framework.notice.dto.*;
import com.sk.framework.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @className    : NoticeController
 * @description  : 공지사항 관리 컨트롤러 - Rule 준수 RESTful API
 * @modification : 2025.08.21(개발자명) 최초생성
 * @author       : 개발자명
 * @date         : 2025.08.21
 * @version      : 1.0
 */
@Tag(name = "공지사항 관리", description = "공지사항 CRUD API")
@RestController
@RequestMapping("/api/notices")  // Rule: RESTful URI, .do 미사용
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 공지사항 목록 조회
     * Rule: ResponseEntity + ApiResponse + PageResponse 사용
     */
    @Operation(summary = "공지사항 목록 조회", description = "검색 조건과 페이징을 통해 공지사항 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NoticeDto>>> getNotices(
            @Parameter(description = "검색 조건") NoticeSearchRequest searchRequest,
            @Parameter(description = "페이징 정보") @PageableDefault(size = 20) Pageable pageable) {
        
        Page<NoticeDto> noticePage = noticeService.findNotices(searchRequest, pageable);
        PageResponse<NoticeDto> pageResponse = PageResponse.of(noticePage);
        
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * 공지사항 상세 조회
     */
    @Operation(summary = "공지사항 상세 조회", description = "ID로 특정 공지사항의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeDto>> getNotice(
            @Parameter(description = "공지사항 ID") @PathVariable Long id) {
        
        NoticeDto notice = noticeService.findNoticeById(id);
        return ResponseEntity.ok(ApiResponse.success(notice));
    }

    /**
     * 공지사항 생성
     * Rule: @Valid Bean Validation 적용
     */
    @Operation(summary = "공지사항 생성", description = "새로운 공지사항을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<NoticeDto>> createNotice(
            @Parameter(description = "공지사항 생성 정보") @Valid @RequestBody NoticeCreateRequest request) {
        
        NoticeDto createdNotice = noticeService.createNotice(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("공지사항이 성공적으로 생성되었습니다.", createdNotice));
    }

    /**
     * 공지사항 수정
     */
    @Operation(summary = "공지사항 수정", description = "기존 공지사항의 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeDto>> updateNotice(
            @Parameter(description = "공지사항 ID") @PathVariable Long id,
            @Parameter(description = "공지사항 수정 정보") @Valid @RequestBody NoticeUpdateRequest request) {
        
        NoticeDto updatedNotice = noticeService.updateNotice(id, request);
        return ResponseEntity.ok(ApiResponse.success("공지사항이 성공적으로 수정되었습니다.", updatedNotice));
    }

    /**
     * 공지사항 삭제
     */
    @Operation(summary = "공지사항 삭제", description = "특정 공지사항을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(
            @Parameter(description = "공지사항 ID") @PathVariable Long id) {
        
        noticeService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("공지사항이 성공적으로 삭제되었습니다."));
    }

    /**
     * 공지사항 상태 변경
     */
    @Operation(summary = "공지사항 상태 변경", description = "공지사항의 활성화 상태를 변경합니다.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> changeNoticeStatus(
            @Parameter(description = "공지사항 ID") @PathVariable Long id,
            @Parameter(description = "변경할 상태") @RequestParam String status) {
        
        noticeService.changeNoticeStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("공지사항 상태가 성공적으로 변경되었습니다."));
    }
}
```

---

## 4. 실무 예제: 공지사항 관리 기능 개발

위의 Step 1-6을 통해 완성된 공지사항 관리 기능은 다음과 같은 특징을 가집니다:

### ✅ Rule 준수 현황
- ✅ RESTful URI 사용 (`/api/notices`)
- ✅ ResponseEntity 반환
- ✅ ApiResponse/PageResponse 래퍼 적용
- ✅ @Valid Bean Validation 적용
- ✅ 요청/응답 DTO 분리
- ✅ 도메인별 Repository 구현
- ✅ @Transactional 명시적 선언
- ✅ OpenAPI 문서화 적용
- ✅ 예외는 GlobalExceptionHandler에서 처리

### 🔄 API 테스트 예시

#### 목록 조회
```bash
GET /api/notices?title=공지&page=0&size=10
```

#### 상세 조회
```bash
GET /api/notices/1
```

#### 생성
```bash
POST /api/notices
Content-Type: application/json

{
  "title": "새로운 공지사항",
  "content": "공지사항 내용입니다."
}
```

---

## 5. 테스트 및 검증

### 🧪 Unit Test 작성
```java
@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @InjectMocks
    private NoticeService noticeService;

    @Test
    @DisplayName("공지사항 생성 테스트")
    void createNotice() {
        // Given
        NoticeCreateRequest request = NoticeCreateRequest.builder()
                .title("테스트 공지")
                .content("테스트 내용")
                .build();

        Notice notice = Notice.builder()
                .id(1L)
                .title("테스트 공지")
                .content("테스트 내용")
                .status(Notice.NoticeStatus.ACTIVE)
                .build();

        when(noticeRepository.save(any(Notice.class))).thenReturn(notice);

        // When
        NoticeDto result = noticeService.createNotice(request);

        // Then
        assertThat(result.getTitle()).isEqualTo("테스트 공지");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }
}
```

### 🌐 Integration Test 작성
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NoticeControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("공지사항 목록 조회 통합 테스트")
    void getNotices() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/notices", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

---

## 6. 배포 및 운영

### 📊 모니터링 설정
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### 📝 로깅 설정
```yaml
logging:
  level:
    com.sk.framework: DEBUG
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

---

## 7. 문제해결 가이드

### ❌ 자주 발생하는 실수들

#### 1. URI에 .do 사용
```java
// ❌ 잘못된 예시
@RequestMapping("/notices.do")

// ✅ 올바른 예시  
@RequestMapping("/api/notices")
```

#### 2. ModelAndView 반환
```java
// ❌ 잘못된 예시
public ModelAndView getNotices() {
    return new ModelAndView("notices");
}

// ✅ 올바른 예시
public ResponseEntity<ApiResponse<PageResponse<NoticeDto>>> getNotices() {
    return ResponseEntity.ok(ApiResponse.success(pageResponse));
}
```

#### 3. Controller에서 try-catch 사용
```java
// ❌ 잘못된 예시
@PostMapping
public ResponseEntity<?> create(@RequestBody NoticeCreateRequest request) {
    try {
        return ResponseEntity.ok(noticeService.create(request));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("오류 발생");
    }
}

// ✅ 올바른 예시 - GlobalExceptionHandler가 처리
@PostMapping
public ResponseEntity<ApiResponse<NoticeDto>> create(@Valid @RequestBody NoticeCreateRequest request) {
    NoticeDto notice = noticeService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(notice));
}
```

### 🔧 디버깅 팁
1. **API 응답 확인**: Swagger UI 또는 Postman 활용
2. **로그 확인**: 애플리케이션 로그에서 오류 추적
3. **DB 상태 확인**: H2 Console 또는 DB 툴 활용
4. **트랜잭션 확인**: @Transactional 적용 여부 점검

---

## 📚 추가 학습 자료

### 공식 문서
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Bean Validation Specification](https://beanvalidation.org/2.0/spec/)

### 프레임워크 내부 문서
- `framework-docs/updated-development-guide.md`
- `sk-framework_rule.md`
- `framework-creation-rules.md`

---

## 🎯 요약

이 가이드를 따라 개발하면:

1. ✅ **Rule 100% 준수**: 최신 Spring Boot 엔터프라이즈 표준 적용
2. ✅ **고품질 코드**: 테스트 가능하고 유지보수하기 쉬운 코드
3. ✅ **표준화**: 팀 내 일관된 개발 스타일 확보
4. ✅ **생산성 향상**: 반복 작업 최소화 및 재사용 가능한 컴포넌트 활용

**💡 개발할 때마다 이 가이드를 참조하여 Rule에 맞는 개발을 진행하세요!**

---

**📞 문의사항**: 프레임워크팀 (framework-team@company.com)  
**📅 최종 업데이트**: 2025.08.21  
**📌 버전**: 2.0
