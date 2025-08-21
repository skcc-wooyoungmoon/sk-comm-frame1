package com.sk.framework.security.config;

import com.sk.framework.security.filter.JwtAuthenticationFilter;
import com.sk.framework.security.jwt.JwtTokenProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 보안 자동 설정 클래스
 * 
 * <p>SK Framework 보안 기능을 자동으로 설정합니다.</p>
 * <p>JWT 기반 인증, CORS 설정, 보안 필터 체인을 구성합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "sk.framework.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SecurityProperties.class)
@EnableWebSecurity
@EnableMethodSecurity
@ComponentScan(basePackages = "com.sk.framework.security")
public class SecurityAutoConfiguration {
    
    private final SecurityProperties securityProperties;
    
    /**
     * 생성자
     * 
     * @param securityProperties 보안 설정 프로퍼티
     */
    public SecurityAutoConfiguration(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }
    
    /**
     * JWT 토큰 제공자 빈을 생성합니다.
     * 
     * @return JwtTokenProvider 인스턴스
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(securityProperties);
    }
    
    /**
     * 패스워드 인코더 빈을 생성합니다.
     * 
     * @return PasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * JWT 인증 필터 빈을 생성합니다.
     * 
     * @param jwtTokenProvider JWT 토큰 제공자
     * @return JwtAuthenticationFilter 인스턴스
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        return new JwtAuthenticationFilter(jwtTokenProvider, securityProperties);
    }
    
    /**
     * 보안 필터 체인을 구성합니다.
     * 
     * @param http HttpSecurity 객체
     * @param jwtAuthenticationFilter JWT 인증 필터
     * @return SecurityFilterChain
     * @throws Exception 설정 중 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, 
                                           JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        
        http
            // CSRF 비활성화 (JWT 사용으로 인해)
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 세션 관리 - 무상태로 설정
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 권한 설정
            .authorizeHttpRequests(auth -> {
                // 무시할 경로들은 허용
                String[] ignoredPaths = securityProperties.getIgnoredPaths().toArray(new String[0]);
                auth.requestMatchers(ignoredPaths).permitAll();
                
                // 나머지 모든 요청은 인증 필요
                auth.anyRequest().authenticated();
            })
            
            // JWT 인증 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // 기본 로그인 폼 비활성화
            .formLogin(AbstractHttpConfigurer::disable)
            
            // HTTP Basic 인증 비활성화
            .httpBasic(AbstractHttpConfigurer::disable);
        
        return http.build();
    }
    
    /**
     * CORS 설정을 구성합니다.
     * 
     * @return CorsConfigurationSource
     */
    @Bean
    @ConditionalOnProperty(prefix = "sk.framework.security.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        SecurityProperties.Cors corsProps = securityProperties.getCors();
        
        configuration.setAllowedOrigins(corsProps.getAllowedOrigins());
        configuration.setAllowedMethods(corsProps.getAllowedMethods());
        configuration.setAllowedHeaders(corsProps.getAllowedHeaders());
        configuration.setExposedHeaders(corsProps.getExposedHeaders());
        configuration.setAllowCredentials(corsProps.isAllowCredentials());
        configuration.setMaxAge(corsProps.getMaxAge());
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
}
