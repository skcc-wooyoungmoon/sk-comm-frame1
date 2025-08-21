📜 차세대 시스템 프레임워크 개발 룰 & 프롬프트 가이드 (개정판)
이 문서는 첨부된 개발 표준을 기반으로 재구성한 프레임워크 개발의 핵심 원칙과 자동 생성을 위한 프롬프트를 정의합니다.

1. 프레임워크 아키텍처 설계
1.1 계층형 아키텍처
표준 샘플.pdf의 아키텍처를 기반으로 SOLID 원칙을 적용하여 역할을 명확히 구분합니다.

┌──────────────────────────────────────────────────┐
│           Presentation Layer (표현 계층)           │  ← API, Web Interface (Controller, VO)
├──────────────────────────────────────────────────┤
│           Business Layer (비즈니스 계층)           │  ← Business Logic (Service)
├──────────────────────────────────────────────────┤
│           Persistence Layer (영속성 계층)          │  ← Data Access Logic (Mapper Interface)
├──────────────────────────────────────────────────┤
│           Infrastructure Layer (인프라 계층)         │  ← DB (Mybatis SQL XML), External APIs
└──────────────────────────────────────────────────┘
1.2 모듈형 구조
프레임워크 자체는 재사용성과 확장성을 위해 기능 단위 모듈로 구성합니다.

Framework Core
├── config-module          # 자동 설정, 프로퍼티
├── security-module        # 인증, 인가
├── data-access-module     # Mybatis 기반 공통 데이터 처리
├── validation-module      # 공통 유효성 검증
├── logging-module         # 표준 로깅
├── web-module             # 웹 공통 (Interceptor, Filter)
└── util-module            # 공통 유틸리티
2. 폴더 구조 표준
2.1 프레임워크 프로젝트 구조
프레임워크는 멀티 모듈 프로젝트로 관리하여 의존성을 명확히 합니다.

koneps-framework/
├── framework-core/              # 핵심 프레임워크 로직
├── framework-starters/          # Spring Boot Starters
│   ├── framework-starter-web/
│   └── framework-starter-mybatis/
├── framework-samples/           # 샘플 애플리케이션
├── framework-docs/              # 문서
└── pom.xml                      # 루트 POM
2.2 Core 모듈 구조 (framework-core)
개발 샘플 가이드.pdf의 com(Core) 구조를 반영하여 체계적으로 구성합니다.

framework-core/
└── src/main/java/
    └── xxxxx/core/
        ├── config/                # 자동 설정
        │   ├── annotation/      # @Auth 등 커스텀 어노테이션
        │   ├── aspect/          # 로깅, 트랜잭션 AOP
        │   ├── datasource/      # DataSource 설정
        │   ├── interceptor/     # 세션 체크 등 인터셉터
        │   └── mvc/             # WebMvcConfig
        ├── constants/             # 공통 상수
        ├── exception/             # 공통 예외 클래스
        ├── mapper/                # BaseMapper 등 공통 매퍼
        ├── service/               # Redis, Rest 등 공통 서비스
        ├── util/                  # StringUtil, FileUtil 등
        └── vo/                    # PageVO, UserSessionVO 등 공통 VO
2.3 애플리케이션 프로젝트 구조
프레임워크를 사용하는 실제 업무 애플리케이션의 폴더 구조 표준입니다.

xxxxx-api-project/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── xxxxx/
    │   │       ├── com/co/coa/      # {업무영역1}/{업무영역2}/{단위업무}
    │   │       │   ├── web/       # AuthController.java
    │   │       │   ├── service/   # AuthService.java
    │   │       │   └── vo/        # AuthVO.java
    │   │       └── fc/fcd/fcdb/
    │   │           └── ...
    │   └── resources/
    │       ├── mapper/
    │       │   └── com/co/coa/      # {업무영역1}/{업무영역2}/{단위업무}
    │       │       └── Auth_SQL.xml # SQL 매퍼 XML
    │       ├── application.yml
    │       └── messages.properties
    └── test/
3. 핵심 기능 모듈 (Mybatis 기반)
3.1 Base 클래스
JPA 대신 Mybatis 기반의 공통 로직을 제공하여 생산성을 높입니다.

BaseMapper (Interface)

Java

public interface BaseMapper<V, R> {
    R selectOne(V vo);
    List<R> selectList(V vo);
    int insert(V vo);
    int update(V vo);
    int delete(V vo);
}
BaseService

Java

public abstract class BaseService {
    
    // 공통 Mapper는 @Autowired로 주입받아 사용
    @Autowired
    protected DsXxxxxBzMapper commonMapper;

    // 각 서비스는 자신의 SQL_PREFIX를 정의하여 사용
    protected abstract String getSqlPrefix();

    public Map<String, Object> selectOne(String queryId, Object parameter) {
        return commonMapper.selectOne(getSqlPrefix() + queryId, parameter);
    }
    
    public List<Map<String, Object>> selectList(String queryId, Object parameter) {
        return commonMapper.selectList(getSqlPrefix() + queryId, parameter);
    }
    
    // insert, update, delete 등 공통 메소드 구현
}
BaseController
표준 샘플.pdf의 "Request는 VO, Response는 Map" 규칙을 적용합니다.

Java

@RestController
public abstract class BaseController {

    protected ModelAndView createModelAndView() {
        return new ModelAndView();
    }
    
