package com.sk.sample.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * @className    : UserDto
 * @description  : 사용자 응답 DTO - Rule에 따른 응답 전용 DTO
 * @modification : 2025.08.21(프레임워크팀) Rule 업데이트에 따른 구현
 * @author       : SK Framework Team
 * @date         : 2025.08.21
 * @version      : 2.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String status;
    private String createdAt;
    private String updatedAt;
}
