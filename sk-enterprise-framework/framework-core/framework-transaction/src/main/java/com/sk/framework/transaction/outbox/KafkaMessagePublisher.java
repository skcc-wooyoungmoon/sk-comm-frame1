package com.sk.framework.transaction.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @className    : KafkaMessagePublisher
 * @description  : Kafka 기반 아웃박스 발행(운영용). 토픽은 {@code topicPrefix + eventType},
 *                 키는 aggregateId(파티션 정렬 보장), 값은 payload(JSON)로 전송한다.
 *                 동기 전송으로 예외를 릴레이에 전파하여 실패 시 재시도가 이뤄지도록 한다.
 *                 소비자는 at-least-once 전제하에 멱등 처리를 구현해야 한다.
 * @modification : 2026.08.14(프레임워크팀) 최초생성(참조 구현)
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Slf4j
public class KafkaMessagePublisher implements MessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topicPrefix;
    private final long sendTimeoutSeconds;

    public KafkaMessagePublisher(KafkaTemplate<String, String> kafkaTemplate, String topicPrefix, long sendTimeoutSeconds) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicPrefix = topicPrefix == null ? "" : topicPrefix;
        this.sendTimeoutSeconds = sendTimeoutSeconds <= 0 ? 10 : sendTimeoutSeconds;
    }

    @Override
    public void publish(OutboxEvent event) {
        String topic = topicPrefix + event.getEventType();
        try {
            // 동기 전송: 실패 시 예외를 던져 릴레이가 재시도 처리하도록 한다.
            kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
            log.debug("[KAFKA-PUBLISH] topic={}, key={}, type={}", topic, event.getAggregateId(), event.getEventType());
        } catch (Exception e) {
            throw new IllegalStateException("Kafka 발행 실패: topic=" + topic + ", eventId=" + event.getId(), e);
        }
    }
}
