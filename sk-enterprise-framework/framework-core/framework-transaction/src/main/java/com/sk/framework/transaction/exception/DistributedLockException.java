package com.sk.framework.transaction.exception;

import com.sk.framework.common.exception.FrameworkException;

/**
 * @className    : DistributedLockException
 * @description  : 분산 락 획득에 실패했을 때 발생하는 예외.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public class DistributedLockException extends FrameworkException {

    private static final long serialVersionUID = 1L;

    public DistributedLockException(String message) {
        super("DISTRIBUTED_LOCK_FAILED", message);
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
