package com.sk.framework.web.config;

import com.sk.framework.web.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * 웹 자동 설정 클래스
 * 
 * <p>SK Framework 웹 기능을 자동으로 설정합니다.</p>
 * <p>전역 예외 처리, API 문서화 등을 구성합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "sk.framework.web", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableWebMvc
@ComponentScan(basePackages = "com.sk.framework.web")
public class WebAutoConfiguration {
    
    /**
     * 전역 예외 처리기 빈을 생성합니다.
     * 
     * @return GlobalExceptionHandler 인스턴스
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
    
}
