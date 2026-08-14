package com.sk.framework.transaction.exception;

import com.sk.framework.common.exception.FrameworkException;

/**
 * @className    : IdempotencyConflictException
 * @description  : 동일 멱등키의 요청이 이미 처리 중이거나 처리 완료되어 재수행이 거부될 때 발생하는 예외.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public class IdempotencyConflictException extends FrameworkException {

    private static final long serialVersionUID = 1L;

    public IdempotencyConflictException(String message) {
        super("IDEMPOTENCY_CONFLICT", message);
    }

    @Override
    public ErrorSeverity getSeverity() {
        return ErrorSeverity.LOW;
    }
}
