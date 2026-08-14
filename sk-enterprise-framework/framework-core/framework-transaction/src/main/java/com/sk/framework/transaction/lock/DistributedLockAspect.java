package com.sk.framework.transaction.lock;

import com.sk.framework.transaction.annotation.DistributedLock;
import com.sk.framework.transaction.exception.DistributedLockException;
import com.sk.framework.transaction.support.SpelKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;

/**
 * @className    : DistributedLockAspect
 * @description  : {@code @DistributedLock} 어드바이스. 키에 대한 락을 획득한 뒤 메소드를 실행하고
 *                 finally에서 반드시 해제합니다. 트랜잭션보다 바깥에서 락을 잡도록
 *                 우선순위를 앞(order=5)에 둡니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Slf4j
@Aspect
@Order(5)
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final DistributedLockManager lockManager;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String key = SpelKeyResolver.resolve(joinPoint, distributedLock.key());
        if (key == null || key.isBlank()) {
            throw new DistributedLockException("분산 락 키를 계산할 수 없습니다. (SpEL=" + distributedLock.key() + ")");
        }

        boolean acquired = lockManager.tryLock(
                key, distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
        if (!acquired) {
            throw new DistributedLockException("분산 락 획득에 실패했습니다. (key=" + key + ")");
        }

        try {
            return joinPoint.proceed();
        } finally {
            lockManager.unlock(key);
        }
    }
}
