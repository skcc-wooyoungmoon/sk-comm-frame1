package com.sk.sample.service;

import com.sk.framework.common.service.BaseService;
import com.sk.framework.transaction.annotation.RetryOnConflict;
import com.sk.sample.dto.*;
import com.sk.sample.entity.User;
import com.sk.sample.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @className    : UserService
 * @description  : 사용자 서비스 - 트랜잭션 정책(readOnly 기본 + 쓰기 override)과
 *                 도메인 메소드 기반 수정으로 낙관적 락을 유지한다.
 *                 쓰기 메소드에는 {@code @RetryOnConflict}를 적용해 동시 수정 충돌 시 자동 재시도한다.
 * @modification : 2026.08.13(프레임워크팀) 낙관적 락 대응 및 role 처리 추가
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 3.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService extends BaseService<User, Long> {

    private final UserRepository userRepository;

    @Override
    protected JpaRepository<User, Long> getRepository() {
        return userRepository;
    }

    @Override
    protected String getEntityName() {
        return "User";
    }

    /**
     * 사용자 목록 조회 (검색 조건 포함)
     */
    public List<UserDto> findUsers(UserSearchRequest searchRequest) {
        User.UserStatus status = searchRequest.getStatus() != null && !searchRequest.getStatus().isBlank()
                ? User.UserStatus.valueOf(searchRequest.getStatus()) : null;

        List<User> users = userRepository.findBySearchConditions(
                emptyToNull(searchRequest.getUsername()),
                emptyToNull(searchRequest.getEmail()),
                emptyToNull(searchRequest.getPhone()),
                status
        );

        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 페이징된 사용자 목록 조회
     */
    public Page<UserDto> findUsers(UserSearchRequest searchRequest, Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(this::convertToDto);
    }

    /**
     * 사용자 상세 조회
     */
    public UserDto getUser(Long id) {
        return convertToDto(findById(id));
    }

    /**
     * 사용자 생성. 쓰기 메소드이므로 @Transactional override.
     */
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(User.UserStatus.ACTIVE)
                .role(parseRole(request.getRole(), User.UserRole.USER))
                .build();

        return convertToDto(userRepository.save(user));
    }

    /**
     * 사용자 수정. managed 엔티티를 직접 변경(dirty checking)하여 낙관적 락과 정합성을 유지한다.
     * 동시 수정 충돌 시 최대 3회 자동 재시도한다.
     */
    @RetryOnConflict(maxAttempts = 3)
    @Transactional
    public UserDto updateUser(Long id, UserUpdateRequest request) {
        User user = findById(id);

        // 이메일 중복 검증 (자신 제외)
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            userRepository.findByEmail(request.getEmail())
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                    });
        }

        user.updateProfile(request.getUsername(), request.getEmail(), request.getPhone());
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.changeRole(parseRole(request.getRole(), user.getRole()));
        }
        // 별도 save 불필요 - 트랜잭션 커밋 시 dirty checking으로 반영
        return convertToDto(user);
    }

    /**
     * 사용자 상태 변경.
     */
    @RetryOnConflict(maxAttempts = 3)
    @Transactional
    public UserDto changeUserStatus(Long id, String status) {
        User user = findById(id);
        user.changeStatus(User.UserStatus.valueOf(status.toUpperCase()));
        return convertToDto(user);
    }

    /**
     * 사용자 권한 변경.
     */
    @RetryOnConflict(maxAttempts = 3)
    @Transactional
    public UserDto changeUserRole(Long id, String role) {
        User user = findById(id);
        user.changeRole(User.UserRole.valueOf(role.toUpperCase()));
        return convertToDto(user);
    }

    /**
     * 사용자 삭제.
     */
    @Transactional
    public void deleteUser(Long id) {
        deleteById(id);
    }

    /**
     * 이메일로 사용자 조회
     */
    public UserDto findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::convertToDto)
                .orElse(null);
    }

    /**
     * Entity → DTO 변환
     */
    public UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .role(user.getRole().name())
                .version(user.getVersion())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null)
                .build();
    }

    private User.UserRole parseRole(String role, User.UserRole defaultRole) {
        if (role == null || role.isBlank()) {
            return defaultRole;
        }
        return User.UserRole.valueOf(role.toUpperCase());
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
