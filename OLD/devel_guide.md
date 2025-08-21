차세대 프레임워크 개발 통합 가이드
📜 1. 프레임워크 개발 룰 (Rules)
이 섹션은 모든 개발자가 반드시 준수해야 하는 핵심 원칙과 규칙을 정의합니다.

아키텍처 원칙
계층 분리 (Layered Architecture): 시스템은 Presentation, Business, Persistence 계층으로 명확히 구분합니다. 각 계층은 자신의 책임만 수행해야 합니다.

Presentation (Controller): HTTP 요청/응답 처리, 데이터(VO) 변환, Service 호출에만 집중합니다. 비즈니스 로직을 포함해서는 안 됩니다.

Business (Service): 트랜잭션 단위로 비즈니스 로직을 처리합니다. 데이터베이스 직접 접근(Mapper 호출)만 허용하며, 다른 Controller를 호출할 수 없습니다.

Persistence (Mapper): SQL 실행과 결과 매핑만을 담당합니다.

데이터 흐름 (Data Flow):

요청(Request): UI → Controller → Service → Mapper

응답(Response): Mapper → Service → Controller → UI

데이터 객체: Controller는 UI로부터 **VO(Value Object)**로 데이터를 받고, UI에는 ModelAndView에 List<Map<String, Object>> 또는 Map<String, Object> 형태로 결과를 담아 반환합니다.

명명 규칙 (Naming Convention)
구분

규칙

예시

URI

/{1depth}/{2depth}/{3depth}/{Controller}/{Method}.do

/com/co/coa/Auth/selectAuthList.do

Package

xxxxx.{1depth}.{2depth}.{3depth}.{layer}

xxxxx.com.co.coa.web

Controller

{업무명}Controller (PascalCase)

AuthController.java

Service

{업무명}Service (PascalCase)

AuthService.java

VO

{업무명}VO (PascalCase)

AuthVO.java

Mapper XML

dsBz-{2depth}.{3depth}.{업무명(축약)}_SQL.xml

dsBz-co.coa.Auth_SQL.xml

Method

동사+명사 (camelCase)

selectAuthList, saveAuth

SQL ID

Service 메소드명과 일치

selectAuthList, saveAuth

변수

타입 Prefix + 명사 (camelCase)

String strUserName;, int iLoopCnt;

코딩 스타일 (Coding Style)
예외 처리: try-catch는 Controller에서 처리하는 것을 원칙으로 합니다. Service에서 비즈니스 오류 발생 시, BizException을 throw하여 트랜잭션을 롤백시키고 Controller에서 일관된 오류 메시지를 처리합니다.

트랜잭션: 트랜잭션은 Service 계층의 메소드 단위로 AOP를 통해 선언적으로 관리됩니다. select*, get*으로 시작하는 메소드는 read-only로, save*, insert*, update*, delete*, multi* 등으로 시작하는 메소드는 read-write 트랜잭션으로 자동 적용됩니다.

주석: 모든 클래스와 메소드, XML 쿼리에는 지정된 템플릿에 따라 주석을 반드시 작성하여 코드의 가독성과 유지보수성을 높입니다.

SQL 작성: 모든 SQL문은 Mybatis Mapper XML 파일 내 <![CDATA[...]]> 섹션 안에 작성하며, 동적 SQL 작성을 위해 ${} 대신 #{} 사용을 원칙으로 합니다.

🤖 2. AI 코드 생성 프롬프트 (Prompts)
AI를 활용하여 표준에 맞는 코드 초안을 빠르게 생성할 수 있는 프롬프트입니다.

업무 CRUD 전체 코드 생성 프롬프트
당신은 SK C&C의 차세대 공공 시스템 개발 표준을 완벽히 이해한 시니어 개발자입니다.

다음 정보를 바탕으로 차세대 시스템 표준에 맞는 Controller, Service, VO, Mybatis Mapper XML 파일의 전체 코드를 생성해주세요.

생성 정보
업무: {업무 한글명} (예: 공지사항 관리)

주요 기능: {주요 기능} (예: 공지사항 목록 조회, 상세 조회, 등록, 수정, 삭제)

패키지 경로:

업무영역1 (1depth): {코드} (예: sm)

업무영역2 (2depth): {코드} (예: sma)

단위업무 (3depth): {코드} (예: smaa)

Controller 클래스명: {클래스명} (예: NoticeController)

URI 기본 경로: /{1depth}/{2depth}/{3depth}/{Controller명(축약)} (예: /sm/sma/smaa/Notice)

