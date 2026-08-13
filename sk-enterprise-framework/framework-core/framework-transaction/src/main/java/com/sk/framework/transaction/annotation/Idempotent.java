package com.sk.framework.transaction.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @className    : Idempotent
 * @description  : 멱등성(Idempotency) 보장 어노테이션.
 *                 동일한 멱등키(Idempotency-Key)로 들어온 중복 요청에 대해
 *                 비즈니스 로직을 1회만 수행하고, 이후 요청에는 최초 수행 결과를 반환하거나
 *                 진행 중 요청은 충돌로 차단합니다.
 *                 결제/주문/포인트 적립 등 "정확히 한 번(exactly-once)" 처리가 필요한
 *                 거래에 사용합니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 멱등키를 계산할 SpEL 표현식.
     * <p>미지정 시 {@code IdempotencyKeyHolder}(HTTP 헤더 {@code Idempotency-Key})의 값을 사용합니다.</p>
     * <pre>{@code
     * @Idempotent(key = "'order:' + #command.orderNo")
     * public OrderResult place(OrderCommand command) { ... }
     * }</pre>
     *
     * @return 멱등키 SpEL 표현식
     */
    String key() default "";

    /**
     * 멱등 결과를 보관하는 기간. 이 기간이 지나면 동일 키의 재수행이 허용됩니다.
     *
     * @return 보관 기간
     */
    long ttl() default 24;

    /**
     * TTL 시간 단위.
     *
     * @return 시간 단위
     */
    TimeUnit timeUnit() default TimeUnit.HOURS;

    /**
     * 처리 중(IN_PROGRESS)인 동일 키 요청이 들어왔을 때의 동작.
     * <p>true(기본): 즉시 충돌 예외를 던져 중복 처리를 차단합니다.</p>
     * <p>false: 진행 중 상태를 무시하고 신규로 처리합니다.(권장하지 않음)</p>
     *
     * @return 진행 중 요청 차단 여부
     */
    boolean failOnConcurrent() default true;
}
