package com.sk.framework.transaction.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * @className    : RedisIdempotencyStore
 * @description  : Redis 기반 멱등 저장소(운영용). 다중 인스턴스에서 원자적 선점(SET NX)과 TTL 만료를 지원한다.
 *                 값은 상태 + 결과(JSON) + 결과 타입을 담은 홀더로 직렬화한다.
 *                 참고: 제네릭 타입 파라미터는 역직렬화 시 소실되므로 단순 DTO 결과에 적합하다.
 * @modification : 2026.08.14(프레임워크팀) 최초생성(참조 구현)
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Slf4j
public class RedisIdempotencyStore implements IdempotencyStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;

    public RedisIdempotencyStore(StringRedisTemplate redis, ObjectMapper objectMapper, String keyPrefix) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.keyPrefix = keyPrefix == null ? "idem:" : keyPrefix;
    }

    private String k(String key) {
        return keyPrefix + key;
    }

    @Override
    public boolean tryBegin(String key, Duration ttl) {
        Entry entry = new Entry();
        entry.status = IdempotencyRecord.Status.IN_PROGRESS.name();
        Boolean ok = redis.opsForValue().setIfAbsent(k(key), write(entry), ttl);
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public Optional<IdempotencyRecord> find(String key) {
        String raw = redis.opsForValue().get(k(key));
        if (raw == null) {
            return Optional.empty();
        }
        Entry entry = read(raw);
        if (entry == null) {
            return Optional.empty();
        }
        IdempotencyRecord.Status status = IdempotencyRecord.Status.valueOf(entry.status);
        Object result = null;
        if (status == IdempotencyRecord.Status.COMPLETED && entry.resultJson != null && entry.resultType != null) {
            result = deserializeResult(entry);
        }
        return Optional.of(IdempotencyRecord.builder()
                .key(key)
                .status(status)
                .result(result)
                .build());
    }

    @Override
    public void complete(String key, Object result, Duration ttl) {
        Entry entry = new Entry();
        entry.status = IdempotencyRecord.Status.COMPLETED.name();
        if (result != null) {
            try {
                entry.resultJson = objectMapper.writeValueAsString(result);
                entry.resultType = result.getClass().getName();
            } catch (Exception e) {
                log.warn("멱등 결과 직렬화 실패 - 결과 없이 완료 저장. key={}", key, e);
            }
        }
        redis.opsForValue().set(k(key), write(entry), ttl);
    }

    @Override
    public void remove(String key) {
        redis.delete(k(key));
    }

    private Object deserializeResult(Entry entry) {
        try {
            Class<?> type = Class.forName(entry.resultType);
            return objectMapper.readValue(entry.resultJson, type);
        } catch (Exception e) {
            log.warn("멱등 결과 역직렬화 실패 - null 반환. type={}", entry.resultType, e);
            return null;
        }
    }

    private String write(Entry entry) {
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (Exception e) {
            throw new IllegalStateException("멱등 엔트리 직렬화 실패", e);
        }
    }

    private Entry read(String raw) {
        try {
            return objectMapper.readValue(raw, Entry.class);
        } catch (Exception e) {
            log.warn("멱등 엔트리 역직렬화 실패. raw={}", raw, e);
            return null;
        }
    }

    /** Redis 저장용 홀더 (Jackson 직렬화 대상 - no-arg + public 필드) */
    public static class Entry {
        public String status;
        public String resultJson;
        public String resultType;
    }
}