준수 규칙
Controller: @RestController 사용, Request는 VO, Response는 ModelAndView에 Map 또는 List 형태로 담아 반환. URI는 .do로 종료.

Service: @Service 사용, 공통 DsXxxxxBzMapper 주입, SQL_PREFIX 정의. 트랜잭션 규칙에 맞는 메소드명 사용.

VO: Lombok @Data 사용, 페이징을 위해 PageVO 상속, 유효성 검증 어노테이션 적용.

Mapper XML: 표준 파일명과 namespace 사용, parameterType은 VO 또는 egovMap, resultType은 egovMap. 모든 쿼리에 표준 주석 작성.

위 요구사항과 규칙을 모두 반영하여 각 파일의 전체 소스 코드를 생성해주세요.

📖 3. 개발 가이드 (Guide)
프레임워크의 구조와 개발 프로세스를 설명하는 가이드입니다.

프레임워크 아키텍처
우리 프레임워크는 Spring Boot 기반의 계층형 아키텍처를 채택하여 각 컴포넌트의 역할을 명확히 분리합니다.

Controller (in web package):

역할: 웹 요청의 진입점(Entry Point).

주요 책임:

@PostMapping, @GetMapping 등으로 URI와 메소드 매핑.

UI로부터 전달된 데이터를 @Param 어노테이션을 통해 VO로 변환.

적절한 Service 메소드 호출.

Service로부터 받은 결과(Map/List)를 ModelAndView에 담아 UI로 반환.

try-catch를 통한 최종 예외 처리 및 결과 메시지 설정.

Service (in service package):

역할: 비즈니스 로직의 핵심.

주요 책임:

@Service 어노테이션으로 비즈니스 계층임을 명시.

@Transactional 어노테이션을 통해 트랜잭션 경계 설정 (AOP로 자동 적용됨).

하나 이상의 Mapper 메소드를 조합하여 하나의 비즈니스 기능 완성.

데이터 정합성 체크, 비즈니스 규칙 적용 등 핵심 로직 수행.

오류 발생 시 BizException throw.

Mapper (XML in resources/mapper):

역할: 데이터베이스와의 통신.

주요 책임:

SQL 쿼리문 작성 및 관리.

namespace로 Service와 매핑.

id로 Service 메소드와 매핑.

동적 쿼리(<if>, <foreach>)를 사용하여 유연한 SQL 작성.

개발 프로세스
요구사항 분석: 개발할 기능의 입/출력 데이터와 비즈니스 규칙을 정의합니다.

패키지 및 클래스 생성: 표준 명명 규칙에 따라 필요한 패키지와 Controller, Service, VO 클래스 파일을 생성합니다.

AI로 초안 생성: AI 코드 생성 프롬프트를 사용하여 기본 CRUD 코드의 초안을 빠르게 생성합니다.

VO 정의: 화면 명세에 따라 VO 클래스에 필요한 멤버 변수와 유효성 검증 규칙(@NotBlank 등)을 상세히 정의합니다.

Mapper(XML) 작성: 요구사항에 맞는 SQL 쿼리문을 Mapper XML 파일에 작성합니다.

Service 로직 구현: Mapper를 호출하고, 비즈니스 규칙을 적용하여 Service 메소드를 완성합니다.

Controller 구현: Service를 호출하고, 결과를 ModelAndView에 담아 반환하도록 Controller를 구현합니다.

테스트 및 디버깅: 단위 테스트 및 통합 테스트를 통해 기능을 검증합니다.

🚀 4. 따라하기: 공지사항 관리 기능 개발 (Tutorial)
실제 공지사항 관리 기능을 개발하며 프레임워크 사용법을 익혀봅니다.

1단계: 요구사항 정의
기능: 공지사항 목록을 페이징으로 조회하고, 제목으로 검색할 수 있다.

패키지: sm/sma/smaa

클래스명: NoticeController, NoticeService, NoticeVO

테이블: TB_NOTICE (NOTICE_ID, TITLE, CONTENT, REG_DT)

2단계: AI로 코드 초안 생성하기
위 **'업무 CRUD 전체 코드 생성 프롬프트'**에 아래 정보를 채워 실행합니다.

업무 한글명: 공지사항 관리

주요 기능: 공지사항 목록 조회, 상세 조회, 등록, 수정, 삭제

1depth: sm, 2depth: sma, 3depth: smaa

Controller 클래스명: NoticeController

