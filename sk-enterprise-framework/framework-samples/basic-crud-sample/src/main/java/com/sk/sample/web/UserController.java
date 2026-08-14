package com.sk.sample.web;

import com.sk.framework.common.dto.ApiResponse;
import com.sk.framework.common.dto.PageResponse;
import com.sk.sample.dto.*;
import com.sk.sample.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @className    : UserController
 * @description  : 사용자 관리 REST 컨트롤러.
 *                 ResponseEntity + ApiResponse 표준 응답, Bean Validation, GlobalExceptionHandler
 *                 정책을 따른다. 프론트엔드(user-admin)의 사용자/권한 관리 화면과 연동된다.
 * @modification : 2026.08.13(프레임워크팀) 최초 구현(기존 빈 파일 대체)
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Tag(name = "사용자 관리", description = "사용자 CRUD 및 상태/권한 관리 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "사용자 목록 조회", description = "페이징된 사용자 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> getUsers(
            @Parameter(description = "검색 조건") UserSearchRequest searchRequest,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserDto> page = userService.findUsers(searchRequest, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @Operation(summary = "사용자 검색", description = "검색 조건에 맞는 사용자 전체 목록을 조회합니다.(페이징 없음)")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserDto>>> searchUsers(UserSearchRequest searchRequest) {
        return ResponseEntity.ok(ApiResponse.success(userService.findUsers(searchRequest)));
    }

    @Operation(summary = "사용자 상세 조회", description = "ID로 단일 사용자를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUser(id)));
    }

    @Operation(summary = "사용자 생성", description = "신규 사용자를 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserDto created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("사용자가 생성되었습니다.", created));
    }

    @Operation(summary = "사용자 수정", description = "사용자 정보를 수정합니다.(낙관적 락 + 충돌 시 자동 재시도)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("사용자가 수정되었습니다.", userService.updateUser(id, request)));
    }

    @Operation(summary = "사용자 상태 변경", description = "ACTIVE/INACTIVE/SUSPENDED 상태를 변경합니다.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserDto>> changeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(ApiResponse.success("상태가 변경되었습니다.", userService.changeUserStatus(id, status)));
    }

    @Operation(summary = "사용자 권한 변경", description = "ADMIN/MANAGER/USER 권한을 변경합니다.")
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserDto>> changeRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String role = body.get("role");
        return ResponseEntity.ok(ApiResponse.success("권한이 변경되었습니다.", userService.changeUserRole(id, role)));
    }

    @Operation(summary = "사용자 삭제", description = "사용자를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("사용자가 삭제되었습니다.", null));
    }
}
