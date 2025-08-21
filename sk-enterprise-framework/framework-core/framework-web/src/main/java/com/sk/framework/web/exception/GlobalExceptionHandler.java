package com.sk.framework.web.exception;

import com.sk.framework.common.dto.ApiResponse;
import com.sk.framework.common.exception.EntityNotFoundException;
import com.sk.framework.common.exception.FrameworkException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * @className    : GlobalExceptionHandler
 * @description  : 전역 예외 처리 핸들러 - Rule에 따라 Controller의 try-catch 대신 일괄 처리
 * @modification : 2025.08.21(프레임워크팀) Rule 업데이트에 따른 구현
 * @author       : SK Framework Team
 * @date         : 2025.08.21
 * @version      : 2.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 엔티티를 찾을 수 없는 경우
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("ENTITY_NOT_FOUND", ex.getMessage()));
    }

    /**
     * 프레임워크 비즈니스 예외
     */
    @ExceptionHandler(FrameworkException.class)
    public ResponseEntity<ApiResponse<Void>> handleFrameworkException(FrameworkException ex) {
        log.error("Framework exception: {}", ex.getMessage(), ex);
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("BUSINESS_ERROR", ex.getMessage()));
    }

    /**
     * Bean Validation 실패 (@Valid/@Validated)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("VALIDATION_ERROR", "입력값 검증에 실패했습니다.", errors));
    }

    /**
     * 일���적인 RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected runtime exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("INTERNAL_ERROR", "서�� 내부 오류가 발생했습니다."));
    }

    /**
     * 최상위 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("UNKNOWN_ERROR", "알 수 없는 오류가 발생했습니다."));
    }
}
