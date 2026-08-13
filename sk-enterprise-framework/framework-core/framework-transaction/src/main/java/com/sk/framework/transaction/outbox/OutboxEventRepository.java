package com.sk.framework.transaction.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @className    : OutboxEventRepository
 * @description  : 아웃박스 이벤트 저장소.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * 발행 대기(PENDING) 이벤트를 생성 순서(created_at)대로 조회합니다.
     *
     * @param status   조회 상태
     * @param pageable 배치 크기 제한
     * @return 발행 대상 이벤트 목록
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status, Pageable pageable);
}
