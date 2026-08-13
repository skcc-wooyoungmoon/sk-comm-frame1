package com.sk.framework.transaction.saga;

import com.sk.framework.common.exception.FrameworkException;

/**
 * @className    : SagaExecutionException
 * @description  : SAGA 실행이 실패하여 보상까지 수행된 경우 발생하는 예외.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public class SagaExecutionException extends FrameworkException {

    private static final long serialVersionUID = 1L;

    public SagaExecutionException(String message, Throwable cause) {
        super("SAGA_EXECUTION_FAILED", message, cause);
    }

    @Override
    public ErrorSeverity getSeverity() {
        return ErrorSeverity.HIGH;
    }
}
