package com.sk.framework.transaction.outbox;

import com.sk.framework.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * @className    : OutboxEvent
 * @description  : 트랜잭션 아웃박스(Transactional Outbox) 이벤트 엔티티.
 *                 비즈니스 데이터 변경과 "동일 트랜잭션"으로 이벤트를 저장(원자성 보장)하고,
 *                 별도 릴레이가 이를 폴링하여 외부(MQ/Kafka 등)로 발행합니다.
 *                 이로써 DB 커밋과 메시지 발행 사이의 이중 쓰기(dual-write) 문제를 해소합니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Entity
@Table(name = "tb_outbox_event",
        indexes = {
                @Index(name = "idx_outbox_status", columnList = "status, created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    /** 애그리거트 타입 (예: Order, User) */
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    /** 애그리거트 식별자 (예: 주문번호) */
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    /** 이벤트 타입 (예: OrderPlaced) */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /** 직렬화된 이벤트 페이로드(JSON 권장) */
    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    /** 발행 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    /** 발행 재시도 횟수 */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /** 마지막 실패 사유 */
    @Column(name = "last_error", length = 1000)
    private String lastError;

    /** 발행 완료 시각 */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /** 발행 성공 처리 */
    public void markPublished() {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.lastError = null;
    }

    /** 발행 실패 처리 (재시도 횟수 증가, 임계치 초과 시 FAILED) */
    public void markFailed(String error, int maxRetry) {
        this.retryCount++;
        this.lastError = error != null && error.length() > 1000 ? error.substring(0, 1000) : error;
        if (this.retryCount >= maxRetry) {
            this.status = OutboxEventStatus.FAILED;
        }
    }
}
