package com.sk.sample.entity;

import com.sk.framework.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @className    : User
 * @description  : 사용자 엔티티 - 도메인 기반 패키지 구조와 Bean Validation 적용.
 *                 수정은 setter가 아닌 도메인 메소드로만 수행하여 낙관적 락(@Version)과
 *                 정합성을 유지한다.
 * @modification : 2026.08.13(프레임워크팀) role 필드 및 도메인 수정 메소드 추가
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 3.0
 */
@Entity
@Table(name = "tb_user")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @NotBlank(message = "사용자명은 필수입니다.")
    @Size(max = 50, message = "사용자명은 50자를 초과할 수 없습니다.")
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다.")
    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    /**
     * 사용자 상태.
     */
    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }

    /**
     * 사용자 권한(역할).
     */
    public enum UserRole {
        ADMIN, MANAGER, USER
    }

    // ===== 도메인 수정 메소드 (managed 엔티티를 직접 변경하여 낙관적 락 유지) =====

    /**
     * 프로필(사용자명/전화번호/이메일)을 수정합니다. null 인자는 변경하지 않습니다.
     */
    public void updateProfile(String username, String email, String phone) {
        if (username != null) {
            this.username = username;
        }
        if (email != null) {
            this.email = email;
        }
        if (phone != null) {
            this.phone = phone;
        }
    }

    /**
     * 권한을 변경합니다.
     */
    public void changeRole(UserRole role) {
        this.role = role;
    }

    /**
     * 상태를 변경합니다.
     */
    public void changeStatus(UserStatus status) {
        this.status = status;
    }

    // ===== 상태 편의 메소드 =====

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return String.format("User[id=%d, username='%s', email='%s', status='%s', role='%s']",
                getId(), username, email, status, role);
    }
}
