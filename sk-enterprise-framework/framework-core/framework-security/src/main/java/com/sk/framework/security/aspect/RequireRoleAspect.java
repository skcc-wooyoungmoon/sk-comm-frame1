package com.sk.framework.security.aspect;

import com.sk.framework.common.annotation.RequireRole;
import com.sk.framework.security.exception.InsufficientPrivilegesException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @RequireRole 어노테이션 처리 Aspect
 * 
 * <p>메서드나 클래스에 @RequireRole 어노테이션이 적용된 경우 권한을 검사합니다.</p>
 * <p>AOP를 사용하여 비즈니스 로직과 보안 로직을 분리합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@Aspect
@Component
public class RequireRoleAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(RequireRoleAspect.class);
    
    /**
     * @RequireRole 어노테이션이 적용된 메서드 실행 전후에 권한을 검사합니다.
     * 
     * @param joinPoint 조인포인트
     * @param requireRole @RequireRole 어노테이션
     * @return 메서드 실행 결과
     * @throws Throwable 메서드 실행 중 발생한 예외 또는 권한 부족 예외
     */
    @Around("@annotation(requireRole) || @within(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) 
            throws Throwable {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InsufficientPrivilegesException("Authentication required");
        }
        
        if (hasRequiredRoles(authentication, requireRole)) {
            logger.debug("Access granted to user '{}' for method '{}'", 
                    authentication.getName(), joinPoint.getSignature().getName());
            return joinPoint.proceed();
        } else {
            String requiredRoles = Arrays.toString(requireRole.value());
            logger.warn("Access denied to user '{}' for method '{}'. Required roles: {}", 
                    authentication.getName(), joinPoint.getSignature().getName(), requiredRoles);
            throw new InsufficientPrivilegesException(requireRole.errorMessage());
        }
    }
    
    /**
     * 사용자가 필요한 역할을 가지고 있는지 확인합니다.
     * 
     * @param authentication 인증 정보
     * @param requireRole @RequireRole 어노테이션
     * @return 권한 보유 여부
     */
    private boolean hasRequiredRoles(Authentication authentication, RequireRole requireRole) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Set<String> userRoles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .collect(Collectors.toSet());
        
        String[] requiredRoles = requireRole.value();
        Set<String> requiredRoleSet = Arrays.stream(requiredRoles)
                .collect(Collectors.toSet());
        
        return switch (requireRole.operation()) {
            case AND -> userRoles.containsAll(requiredRoleSet);
            case OR -> requiredRoleSet.stream().anyMatch(userRoles::contains);
        };
    }
    
}
