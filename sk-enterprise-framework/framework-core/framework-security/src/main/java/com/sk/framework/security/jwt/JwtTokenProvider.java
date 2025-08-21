package com.sk.framework.security.jwt;

import com.sk.framework.security.config.SecurityProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 토큰 제공자
 * 
 * <p>JWT 토큰의 생성, 검증, 파싱 기능을 제공합니다.</p>
 * <p>Single Responsibility Principle을 준수하여 JWT 관련 기능만 담당합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@Component
public class JwtTokenProvider {
    
    private final SecurityProperties securityProperties;
    private final Key key;
    
    /**
     * 생성자
     * 
     * @param securityProperties 보안 설정 프로퍼티
     */
    public JwtTokenProvider(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        this.key = Keys.hmacShaKeyFor(
                securityProperties.getJwt().getSecretKey().getBytes(StandardCharsets.UTF_8)
        );
    }
    
    /**
     * 액세스 토큰을 생성합니다.
     * 
     * @param authentication 인증 정보
     * @return JWT 액세스 토큰
     */
    public String generateAccessToken(Authentication authentication) {
        return generateToken(
                authentication,
                securityProperties.getJwt().getAccessTokenExpiration(),
                TokenType.ACCESS
        );
    }
    
    /**
     * 리프레시 토큰을 생성합니다.
     * 
     * @param authentication 인증 정보
     * @return JWT 리프레시 토큰
     */
    public String generateRefreshToken(Authentication authentication) {
        return generateToken(
                authentication,
                securityProperties.getJwt().getRefreshTokenExpiration(),
                TokenType.REFRESH
        );
    }
    
    /**
     * JWT 토큰을 생성합니다.
     * 
     * @param authentication 인증 정보
     * @param expirationSeconds 만료 시간 (초)
     * @param tokenType 토큰 타입
     * @return JWT 토큰
     */
    private String generateToken(Authentication authentication, long expirationSeconds, TokenType tokenType) {
        String username = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationSeconds * 1000);
        
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .claim("type", tokenType.name())
                .setIssuer(securityProperties.getJwt().getIssuer())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * JWT 토큰에서 사용자 이름을 추출합니다.
     * 
     * @param token JWT 토큰
     * @return 사용자 이름
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseClaimsFromToken(token);
        return claims.getSubject();
    }
    
    /**
     * JWT 토큰에서 권한 정보를 추출합니다.
     * 
     * @param token JWT 토큰
     * @return 권한 목록
     */
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> getAuthoritiesFromToken(String token) {
        Claims claims = parseClaimsFromToken(token);
        List<String> roles = claims.get("roles", List.class);
        
        if (roles == null) {
            return List.of();
        }
        
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
    
    /**
     * JWT 토큰에서 Authentication 객체를 생성합니다.
     * 
     * @param token JWT 토큰
     * @return Authentication 객체
     */
    public Authentication getAuthenticationFromToken(String token) {
        String username = getUsernameFromToken(token);
        Collection<GrantedAuthority> authorities = getAuthoritiesFromToken(token);
        
        User principal = new User(username, "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }
    
    /**
     * JWT 토큰의 만료 시간을 확인합니다.
     * 
     * @param token JWT 토큰
     * @return 만료 시간
     */
    public LocalDateTime getExpirationFromToken(String token) {
        Claims claims = parseClaimsFromToken(token);
        return claims.getExpiration().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
    
    /**
     * JWT 토큰이 유효한지 검증합니다.
     * 
     * @param token JWT 토큰
     * @return 유효성 여부
     */
    public boolean validateToken(String token) {
        try {
            parseClaimsFromToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * JWT 토큰이 만료되었는지 확인합니다.
     * 
     * @param token JWT 토큰
     * @return 만료 여부
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }
    
    /**
     * JWT 토큰의 타입을 확인합니다.
     * 
     * @param token JWT 토큰
     * @return 토큰 타입
     */
    public TokenType getTokenType(String token) {
        try {
            Claims claims = parseClaimsFromToken(token);
            String type = claims.get("type", String.class);
            return type != null ? TokenType.valueOf(type) : TokenType.ACCESS;
        } catch (JwtException | IllegalArgumentException e) {
            return TokenType.ACCESS;
        }
    }
    
    /**
     * JWT 토큰에서 Claims를 파싱합니다.
     * 
     * @param token JWT 토큰
     * @return Claims 객체
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    private Claims parseClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Authorization 헤더에서 토큰을 추출합니다.
     * 
     * @param authorizationHeader Authorization 헤더 값
     * @return JWT 토큰 (접두사 제거된)
     */
    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(
                securityProperties.getJwt().getTokenPrefix())) {
            return null;
        }
        
        return authorizationHeader.substring(
                securityProperties.getJwt().getTokenPrefix().length()
        ).trim();
    }
    
    /**
     * 토큰 타입 열거형
     */
    public enum TokenType {
        ACCESS, REFRESH
    }
    
}
