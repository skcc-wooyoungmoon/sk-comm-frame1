package com.sk.framework.transaction.idempotency;

import com.sk.framework.transaction.annotation.Idempotent;
import com.sk.framework.transaction.exception.IdempotencyConflictException;
import com.sk.framework.transaction.support.SpelKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;

import java.time.Duration;
import java.util.Optional;

/**
 * @className    : IdempotencyAspect
 * @description  : {@code @Idempotent} 어드바이스. 멱등키를 계산하여 중복 요청을 걸러냅니다.
 *                 - 최초 요청: IN_PROGRESS 선점 → 비즈니스 수행 → COMPLETED(결과 보관)
 *                 - 완료된 중복 요청: 보관된 최초 결과를 그대로 반환(재수행 없음)
 *                 - 진행 중 중복 요청: 충돌 예외(설정에 따라)
 *                 트랜잭션보다 바깥에서 동작하도록 낮은 order(우선순위 높음)를 부여합니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Slf4j
@Aspect
@Order(10)
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyStore idempotencyStore;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = resolveKey(joinPoint, idempotent);
        if (key == null || key.isBlank()) {
            // 멱등키가 없으면 멱등 처리를 적용하지 않고 그대로 수행한다.
            log.debug("멱등키가 없어 멱등 처리를 건너뜁니다. method={}",
                    ((MethodSignature) joinPoint.getSignature()).getMethod().getName());
            return joinPoint.proceed();
        }

        Duration ttl = Duration.ofMillis(idempotent.timeUnit().toMillis(idempotent.ttl()));

        // 1) 이미 처리 이력이 있는지 확인
        Optional<IdempotencyRecord> existing = idempotencyStore.find(key);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (record.isCompleted()) {
                log.info("멱등 중복 요청 - 최초 결과 반환. key={}", key);
                return record.getResult();
            }
            if (record.isInProgress() && idempotent.failOnConcurrent()) {
                throw new IdempotencyConflictException(
                        "동일한 요청이 이미 처리 중입니다. (idempotencyKey=" + key + ")");
            }
        }

        // 2) IN_PROGRESS 선점 (원자적 put-if-absent)
        if (!idempotencyStore.tryBegin(key, ttl)) {
            // 선점 실패 = 방금 다른 스레드가 선점 → 완료됐으면 결과 반환, 아니면 충돌
            Optional<IdempotencyRecord> raced = idempotencyStore.find(key);
            if (raced.isPresent() && raced.get().isCompleted()) {
                return raced.get().getResult();
            }
            if (idempotent.failOnConcurrent()) {
                throw new IdempotencyConflictException(
                        "동일한 요청이 이미 처리 중입니다. (idempotencyKey=" + key + ")");
            }
        }

        // 3) 비즈니스 수행 및 결과 보관 (실패 시 이력 제거하여 재시도 허용)
        try {
            Object result = joinPoint.proceed();
            idempotencyStore.complete(key, result, ttl);
            log.debug("멱등 처리 완료. key={}", key);
            return result;
        } catch (Throwable ex) {
            idempotencyStore.remove(key);
            throw ex;
        }
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        String key = SpelKeyResolver.resolve(joinPoint, idempotent.key());
        if (key != null && !key.isBlank()) {
            return key;
        }
        // SpEL 키가 없으면 요청 헤더 기반 멱등키를 사용
        return IdempotencyKeyHolder.get();
    }
}
