package com.sk.framework.transaction.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @className    : OutboxRelay
 * @description  : 아웃박스 릴레이(폴링 퍼블리셔). 주기적으로 PENDING 이벤트를 조회하여
 *                 {@link MessagePublisher}로 발행하고 상태를 갱신합니다.
 *                 "at-least-once" 전달을 보장하므로, 소비자 측은 멱등 처리를 전제로 설계해야 합니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final MessagePublisher messagePublisher;
    private final OutboxProperties properties;

    /**
     * PENDING 이벤트를 배치로 발행합니다. 발행 실패 이벤트는 재시도 카운트를 올리고,
     * 임계치를 넘으면 FAILED로 격리합니다.
     */
    @Scheduled(fixedDelayString = "${sk.framework.transaction.outbox.poll-interval-ms:5000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.PENDING, PageRequest.of(0, properties.getBatchSize()));

        if (pending.isEmpty()) {
            return;
        }

        log.debug("아웃박스 릴레이 시작 - 대상 {}건", pending.size());
        for (OutboxEvent event : pending) {
            try {
                messagePublisher.publish(event);
                event.markPublished();
            } catch (Exception ex) {
                log.warn("아웃박스 이벤트 발행 실패. eventId={}, retry={}", event.getId(), event.getRetryCount(), ex);
                event.markFailed(ex.getMessage(), properties.getMaxRetry());
            }
        }
    }
}
