package com.sk.sample.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * @className    : UserDto
 * @description  : 사용자 응답 DTO - Rule에 따른 응답 전용 DTO
 * @modification : 2026.08.13(프레임워크팀) role/version 필드 추가
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 3.0
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
    private String role;
    private Long version;
    private String createdAt;
    private String updatedAt;
}
