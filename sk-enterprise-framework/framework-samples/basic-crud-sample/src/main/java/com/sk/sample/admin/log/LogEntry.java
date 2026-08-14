package com.sk.sample.admin.log;

import lombok.*;

/**
 * @className    : LogEntry
 * @description  : 로그 뷰어용 로그 항목 DTO.
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Getter
@AllArgsConstructor
@Builder
public class LogEntry {
    private long timestamp;   // epoch millis
    private String level;     // ERROR/WARN/INFO/DEBUG
    private String logger;    // 로거명(축약)
    private String thread;
    private String message;
    private String traceId;   // MDC traceId (없으면 null)
}
