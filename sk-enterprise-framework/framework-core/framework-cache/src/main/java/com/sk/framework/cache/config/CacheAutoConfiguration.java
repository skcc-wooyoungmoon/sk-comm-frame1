package com.sk.framework.cache.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

/**
 * 캐시 자동 설정 클래스
 * 
 * <p>SK Framework 캐시 기능을 자동으로 설정합니다.</p>
 * <p>Redis 기반 캐싱과 어노테이션 기반 캐시 관리를 구성합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "sk.framework.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableCaching
@ComponentScan(basePackages = "com.sk.framework.cache")
public class CacheAutoConfiguration {
    
}
