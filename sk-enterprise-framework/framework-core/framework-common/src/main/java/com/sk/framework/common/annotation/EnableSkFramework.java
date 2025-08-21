package com.sk.framework.common.annotation;

import com.sk.framework.common.config.SkFrameworkConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * SK Framework를 활성화하는 어노테이션
 * 
 * <p>이 어노테이션을 Spring Boot 메인 클래스에 추가하면 SK Framework의 모든 기능이 자동으로 활성화됩니다.</p>
 * 
 * <pre>
 * {@code
 * @SpringBootApplication
 * @EnableSkFramework
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * }
 * </pre>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SkFrameworkConfiguration.class)
public @interface EnableSkFramework {
    
    /**
     * 활성화할 모듈들을 지정합니다.
     * 기본값은 모든 모듈입니다.
     * 
     * @return 활성화할 모듈 배열
     */
    String[] modules() default {"security", "data", "web", "cache", "logging"};
    
    /**
     * 디버그 모드 활성화 여부
     * 
     * @return 디버그 모드 활성화 여부
     */
    boolean debug() default false;
    
}
