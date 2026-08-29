package com.sk.sample.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * @className    : RefreshRequest
 * @description  : 액세스 토큰 재발급 요청 DTO.
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshRequest {
    @NotBlank(message = "refreshToken은 필수입니다.")
    private String refreshToken;
}
