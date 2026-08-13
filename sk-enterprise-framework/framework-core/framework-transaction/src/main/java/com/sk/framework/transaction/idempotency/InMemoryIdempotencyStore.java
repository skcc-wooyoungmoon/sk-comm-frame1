package com.sk.framework.transaction.idempotency;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @className    : InMemoryIdempotencyStore
 * @description  : 단일 JVM용 인메모리 멱등 저장소 기본 구현.
 *                 {@code ConcurrentHashMap}의 원자적 연산으로 put-if-absent를 보장합니다.
 *                 다중 인스턴스/영속성이 필요하면 Redis/DB 구현으로 교체하십시오.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final ConcurrentHashMap<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

    @Override
    public boolean tryBegin(String key, Duration ttl) {
        Instant now = Instant.now();
        IdempotencyRecord beginning = IdempotencyRecord.builder()
                .key(key)
                .status(IdempotencyRecord.Status.IN_PROGRESS)
                .expiresAt(now.plus(ttl))
                .build();

        // 만료된 기존 레코드는 신규 선점을 허용하도록 정리한다.
        store.computeIfPresent(key, (k, existing) -> existing.isExpired(now) ? null : existing);

        return store.putIfAbsent(key, beginning) == null;
    }

    @Override
    public Optional<IdempotencyRecord> find(String key) {
        IdempotencyRecord record = store.get(key);
        if (record == null) {
            return Optional.empty();
        }
        if (record.isExpired(Instant.now())) {
            store.remove(key, record);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    @Override
    public void complete(String key, Object result, Duration ttl) {
        IdempotencyRecord completed = IdempotencyRecord.builder()
                .key(key)
                .status(IdempotencyRecord.Status.COMPLETED)
                .result(result)
                .expiresAt(Instant.now().plus(ttl))
                .build();
        store.put(key, completed);
    }

    @Override
    public void remove(String key) {
        store.remove(key);
    }
}
