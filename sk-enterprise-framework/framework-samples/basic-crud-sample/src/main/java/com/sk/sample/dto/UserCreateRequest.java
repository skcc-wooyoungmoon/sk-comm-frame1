package com.sk.sample.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * @className    : UserCreateRequest
 * @description  : 사용자 생성 요청 DTO - Rule에 따른 요청 전용 DTO와 Bean Validation 적용
 * @modification : 2025.08.21(프레임워크팀) Rule 업데이트에 따른 구현
 * @author       : SK Framework Team
 * @date         : 2025.08.21
 * @version      : 2.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {

    @NotBlank(message = "사용자명은 필수입니다.")
    @Size(max = 50, message = "사용자명은 50자를 초과할 수 없습니다.")
    private String username;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
    private String email;

    @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다.")
    private String phone;

    /** 권한 (미지정 시 USER). 예: ADMIN, MANAGER, USER */
    private String role;
}
