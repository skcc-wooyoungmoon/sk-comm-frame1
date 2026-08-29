package com.sk.framework.web.config;

import com.sk.framework.web.exception.GlobalExceptionHandler;
import com.sk.framework.web.filter.TraceIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * 웹 자동 설정 클래스
 *
 * <p>SK Framework 웹 기능(전역 예외 처리 등)을 자동으로 설정합니다.</p>
 *
 * <p>주의: {@code @EnableWebMvc}는 Spring Boot의 MVC 자동설정을 비활성화(springdoc/정적리소스/
 * 콘텐츠 협상 등 무력화)하는 부작용이 있어 제거했습니다. Boot 기본 MVC 자동설정 위에서
 * {@link GlobalExceptionHandler}(@RestControllerAdvice)만 빈으로 등록합니다.</p>
 *
 * @author SK Framework Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "sk.framework.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebAutoConfiguration {

    /**
     * 전역 예외 처리기 빈을 생성합니다.
     *
     * @return GlobalExceptionHandler 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * 요청별 traceId를 MDC에 세팅하는 필터. 로그 상관/분산 추적에 사용됩니다.
     */
    @Bean
    @ConditionalOnMissingBean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

}
