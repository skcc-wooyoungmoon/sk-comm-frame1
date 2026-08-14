package com.sk.framework.transaction.idempotency;

import java.time.Duration;
import java.util.Optional;

/**
 * @className    : IdempotencyStore
 * @description  : 멱등 처리 이력 저장소 추상화.
 *                 기본 구현은 단일 JVM용 {@code InMemoryIdempotencyStore}이며,
 *                 운영 환경에서는 Redis/DB 기반 구현으로 교체하여 다중 인스턴스/영속성을 확보합니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public interface IdempotencyStore {

    /**
     * 멱등키에 대해 "처리 중(IN_PROGRESS)" 상태를 선점(atomic put-if-absent)합니다.
     *
     * @param key 멱등키
     * @param ttl 보관 기간
     * @return 선점에 성공하면 true, 이미 이력이 존재하면 false
     */
    boolean tryBegin(String key, Duration ttl);

    /**
     * 멱등키의 현재 이력을 조회합니다.
     *
     * @param key 멱등키
     * @return 이력(없으면 Optional.empty)
     */
    Optional<IdempotencyRecord> find(String key);

    /**
     * 처리 완료 결과를 저장합니다. 상태를 COMPLETED로 전이시킵니다.
     *
     * @param key    멱등키
     * @param result 최초 수행 결과
     * @param ttl    보관 기간
     */
    void complete(String key, Object result, Duration ttl);

    /**
     * 처리 실패 시 이력을 제거하여 재수행이 가능하도록 합니다.
     *
     * @param key 멱등키
     */
    void remove(String key);
}
