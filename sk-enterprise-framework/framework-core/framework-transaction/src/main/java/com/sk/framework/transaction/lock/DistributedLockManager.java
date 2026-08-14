package com.sk.framework.transaction.lock;

import java.util.concurrent.TimeUnit;

/**
 * @className    : DistributedLockManager
 * @description  : 분산 락 관리자 추상화.
 *                 기본 구현은 단일 JVM용({@code InMemoryDistributedLockManager})이며,
 *                 운영에서는 Redis(Redisson)/DB 기반 구현으로 이 빈을 교체합니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public interface DistributedLockManager {

    /**
     * 락 획득을 시도합니다.
     *
     * @param key       락 키
     * @param waitTime  락 획득 대기 시간
     * @param leaseTime 락 임대 시간(자동 해제까지의 시간)
     * @param unit      시간 단위
     * @return 락 획득에 성공하면 true
     */
    boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 락을 해제합니다.
     *
     * @param key 락 키
     */
    void unlock(String key);
}
