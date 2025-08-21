package com.sk.framework.security.exception;

/**
 * 유효하지 않은 토큰 예외
 * 
 * <p>JWT 토큰이 유효하지 않거나 만료된 경우 발생합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
public class InvalidTokenException extends SecurityException {
    
    private static final long serialVersionUID = 1L;
    private static final String ERROR_CODE = "INVALID_TOKEN";
    
    /**
     * 생성자
     * 
     * @param message 에러 메시지
     */
    public InvalidTokenException(String message) {
        super(ERROR_CODE, message);
    }
    
    /**
     * 생성자
     * 
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public InvalidTokenException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
    
    @Override
    public boolean isRetryable() {
        return false;
    }
    
}
