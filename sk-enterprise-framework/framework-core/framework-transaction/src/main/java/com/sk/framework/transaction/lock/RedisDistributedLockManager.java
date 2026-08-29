package com.sk.framework.transaction.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @className    : RedisDistributedLockManager
 * @description  : Redis 기반 분산 락(운영용). {@code SET key token NX PX leaseTime}로 원자적 획득,
 *                 해제는 Lua 스크립트로 "내 토큰일 때만" 삭제하여 타인의 락을 해제하지 않도록 한다.
 *                 락 토큰은 (스레드+키)별로 보관하여 재진입/해제 안전성을 확보한다.
 * @modification : 2026.08.14(프레임워크팀) 최초생성(참조 구현)
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Slf4j
public class RedisDistributedLockManager implements DistributedLockManager {

    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;
    private final String keyPrefix;

    /** (스레드 로컬) 키 → 소유 토큰 */
    private final ThreadLocal<Map<String, String>> heldTokens = ThreadLocal.withInitial(HashMap::new);

    public RedisDistributedLockManager(StringRedisTemplate redis, String keyPrefix) {
        this.redis = redis;
        this.keyPrefix = keyPrefix == null ? "lock:" : keyPrefix;
    }

    private String k(String key) {
        return keyPrefix + key;
    }

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        String token = UUID.randomUUID().toString();
        long deadline = System.nanoTime() + unit.toNanos(waitTime);
        Duration lease = Duration.ofMillis(unit.toMillis(leaseTime));

        do {
            Boolean ok = redis.opsForValue().setIfAbsent(k(key), token, lease);
            if (Boolean.TRUE.equals(ok)) {
                heldTokens.get().put(key, token);
                return true;
            }
            try {
                Thread.sleep(50); // 짧은 백오프 후 재시도
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (System.nanoTime() < deadline);

        return false;
    }

    @Override
    public void unlock(String key) {
        String token = heldTokens.get().remove(key);
        if (token == null) {
            return;
        }
        try {
            redis.execute(UNLOCK_SCRIPT, Collections.singletonList(k(key)), token);
        } catch (Exception e) {
            log.warn("분산 락 해제 실패(임대시간 경과 시 자동 해제됨). key={}", key, e);
        } finally {
            if (heldTokens.get().isEmpty()) {
                heldTokens.remove();
            }
        }
    }
}
