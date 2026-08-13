package com.sk.framework.transaction.outbox;

import lombok.extern.slf4j.Slf4j;

/**
 * @className    : LoggingMessagePublisher
 * @description  : 기본 MessagePublisher 구현. 실제 브로커 대신 로그만 남깁니다.
 *                 데모/로컬 개발용이며, 운영에서는 Kafka/RabbitMQ 등 실제 구현으로 교체합니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Slf4j
public class LoggingMessagePublisher implements MessagePublisher {

    @Override
    public void publish(OutboxEvent event) {
        log.info("[OUTBOX-PUBLISH] type={}, aggregate={}#{}, payload={}",
                event.getEventType(), event.getAggregateType(), event.getAggregateId(), event.getPayload());
    }
}
