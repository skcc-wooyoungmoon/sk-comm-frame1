package com.sk.sample.admin.auth.dto;

import com.sk.sample.dto.UserDto;
import lombok.*;

/**
 * @className    : LoginResponse
 * @description  : 로그인 응답 DTO. 데모 토큰과 인증된 사용자 정보를 반환한다.
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Getter
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private UserDto user;
}