    // 성공 시 공통 처리
    protected ModelAndView handleSuccess(ModelAndView mav, Object result) {
        mav.addObject("result", result);
        mav.addObject("statCd", "0"); // 0: 정상
        mav.addObject("msg", MessageUtils.getMessage("msg.common.success"));
        return mav;
    }

    // 실패 시 공통 처리
    protected ModelAndView handleFailure(ModelAndView mav, Exception e) {
        mav.addObject("statCd", "-1"); // -1: 비정상
        if (e instanceof BizException) {
            mav.addObject("msg", MessageUtils.getMessage(((BizException) e).getMessageKey()));
        } else {
            mav.addObject("msg", MessageUtils.getMessage("error.common.error"));
        }
        return mav;
    }
}
4. 개발 룰 & 컨벤션
4.1 명명 규칙 (표준 샘플.pdf 기반)
URI: ./{업무영역1}/{업무영역2}/{단위업무}/{Controller명(축약)}/{메소드명}.do

예: /fc/fcd/fcdb/Auth/selectAuthList.do

Package: xxxxx.{업무영역1}.{업무영역2}.{단위업무}.{layer}

예: xxxxx.com.co.coa.web

Class:

Controller: {업무명}Controller (PascalCase) -> AuthController

Service: {업무명}Service (PascalCase) -> AuthService

VO: {업무명}VO (PascalCase) -> AuthVO

Mapper (Java): Ds{데이터소스}{프로젝트}BzMapper -> DsXxxxxBzMapper

Mapper (XML): dsBz-{업무영역2}.{단위업무}.{업무명(축약)}_SQL.xml -> dsBz-co.coa.Auth_SQL.xml

Method: 동사+명사 (camelCase) -> selectAuthList, saveAuth

SQL ID: select{업무명}List (Service 메소드명과 일치 권장)

4.2 코드 작성 룰
단일 책임 원칙 (SRP):

Controller: HTTP 요청/응답 처리, 데이터 변환, Service 호출 담당. 비즈니스 로직 구현 금지.

Service: 트랜잭션 단위의 비즈니스 로직 처리. 타 Service 또는 Mapper 호출.

Mapper: SQL 실행 및 결과 매핑.

의존성 주입: @Autowired를 사용한 생성자 또는 필드 주입을 사용한다.

설정 외부화: DB 접속 정보, 파일 경로 등은 application.yml 파일로 외부화하여 관리한다.

예외 처리: try-catch문은 Controller 계층에서 처리하는 것을 원칙으로 한다. Service 계층에서는 비즈니스 예외 발생 시 BizException을 throw하여 트랜잭션 롤백을 유도한다.

5. 프레임워크 생성 프롬프트
5.1 Mybatis 기반 모듈 생성 프롬프트
당신은 SK C&C의 차세대 공공 시스템 프레임워크 개발 전문가입니다.

다음 요구사항에 맞는 Mybatis 기반의 Spring Boot 프레임워크 모듈을 생성해주세요.

기본 정보
모듈명: {MODULE_NAME} (예: DataAccess)

기능: {FEATURE_DESCRIPTION} (예: Mybatis 기반 공통 데이터 접근 처리)

핵심 의존성: mybatis-spring-boot-starter

생성 규칙
xxxxx.core.{module} 패키지 구조를 따릅니다.

AutoConfiguration, Properties, Base 클래스 (필요시)를 포함하여 생성합니다.

명명 규칙을 준수합니다: {Module}AutoConfiguration, {Module}Properties.

BaseMapper 인터페이스와 공통 DsXxxxxBzMapper 클래스를 활용하는 구조로 설계합니다.

추가 요구사항
{ADDITIONAL_REQUIREMENTS}

위 규칙에 따라 완전한 프레임워크 모듈 코드를 생성해주세요.

5.2 업무 CRUD 자동 생성 프롬프트
당신은 표준 샘플.pdf 개발 표준을 완벽히 이해한 프레임워크 개발자입니다.

다음 정보를 바탕으로 차세대 시스템 표준에 맞는 Controller, Service, VO, Mybatis Mapper XML 파일의 전체 코드를 생성해주세요.

생성 정보
업무: 권한 관리 (Authentication)

주요 기능: 권한 목록 조회, 단건 조회, 저장(등록/수정), 삭제

패키지 경로:

업무영역1: com (공통)

업무영역2: co (공통관리)

단위업무: coa (공통권한관리)

Controller 클래스명: AuthController

URI 기본 경로: /com/co/coa/Auth

준수 규칙
Controller:

@RestController 사용

Request는 VO(AuthVO)로, Response는 ModelAndView에 Map 또는 List 형태로 담아서 반환

URI는 .do로 끝나야 함 (예: /selectAuthList.do)

Service:

@Service 사용

공통 DsXxxxxBzMapper를 주입받아 사용

SQL_PREFIX (예: co.coa.auth.)를 정의하여 SQL ID 호출

트랜잭션 처리를 위해 메소드명은 select*, save*, delete* 규칙 준수

VO:

@Data (Lombok) 사용

페이징을 위해 PageVO 상속

유효성 검증을 위해 @NotBlank 등 어노테이션 사용

Mapper XML:

파일명: dsBz-co.coa.Auth_SQL.xml

namespace: co.coa.auth

parameterType: xxxxx.com.co.coa.vo.AuthVO 또는 egovMap

resultType: egovMap

모든 쿼리는 <![CDATA[...]]> 안에 작성

위 요구사항과 규칙을 모두 반영하여 각 파일의 전체 소스 코드를 생성해주세요.