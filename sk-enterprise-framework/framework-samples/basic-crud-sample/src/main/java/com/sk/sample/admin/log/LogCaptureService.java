package com.sk.sample.admin.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @className    : LogCaptureService
 * @description  : 애플리케이션 로그를 인메모리 링버퍼에 캡처하는 서비스.
 *                 기동 시 Logback ROOT 로거에 커스텀 Appender를 부착하여 최근 N건을 보관하고,
 *                 로그 뷰어 화면(/api/admin/logs)에 제공한다.
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Service
public class LogCaptureService {

    /** 보관 최대 건수 */
    private static final int CAPACITY = 500;

    private final Deque<LogEntry> buffer = new ConcurrentLinkedDeque<>();

    @PostConstruct
    public void attach() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        RingBufferAppender appender = new RingBufferAppender();
        appender.setContext(root.getLoggerContext());
        appender.setName("SK_ADMIN_RING");
        appender.start();
        root.addAppender(appender);
    }

    /** 최근 로그를 최신순으로 반환한다. level 필터(선택). */
    public List<LogEntry> recent(String level, int limit) {
        List<LogEntry> snapshot = new ArrayList<>(buffer);
        Collections.reverse(snapshot); // 최신 우선
        return snapshot.stream()
                .filter(e -> level == null || level.isBlank() || e.getLevel().equalsIgnoreCase(level))
                .limit(limit <= 0 ? 100 : limit)
                .toList();
    }

    private void add(LogEntry entry) {
        buffer.addLast(entry);
        while (buffer.size() > CAPACITY) {
            buffer.pollFirst();
        }
    }

    /** Logback 이벤트를 LogEntry로 변환해 링버퍼에 적재하는 내부 Appender. */
    private class RingBufferAppender extends AppenderBase<ILoggingEvent> {
        @Override
        protected void append(ILoggingEvent event) {
            String logger = event.getLoggerName();
            int lastDot = logger.lastIndexOf('.');
            String shortLogger = lastDot >= 0 ? logger.substring(lastDot + 1) : logger;
            add(LogEntry.builder()
                    .timestamp(event.getTimeStamp())
                    .level(event.getLevel().toString())
                    .logger(shortLogger)
                    .thread(event.getThreadName())
                    .message(event.getFormattedMessage())
                    .traceId(event.getMDCPropertyMap().get("traceId"))
                    .build());
        }
    }
}
