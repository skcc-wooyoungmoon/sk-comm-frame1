package com.sk.framework.transaction.annotation;

import java.lang.annotation.*;

/**
 * @className    : RetryOnConflict
 * @description  : 낙관적 락(Optimistic Lock) 충돌 시 자동 재시도 어노테이션.
 *                 {@code OptimisticLockingFailureException} 등 동시성 충돌 예외가 발생하면
 *                 지정된 횟수만큼 지수 백오프(exponential backoff)로 재시도합니다.
 *                 각 재시도는 새로운 트랜잭션에서 수행되어야 하므로,
 *                 재시도 대상 메소드는 트랜잭션 경계의 "바깥"에 두는 것을 권장합니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RetryOnConflict {

    /**
     * 최대 재시도 횟수(최초 시도 제외).
     *
     * @return 최대 재시도 횟수
     */
    int maxAttempts() default 3;

    /**
     * 최초 재시도 대기 시간(ms).
     *
     * @return 대기 시간(ms)
     */
    long backoffMillis() default 50L;

    /**
     * 백오프 배수. 재시도할 때마다 대기 시간에 곱해집니다.
     *
     * @return 백오프 배수
     */
    double multiplier() default 2.0;

    /**
     * 최대 대기 시간(ms). 지수 백오프가 이 값을 넘지 않도록 제한합니다.
     *
     * @return 최대 대기 시간(ms)
     */
    long maxBackoffMillis() default 1000L;
}
