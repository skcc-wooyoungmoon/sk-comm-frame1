package com.sk.framework.transaction.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @className    : DistributedLock
 * @description  : 분산 락(Distributed Lock) 어노테이션.
 *                 지정한 키에 대해 락을 획득한 뒤 메소드를 실행하고, 종료 시 해제합니다.
 *                 다중 인스턴스 환경에서 동일 자원에 대한 동시 갱신을 직렬화할 때 사용합니다.
 *                 기본 구현은 단일 JVM용(InMemory)이며, 운영에서는 Redis/DB 기반 구현으로
 *                 {@code DistributedLockManager} 빈을 교체하면 됩니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 락 키를 계산할 SpEL 표현식. (예: {@code "'stock:' + #productId"})
     *
     * @return 락 키 SpEL 표현식
     */
    String key();

    /**
     * 락 획득 대기 시간. 이 시간 안에 락을 얻지 못하면 예외를 던집니다.
     *
     * @return 대기 시간
     */
    long waitTime() default 5L;

    /**
     * 락 임대(lease) 시간. 이 시간이 지나면 락이 자동 해제되어 데드락을 방지합니다.
     *
     * @return 임대 시간
     */
    long leaseTime() default 10L;

    /**
     * 시간 단위.
     *
     * @return 시간 단위
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
