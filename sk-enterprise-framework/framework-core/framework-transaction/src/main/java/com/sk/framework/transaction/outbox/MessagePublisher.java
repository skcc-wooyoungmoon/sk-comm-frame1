package com.sk.framework.transaction.outbox;

/**
 * @className    : MessagePublisher
 * @description  : 아웃박스 릴레이가 실제 메시지를 외부 브로커(Kafka/RabbitMQ/SNS 등)로 발행할 때
 *                 사용하는 추상화. 기본 구현({@code LoggingMessagePublisher})은 로그만 남기므로,
 *                 프로젝트에서 실제 브로커 연동 빈으로 교체하십시오.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public interface MessagePublisher {

    /**
     * 아웃박스 이벤트를 외부로 발행합니다. 예외를 던지면 릴레이가 재시도 처리합니다.
     *
     * @param event 발행할 이벤트
     */
    void publish(OutboxEvent event);
}
