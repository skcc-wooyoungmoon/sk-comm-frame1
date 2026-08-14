package com.sk.framework.transaction.exception;

import com.sk.framework.common.exception.FrameworkException;

/**
 * @className    : ConcurrencyConflictException
 * @description  : 낙관적 락 재시도가 모두 소진되었음에도 동시성 충돌이 해소되지 않을 때 발생하는 예외.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public class ConcurrencyConflictException extends FrameworkException {

    private static final long serialVersionUID = 1L;

    public ConcurrencyConflictException(String message, Throwable cause) {
        super("CONCURRENCY_CONFLICT", message, cause);
    }

    @Override
    public ErrorSeverity getSeverity() {
        return ErrorSeverity.MEDIUM;
    }

    @Override
    public boolean isRetryable() {
        return true;
    }
}
