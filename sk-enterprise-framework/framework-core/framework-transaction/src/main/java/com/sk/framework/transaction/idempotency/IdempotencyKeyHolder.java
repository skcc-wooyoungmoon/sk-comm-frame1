package com.sk.framework.transaction.idempotency;

/**
 * @className    : IdempotencyKeyHolder
 * @description  : 요청 스코프(ThreadLocal)로 전달되는 멱등키 보관소.
 *                 웹 계층의 필터/인터셉터가 HTTP 헤더 {@code Idempotency-Key}를 읽어 여기에 담아두면,
 *                 {@code @Idempotent} 어드바이스가 SpEL 키가 없을 때 이 값을 사용합니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public final class IdempotencyKeyHolder {

    /** HTTP 표준 멱등키 헤더명 */
    public static final String HEADER_NAME = "Idempotency-Key";

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private IdempotencyKeyHolder() {
    }

    public static void set(String key) {
        HOLDER.set(key);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