URI 기본 경로: /sm/sma/smaa/Notice

AI가 NoticeController.java, NoticeService.java, NoticeVO.java, dsBz-sma.smaa.Notice_SQL.xml 파일의 기본 구조를 생성해 줄 것입니다.

3단계: 코드 상세 구현하기
생성된 코드를 기반으로 실제 요구사항에 맞게 수정합니다.

1. NoticeVO.java 수정
// src/main/java/xxxxx/sm/sma/smaa/vo/NoticeVO.java
package xxxxx.sm.sma.smaa.vo;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import xxxxx.core.vo.PageVO;

@Data
public class NoticeVO extends PageVO {
    private String noticeId; // 공지 ID

    @NotBlank(message = "{msg.common.required, 제목}")
    private String title;    // 제목 (검색 조건 및 등록 시 사용)

    private String content;  // 내용
    private String regDt;    // 등록일
}

2. dsBz-sma.smaa.Notice_SQL.xml 수정
selectNoticeList 쿼리를 검색 조건과 페이징이 가능하도록 수정합니다.

<!-- src/main/resources/mapper/sm/sma/smaa/dsBz-sma.smaa.Notice_SQL.xml -->
<mapper namespace="sm.sma.smaa.notice">
    <select id="selectNoticeList" parameterType="xxxxx.sm.sma.smaa.vo.NoticeVO" resultType="egovMap">
        <![CDATA[
            /* sm.sma.smaa.notice.selectNoticeList */
            SELECT
                TB.*
            FROM (
                SELECT
                    NOTICE_ID,
                    TITLE,
                    CONTENT,
                    TO_CHAR(REG_DT, 'YYYY-MM-DD') AS REG_DT,
                    COUNT(1) OVER() AS TOT_CNT,
                    ROW_NUMBER() OVER(ORDER BY REG_DT DESC) AS ROW_NUM
                FROM TB_NOTICE
                WHERE 1=1
                <if test='title != null and title != ""'>
                    AND TITLE LIKE '%' || #{title} || '%'
                </if>
            ) TB
            WHERE TB.ROW_NUM BETWEEN #{firstIndex} AND #{lastIndex}
        ]]>
    </select>
</mapper>

3. NoticeService.java 확인
AI가 생성한 코드가 표준을 잘 따르고 있는지 확인합니다. SQL_PREFIX와 selectListForPaging 호출 부분을 중점적으로 봅니다.

// src/main/java/xxxxx/sm/sma/smaa/service/NoticeService.java
package xxxxx.sm.sma.smaa.service;

import org.springframework.stereotype.Service;
import xxxxx.sm.sma.smaa.vo.NoticeVO;
import xxxxx.core.service.BaseService;
import java.util.List;
import java.util.Map;

@Service
public class NoticeService extends BaseService {

    private static final String SQL_PREFIX = "sm.sma.smaa.notice.";

    @Override
    protected String getSqlPrefix() {
        return SQL_PREFIX;
    }

    public List<Map<String, Object>> selectNoticeList(NoticeVO noticeVO) {
        return commonMapper.selectListForPaging(SQL_PREFIX + "selectNoticeList", noticeVO);
    }
}

4. NoticeController.java 확인
마찬가지로 AI 생성 코드를 확인하고, selectNoticeList 메소드가 표준에 맞게 작성되었는지 검토합니다.

// src/main/java/xxxxx/sm/sma/smaa/web/NoticeController.java
package xxxxx.sm.sma.smaa.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import xxxxx.sm.sma.smaa.service.NoticeService;
import xxxxx.sm.sma.smaa.vo.NoticeVO;
import xxxxx.core.config.annotation.Param;
import xxxxx.core.web.BaseController;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
public class NoticeController extends BaseController {

    @Autowired
    private NoticeService noticeService;

    @PostMapping("/sm/sma/smaa/Notice/selectNoticeList.do")
    public ModelAndView selectNoticeList(@Param(name = "params") @Valid NoticeVO noticeVO) {
        ModelAndView mav = createModelAndView();
        try {
            List<Map<String, Object>> resultList = noticeService.selectNoticeList(noticeVO);
            return handleSuccess(mav, resultList);
        } catch (Exception e) {
            return handleFailure(mav, e);
        }
    }
}

이것으로 공지사항 목록 조회 기능 개발이 완료되었습니다. 이와 같은 방식으로 다른 CRUD 기능들도 표준에 따라 개발을 진행할 수 있습니다.