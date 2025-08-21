package com.sk.framework.security.exception;

import com.sk.framework.common.exception.FrameworkException;

/**
 * 보안 관련 예외의 최상위 클래스
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
public class SecurityException extends FrameworkException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 생성자
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     */
    public SecurityException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    /**
     * 생성자
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public SecurityException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    @Override
    public ErrorSeverity getSeverity() {
        return ErrorSeverity.HIGH;
    }
    
}
