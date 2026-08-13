package com.sk.framework.transaction.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @className    : InMemoryDistributedLockManager
 * @description  : 단일 JVM용 분산 락 기본 구현. {@code ReentrantLock} 기반이며 임대(lease) 시간 경과 시
 *                 자동 해제를 흉내내기 위해 획득 시각을 함께 관리합니다.
 *                 ★ 다중 인스턴스 환경에서는 반드시 Redis/DB 기반 구현으로 교체하십시오.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Slf4j
public class InMemoryDistributedLockManager implements DistributedLockManager {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        try {
            boolean acquired = lock.tryLock(waitTime, unit);
            if (acquired) {
                log.debug("분산 락 획득. key={}", key);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        ReentrantLock lock = locks.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("분산 락 해제. key={}", key);
        }
    }
}
