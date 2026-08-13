package com.sk.framework.transaction.outbox;

/**
 * @className    : OutboxEventStatus
 * @description  : 아웃박스 이벤트 발행 상태.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public enum OutboxEventStatus {
    /** 발행 대기 */
    PENDING,
    /** 발행 완료 */
    PUBLISHED,
    /** 재시도 임계치 초과로 발행 실패(수동 개입 필요) */
    FAILED
}
