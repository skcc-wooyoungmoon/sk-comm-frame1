package com.sk.sample.admin.auth;

import com.sk.framework.common.dto.ApiResponse;
import com.sk.sample.admin.auth.dto.LoginRequest;
import com.sk.sample.admin.auth.dto.LoginResponse;
import com.sk.sample.dto.UserDto;
import com.sk.sample.entity.User;
import com.sk.sample.repository.UserRepository;
import com.sk.sample.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @className    : AuthController
 * @description  : 관리자 콘솔용 데모 인증 API.
 *                 ★ 데모 목적: 시드 사용자 이메일로 인증하며 비밀번호는 검증하지 않는다.
 *                 실제 운영에서는 framework-security(JWT) + 비밀번호 해시 검증으로 대체할 것.
 *                 반환 토큰은 role 기반 UI 게이팅 데모를 위한 값이다.
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Tag(name = "인증", description = "관리자 콘솔 데모 인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;

    @Operation(summary = "로그인", description = "시드 사용자 이메일로 로그인합니다.(데모: 비밀번호 미검증)")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new IllegalArgumentException("비활성/정지 상태의 사용자입니다.");
        }
        UserDto dto = userService.convertToDto(user);
        String token = issueToken(user);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", LoginResponse.builder()
                .token(token).user(dto).build()));
    }

    @Operation(summary = "내 정보", description = "Authorization: Bearer <token> 으로 현재 사용자를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(@RequestHeader("Authorization") String authorization) {
        String email = parseEmail(authorization);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));
        return ResponseEntity.ok(ApiResponse.success(userService.convertToDto(user)));
    }

    // ===== 데모 토큰 유틸 (운영에서는 JWT 서명 검증으로 대체) =====

    private String issueToken(User user) {
        String raw = user.getEmail() + ":" + user.getRole().name() + ":" + System.currentTimeMillis();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String parseEmail(String authorization) {
        try {
            String token = authorization.replaceFirst("(?i)^Bearer ", "").trim();
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            return decoded.split(":", 2)[0];
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
    }
}
