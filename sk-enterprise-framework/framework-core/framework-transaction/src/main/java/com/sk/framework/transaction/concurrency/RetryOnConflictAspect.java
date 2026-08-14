package com.sk.framework.transaction.concurrency;

import com.sk.framework.transaction.annotation.RetryOnConflict;
import com.sk.framework.transaction.exception.ConcurrencyConflictException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;

/**
 * @className    : RetryOnConflictAspect
 * @description  : {@code @RetryOnConflict} 어드바이스. 동시성 충돌 예외 발생 시
 *                 지수 백오프로 지정 횟수만큼 재시도합니다.
 *                 트랜잭션 어드바이스보다 바깥에서 동작해야 각 재시도가 새 트랜잭션으로 수행되므로
 *                 우선순위(order)를 트랜잭션(Ordered.LOWEST-1 = Integer.MAX_VALUE)보다 앞에 둡니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Slf4j
@Aspect
@Order(20)
public class RetryOnConflictAspect {

    @Around("@annotation(retry)")
    public Object around(ProceedingJoinPoint joinPoint, RetryOnConflict retry) throws Throwable {
        String method = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
        int maxAttempts = retry.maxAttempts();
        long backoff = retry.backoffMillis();

        int attempt = 0;
        Throwable lastError;
        while (true) {
            try {
                return joinPoint.proceed();
            } catch (Throwable ex) {
                if (!isConcurrencyConflict(ex)) {
                    throw ex;
                }
                lastError = ex;
                if (attempt >= maxAttempts) {
                    log.warn("동시성 충돌 재시도 소진. method={}, attempts={}", method, attempt);
                    throw new ConcurrencyConflictException(
                            "동시성 충돌로 처리에 실패했습니다. (method=" + method + ", 재시도=" + attempt + "회)",
                            lastError);
                }
                attempt++;
                long sleep = Math.min(
                        (long) (backoff * Math.pow(retry.multiplier(), attempt - 1.0)),
                        retry.maxBackoffMillis());
                log.info("동시성 충돌 감지 - {}ms 후 재시도({}/{}). method={}",
                        sleep, attempt, maxAttempts, method);
                sleep(sleep);
            }
        }
    }

    /**
     * 동시성 충돌 계열 예외인지 판별합니다. (원인 체인까지 탐색)
     */
    private boolean isConcurrencyConflict(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof OptimisticLockingFailureException
                    || current instanceof PessimisticLockingFailureException
                    || current instanceof ConcurrencyFailureException
                    || current instanceof jakarta.persistence.OptimisticLockException
                    || current instanceof jakarta.persistence.PessimisticLockException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ConcurrencyConflictException("재시도 대기 중 인터럽트가 발생했습니다.", ie);
        }
    }
}
