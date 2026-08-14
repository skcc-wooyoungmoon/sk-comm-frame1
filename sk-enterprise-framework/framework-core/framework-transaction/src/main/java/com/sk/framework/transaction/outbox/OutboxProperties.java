package com.sk.framework.transaction.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @className    : OutboxProperties
 * @description  : 아웃박스 릴레이 설정. (prefix: sk.framework.transaction.outbox)
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sk.framework.transaction.outbox")
public class OutboxProperties {

    /** 릴레이 활성화 여부 */
    private boolean enabled = true;

    /** 폴링 주기(ms) */
    private long pollIntervalMs = 5000L;

    /** 한 번에 처리할 이벤트 배치 크기 */
    private int batchSize = 100;

    /** 발행 최대 재시도 횟수(초과 시 FAILED) */
    private int maxRetry = 5;
}
