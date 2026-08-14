package com.sk.sample.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @className    : UserUpdateRequest
 * @description  : 사용자 수정 요청 DTO - Rule에 따른 요청 전용 DTO와 Bean Validation 적용
 * @modification : 2025.08.21(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2025.08.21
 * @version      : 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @Size(max = 50, message = "사용자명은 50자를 초과할 수 없습니다.")
    private String username;

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
    private String email;

    @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다.")
    private String phone;

    /** 권한 변경 시 지정 (예: ADMIN, MANAGER, USER). null이면 변경하지 않음 */
    private String role;
}
