package com.sk.sample.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * @className    : UserSearchRequest
 * @description  : 사용자 검색 요청 DTO - Rule에 따른 검색 조건 DTO
 * @modification : 2025.08.21(프레임워크팀) Rule 업데이트에 따른 구현
 * @author       : SK Framework Team
 * @date         : 2025.08.21
 * @version      : 2.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchRequest {

    private String username;
    private String email;
    private String phone;
    private String status;

    // 페이징 정보 (선택사항 - Pageable로 대체 가능)
    private Integer page;
    private Integer size;
    private String sort;
}
