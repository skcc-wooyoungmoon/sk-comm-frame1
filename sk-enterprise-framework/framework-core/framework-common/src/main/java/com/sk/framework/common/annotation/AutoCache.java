package com.sk.framework.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 자동 캐시 관리를 위한 어노테이션
 * 
 * <p>이 어노테이션을 메서드에 추가하면 자동으로 캐싱이 적용됩니다.</p>
 * 
 * <pre>
 * {@code
 * @Service
 * public class UserService {
 *     
 *     @AutoCache(key = "user:{#id}", expireTime = 30, timeUnit = TimeUnit.MINUTES)
 *     public User findById(Long id) {
 *         return userRepository.findById(id);
 *     }
 * }
 * }
 * </pre>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoCache {
    
    /**
     * 캐시 키를 지정합니다.
     * SpEL(Spring Expression Language)을 사용할 수 있습니다.
     * 
     * @return 캐시 키
     */
    String key() default "";
    
    /**
     * 캐시 만료 시간을 지정합니다.
     * 
     * @return 캐시 만료 시간
     */
    long expireTime() default 60;
    
    /**
     * 캐시 만료 시간의 단위를 지정합니다.
     * 
     * @return 시간 단위
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;
    
    /**
     * 캐시 무효화 조건을 지정합니다.
     * SpEL을 사용하여 조건을 설정할 수 있습니다.
     * 
     * @return 무효화 조건
     */
    String condition() default "";
    
    /**
     * 캐시 무효화 여부
     * true인 경우 메서드 실행 후 캐시를 삭제합니다.
     * 
     * @return 캐시 무효화 여부
     */
    boolean evict() default false;
    
    /**
     * 캐시 전체 초기화 여부
     * true인 경우 관련된 모든 캐시를 삭제합니다.
     * 
     * @return 전체 초기화 여부
     */
    boolean allEntries() default false;
    
}
