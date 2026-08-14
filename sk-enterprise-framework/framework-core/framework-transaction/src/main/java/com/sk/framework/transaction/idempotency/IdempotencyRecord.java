package com.sk.framework.transaction.idempotency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * @className    : IdempotencyRecord
 * @description  : 멱등 처리 이력 레코드. 특정 멱등키의 처리 상태와 최초 수행 결과를 보관합니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Getter
@Builder
@AllArgsConstructor
public class IdempotencyRecord {

    /** 멱등키 */
    private final String key;

    /** 처리 상태 */
    private final Status status;

    /** 최초 수행 결과(직렬화 없이 원본 객체 보관 - InMemory 구현 기준) */
    private final Object result;

    /** 만료 시각 */
    private final Instant expiresAt;

    /**
     * 멱등 처리 상태.
     */
    public enum Status {
        /** 처리 중 */
        IN_PROGRESS,
        /** 처리 완료(결과 보관) */
        COMPLETED
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public boolean isInProgress() {
        return status == Status.IN_PROGRESS;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }
}
