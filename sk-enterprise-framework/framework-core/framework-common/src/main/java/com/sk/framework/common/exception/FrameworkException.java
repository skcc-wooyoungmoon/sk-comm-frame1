package com.sk.framework.common.exception;

/**
 * 프레임워크 최상위 예외 클래스
 * 
 * <p>SK Framework의 모든 예외는 이 클래스를 상속받습니다.</p>
 * <p>에러 코드와 메시지를 포함하여 체계적인 예외 처리를 지원합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
public abstract class FrameworkException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 에러 코드
     */
    private final String errorCode;
    
    /**
     * 생성자
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     */
    protected FrameworkException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 생성자
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    protected FrameworkException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 에러 코드를 반환합니다.
     * 
     * @return 에러 코드
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * 에러의 심각도를 반환합니다.
     * 서브클래스에서 오버라이드하여 사용합니다.
     * 
     * @return 에러 심각도
     */
    public ErrorSeverity getSeverity() {
        return ErrorSeverity.MEDIUM;
    }
    
    /**
     * 에러가 재시도 가능한지 여부를 반환합니다.
     * 서브클래스에서 오버라이드하여 사용합니다.
     * 
     * @return 재시도 가능 여부
     */
    public boolean isRetryable() {
        return false;
    }
    
    /**
     * 에러 심각도 열거형
     */
    public enum ErrorSeverity {
        LOW,      // 낮음: 로그만 기록
        MEDIUM,   // 보통: 로그 기록 + 모니터링
        HIGH,     // 높음: 로그 기록 + 모니터링 + 알림
        CRITICAL  // 심각: 로그 기록 + 모니터링 + 즉시 알림
    }
    
}
