package com.sk.sample.config;

import com.sk.framework.transaction.idempotency.IdempotencyKeyHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @className    : IdempotencyKeyFilter
 * @description  : HTTP 헤더 {@code Idempotency-Key}를 읽어 요청 스코프의 {@link IdempotencyKeyHolder}에 담는 필터.
 *                 {@code @Idempotent}의 SpEL 키가 없을 때 이 헤더 값이 멱등키로 사용된다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Component
@Order(1)
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String key = request.getHeader(IdempotencyKeyHolder.HEADER_NAME);
            if (key != null && !key.isBlank()) {
                IdempotencyKeyHolder.set(key);
            }
            filterChain.doFilter(request, response);
        } finally {
            IdempotencyKeyHolder.clear();
        }
    }
}
