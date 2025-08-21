package com.sk.framework.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * SK Framework 메인 설정 클래스
 * 
 * <p>@EnableSkFramework 어노테이션을 통해 자동으로 임포트되는 설정 클래스입니다.</p>
 * <p>프레임워크의 모든 핵심 기능을 초기화합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = "com.sk.framework")
@EnableAspectJAutoProxy
public class SkFrameworkConfiguration {
    
}
