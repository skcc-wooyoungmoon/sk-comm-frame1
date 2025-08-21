package com.sk.framework.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @className    : ApiResponse
 * @description  : 표준 API 응답 구조 - Rule에 따른 통일된 응답 포맷
 * @modification : 2025.08.21(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2025.08.21
 * @version      : 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;

    /**
     * 성공 응답 생성 (데이터 없음)
     */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("요청이 성공적으로 처리되었습니다.")
                .build();
    }

    /**
     * 성공 응답 생성 (데이터 포함)
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("요청이 성공적으로 처리되었습니다.")
                .data(data)
                .build();
    }

    /**
     * 성공 응답 생성 (메시지 커스터마이징)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 실패 응답 생성
     */
    public static <T> ApiResponse<T> failure(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 실패 응답 생성 (데이터 포함)
     */
    public static <T> ApiResponse<T> failure(String code, String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
