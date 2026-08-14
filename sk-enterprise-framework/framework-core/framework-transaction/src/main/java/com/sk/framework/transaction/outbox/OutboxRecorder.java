package com.sk.framework.transaction.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @className    : OutboxRecorder
 * @description  : 비즈니스 로직에서 아웃박스 이벤트를 기록하는 진입점.
 *                 반드시 비즈니스 데이터 변경과 "같은 트랜잭션" 안에서 호출해야 원자성이 보장됩니다.
 *                 (그래서 전파 속성을 MANDATORY로 두어 트랜잭션 밖 호출을 방지합니다.)
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@RequiredArgsConstructor
public class OutboxRecorder {

    private final OutboxEventRepository outboxEventRepository;

    /**
     * 아웃박스 이벤트를 현재 트랜잭션에 기록합니다.
     *
     * @param aggregateType 애그리거트 타입 (예: "Order")
     * @param aggregateId   애그리거트 식별자 (예: 주문번호)
     * @param eventType     이벤트 타입 (예: "OrderPlaced")
     * @param payload       직렬화된 페이로드(JSON 권장)
     * @return 저장된 아웃박스 이벤트
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent record(String aggregateType, String aggregateId, String eventType, String payload) {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .status(OutboxEventStatus.PENDING)
                .build();
        return outboxEventRepository.save(event);
    }
}
