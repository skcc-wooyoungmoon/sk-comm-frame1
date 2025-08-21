package com.sk.framework.security.exception;

/**
 * 권한 부족 예외
 * 
 * <p>사용자가 필요한 권한을 가지고 있지 않을 때 발생합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
public class InsufficientPrivilegesException extends SecurityException {
    
    private static final long serialVersionUID = 1L;
    private static final String ERROR_CODE = "INSUFFICIENT_PRIVILEGES";
    
    /**
     * 생성자
     * 
     * @param message 에러 메시지
     */
    public InsufficientPrivilegesException(String message) {
        super(ERROR_CODE, message);
    }
    
    /**
     * 생성자
     * 
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public InsufficientPrivilegesException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
    
}
