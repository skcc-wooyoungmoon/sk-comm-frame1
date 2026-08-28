package com.sk.framework.security.filter;

import com.sk.framework.security.config.SecurityProperties;
import com.sk.framework.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * 
 * <p>HTTP 요청에서 JWT 토큰을 추출하여 인증을 처리합니다.</p>
 * <p>Single Responsibility Principle을 준수하여 JWT 인증만 담당합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    /**
     * 생성자
     * 
     * @param jwtTokenProvider JWT 토큰 제공자
     * @param securityProperties 보안 설정 프로퍼티
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, 
                                   SecurityProperties securityProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.securityProperties = securityProperties;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            String token = extractJwtFromRequest(request);
            
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthenticationFromToken(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                if (securityProperties.isDebug()) {
                    logger.debug("JWT authentication successful for user: {}", 
                            authentication.getName());
                }
            }
        } catch (Exception e) {
            logger.error("JWT authentication failed", e);
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // 보안 무시 경로들은 필터링하지 않음
        return securityProperties.getIgnoredPaths().stream()
                .anyMatch(ignoredPath -> pathMatcher.match(ignoredPath, path));
    }
    
    /**
     * HTTP 요청에서 JWT 토큰을 추출합니다.
     * 
     * @param request HTTP 요청
     * @return JWT 토큰 (없으면 null)
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(
                securityProperties.getJwt().getHeaderName()
        );
        
        return jwtTokenProvider.extractTokenFromHeader(authorizationHeader);
    }
    
}
