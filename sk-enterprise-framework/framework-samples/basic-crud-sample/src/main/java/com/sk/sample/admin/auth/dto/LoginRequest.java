package com.sk.sample.admin.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * @className    : LoginRequest
 * @description  : 로그인 요청 DTO.
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
