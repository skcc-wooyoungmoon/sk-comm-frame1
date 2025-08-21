package com.sk.framework.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 보안 모듈 설정 프로퍼티
 * 
 * <p>JWT 설정, CORS 설정, 보안 필터 설정 등을 관리합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "sk.framework.security")
public class SecurityProperties {
    
    /**
     * 보안 기능 활성화 여부
     */
    private boolean enabled = true;
    
    /**
     * JWT 관련 설정
     */
    private Jwt jwt = new Jwt();
    
    /**
     * CORS 관련 설정
     */
    private Cors cors = new Cors();
    
    /**
     * 보안 무시 경로들
     */
    private List<String> ignoredPaths = new ArrayList<>(List.of(
            "/health/**",
            "/actuator/**",
            "/api/public/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    ));
    
    /**
     * 디버그 모드 활성화 여부
     */
    private boolean debug = false;
    
    // Getters and Setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Jwt getJwt() {
        return jwt;
    }
    
    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }
    
    public Cors getCors() {
        return cors;
    }
    
    public void setCors(Cors cors) {
        this.cors = cors;
    }
    
    public List<String> getIgnoredPaths() {
        return ignoredPaths;
    }
    
    public void setIgnoredPaths(List<String> ignoredPaths) {
        this.ignoredPaths = ignoredPaths;
    }
    
    public boolean isDebug() {
        return debug;
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    /**
     * JWT 설정
     */
    public static class Jwt {
        
        /**
         * JWT 서명에 사용할 비밀 키
         */
        private String secretKey = "sk-framework-default-secret-key-change-in-production";
        
        /**
         * 액세스 토큰 만료 시간 (초)
         */
        private long accessTokenExpiration = 3600; // 1시간
        
        /**
         * 리프레시 토큰 만료 시간 (초)
         */
        private long refreshTokenExpiration = 1209600; // 2주
        
        /**
         * 토큰 발급자
         */
        private String issuer = "SK Framework";
        
        /**
         * Authorization 헤더 이름
         */
        private String headerName = "Authorization";
        
        /**
         * 토큰 접두사
         */
        private String tokenPrefix = "Bearer ";
        
        // Getters and Setters
        
        public String getSecretKey() {
            return secretKey;
        }
        
        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
        
        public long getAccessTokenExpiration() {
            return accessTokenExpiration;
        }
        
        public void setAccessTokenExpiration(long accessTokenExpiration) {
            this.accessTokenExpiration = accessTokenExpiration;
        }
        
        public long getRefreshTokenExpiration() {
            return refreshTokenExpiration;
        }
        
        public void setRefreshTokenExpiration(long refreshTokenExpiration) {
            this.refreshTokenExpiration = refreshTokenExpiration;
        }
        
        public String getIssuer() {
            return issuer;
        }
        
        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
        
        public String getHeaderName() {
            return headerName;
        }
        
        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }
        
        public String getTokenPrefix() {
            return tokenPrefix;
        }
        
        public void setTokenPrefix(String tokenPrefix) {
            this.tokenPrefix = tokenPrefix;
        }
    }
    
    /**
     * CORS 설정
     */
    public static class Cors {
        
        /**
         * CORS 활성화 여부
         */
        private boolean enabled = true;
        
        /**
         * 허용된 Origin 목록
         */
        private List<String> allowedOrigins = new ArrayList<>(List.of("*"));
        
        /**
         * 허용된 HTTP 메서드 목록
         */
        private List<String> allowedMethods = new ArrayList<>(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        /**
         * 허용된 헤더 목록
         */
        private List<String> allowedHeaders = new ArrayList<>(List.of("*"));
        
        /**
         * 노출할 헤더 목록
         */
        private List<String> exposedHeaders = new ArrayList<>();
        
        /**
         * 자격 증명 허용 여부
         */
        private boolean allowCredentials = true;
        
        /**
         * Preflight 요청 캐시 시간 (초)
         */
        private long maxAge = 3600;
        
        // Getters and Setters
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }
        
        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
        
        public List<String> getAllowedMethods() {
            return allowedMethods;
        }
        
        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }
        
        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }
        
        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }
        
        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }
        
        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }
        
        public boolean isAllowCredentials() {
            return allowCredentials;
        }
        
        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }
        
        public long getMaxAge() {
            return maxAge;
        }
        
        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }
    
}
