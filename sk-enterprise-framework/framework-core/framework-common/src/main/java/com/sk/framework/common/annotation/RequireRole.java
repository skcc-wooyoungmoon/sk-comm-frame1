package com.sk.framework.common.annotation;

import java.lang.annotation.*;

/**
 * 역할 기반 접근 제어를 위한 어노테이션
 * 
 * <p>이 어노테이션을 메서드나 클래스에 추가하면 지정된 역할을 가진 사용자만 접근할 수 있습니다.</p>
 * 
 * <pre>
 * {@code
 * @RestController
 * @RequireRole("ADMIN")
 * public class AdminController {
 *     
 *     @GetMapping("/users")
 *     @RequireRole(value = "SUPER_ADMIN", operation = "AND")
 *     public List<User> getUsers() {
 *         return userService.findAll();
 *     }
 * }
 * }
 * </pre>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    
    /**
     * 필요한 역할을 지정합니다.
     * 여러 역할을 지정할 수 있습니다.
     * 
     * @return 필요한 역할 배열
     */
    String[] value();
    
    /**
     * 역할 검사 연산자를 지정합니다.
     * AND: 모든 역할을 가져야 함
     * OR: 하나 이상의 역할을 가져야 함
     * 
     * @return 연산자
     */
    LogicalOperator operation() default LogicalOperator.OR;
    
    /**
     * 접근 거부 시 리다이렉트할 URL을 지정합니다.
     * 
     * @return 리다이렉트 URL
     */
    String redirectUrl() default "";
    
    /**
     * 접근 거부 시 반환할 에러 메시지를 지정합니다.
     * 
     * @return 에러 메시지
     */
    String errorMessage() default "Access denied: insufficient privileges";
    
    /**
     * 논리 연산자
     */
    enum LogicalOperator {
        AND, OR
    }
    
}
