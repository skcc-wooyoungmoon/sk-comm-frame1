package com.sk.framework.transaction.idempotency;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @className    : InMemoryIdempotencyStoreTest
 * @description  : 인메모리 멱등 저장소 단위 테스트.
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
class InMemoryIdempotencyStoreTest {

    private final IdempotencyStore store = new InMemoryIdempotencyStore();
    private static final Duration TTL = Duration.ofMinutes(10);

    @Test
    void tryBegin_firstCallSucceeds_secondCallFails() {
        assertThat(store.tryBegin("key-1", TTL)).isTrue();
        assertThat(store.tryBegin("key-1", TTL)).isFalse(); // 이미 선점됨
    }

    @Test
    void complete_storesResult_andFindReturnsCompleted() {
        store.tryBegin("key-2", TTL);
        store.complete("key-2", "RESULT", TTL);

        Optional<IdempotencyRecord> found = store.find("key-2");
        assertThat(found).isPresent();
        assertThat(found.get().isCompleted()).isTrue();
        assertThat(found.get().getResult()).isEqualTo("RESULT");
    }

    @Test
    void remove_allowsReBegin() {
        store.tryBegin("key-3", TTL);
        store.remove("key-3");
        assertThat(store.tryBegin("key-3", TTL)).isTrue(); // 제거 후 재선점 가능
    }

    @Test
    void inProgressRecord_isNotCompleted() {
        store.tryBegin("key-4", TTL);
        Optional<IdempotencyRecord> found = store.find("key-4");
        assertThat(found).isPresent();
        assertThat(found.get().isInProgress()).isTrue();
        assertThat(found.get().isCompleted()).isFalse();
    }

    @Test
    void expiredRecord_isTreatedAsAbsent() {
        // TTL 0 → 즉시 만료
        store.tryBegin("key-5", Duration.ofMillis(1));
        try {
            Thread.sleep(5);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        assertThat(store.find("key-5")).isEmpty();
        assertThat(store.tryBegin("key-5", TTL)).isTrue(); // 만료 후 재선점 가능
    }
}
