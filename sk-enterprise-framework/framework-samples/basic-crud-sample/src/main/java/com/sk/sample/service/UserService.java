package com.sk.sample.service;

import com.sk.framework.common.service.BaseService;
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
 * @description  : 사용자 서비스 - Rule에 따른 트랜잭션 정책과 도메인별 서비스 구현
 * @modification : 2025.08.21(프레임워크팀) Rule 업데이트에 따른 구현
 * @author       : SK Framework Team
 * @date         : 2025.08.21
 * @version      : 2.0
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
        User.UserStatus status = searchRequest.getStatus() != null ?
            User.UserStatus.valueOf(searchRequest.getStatus()) : null;

        List<User> users = userRepository.findBySearchConditions(
            searchRequest.getUsername(),
            searchRequest.getEmail(),
            searchRequest.getPhone(),
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
        // 복잡한 검색 조건이 있는 경우 Specification 또는 QueryDSL 사용 권장
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(this::convertToDto);
    }

    /**
     * 사용자 생성
     * Rule에 따라 쓰기 메소드는 @Transactional로 override
     */
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        // 이메일 중복 검증
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(User.UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

    /**
     * 사용자 수정
     * Rule에 따라 쓰기 메소드는 @Transactional로 override
     */
    @Transactional
    public UserDto updateUser(Long id, UserUpdateRequest request) {
        User existingUser = findById(id);

        // 빌더 패턴을 사용한 불변 객체 업데이트
        User.UserBuilder builder = User.builder()
                .id(existingUser.getId())
                .username(request.getUsername() != null ? request.getUsername() : existingUser.getUsername())
                .email(request.getEmail() != null ? request.getEmail() : existingUser.getEmail())
                .phone(request.getPhone() != null ? request.getPhone() : existingUser.getPhone())
                .status(existingUser.getStatus());

        // 이메일 중복 검증 (자신 제외)
        if (request.getEmail() != null) {
            userRepository.findByEmail(request.getEmail())
                    .filter(user -> !user.getId().equals(id))
                    .ifPresent(user -> {
                        throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                    });
        }

        User updatedUser = userRepository.save(builder.build());
        return convertToDto(updatedUser);
    }

    /**
     * 사용자 상태 변경
     * Rule에 따라 쓰기 메소드는 @Transactional로 override
     */
    @Transactional
    public void changeUserStatus(Long id, String status) {
        User user = findById(id);
        User.UserStatus newStatus = User.UserStatus.valueOf(status.toUpperCase());

        User updatedUser = User.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(newStatus)
                .build();

        userRepository.save(updatedUser);
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
     * Entity를 DTO로 변환 (public으로 변경하여 Controller에서 접근 가능)
     */
    public UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null)
                .build();
    }
}
