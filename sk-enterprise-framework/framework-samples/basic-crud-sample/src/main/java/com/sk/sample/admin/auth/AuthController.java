package com.sk.sample.admin.auth;

import com.sk.framework.common.dto.ApiResponse;
import com.sk.framework.security.jwt.JwtTokenProvider;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @className    : AuthController
 * @description  : 관리자 콘솔 인증 API. 실제 JWT(HS512) 기반 인증으로 승격되었다.
 *                 - 로그인: 이메일 + BCrypt 비밀번호 검증 → JWT 액세스 토큰 발급(roles 클레임 포함)
 *                 - 내 정보: SecurityContext(JWT 필터가 설정)에서 현재 사용자 조회
 * @modification : 2026.08.14(프레임워크팀) 데모 토큰 → framework-security JWT로 승격
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 2.0
 */
@Tag(name = "인증", description = "JWT 기반 인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "로그인", description = "이메일/비밀번호로 인증 후 JWT 액세스 토큰을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .filter(u -> u.getPassword() != null && passwordEncoder.matches(request.getPassword(), u.getPassword()))
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new IllegalArgumentException("비활성/정지 상태의 사용자입니다.");
        }

        // 권한을 담은 Authentication으로 JWT 발급 (roles 클레임: ROLE_ADMIN 등)
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        String token = jwtTokenProvider.generateAccessToken(authentication);

        return ResponseEntity.ok(ApiResponse.success("로그인 성공", LoginResponse.builder()
                .token(token)
                .user(userService.convertToDto(user))
                .build()));
    }

    @Operation(summary = "내 정보", description = "JWT로 인증된 현재 사용자를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));
        return ResponseEntity.ok(ApiResponse.success(userService.convertToDto(user)));
    }
}
