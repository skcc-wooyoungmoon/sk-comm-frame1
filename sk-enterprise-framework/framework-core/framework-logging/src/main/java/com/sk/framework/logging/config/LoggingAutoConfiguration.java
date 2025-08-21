package com.sk.framework.logging.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * 로깅 자동 설정 클래스
 * 
 * <p>SK Framework 로깅 기능을 자동으로 설정합니다.</p>
 * <p>구조화된 로깅과 성능 모니터링을 구성합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "sk.framework.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.sk.framework.logging")
public class LoggingAutoConfiguration {
    
}
