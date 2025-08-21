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
 * @description  : 사용자 엔티티 - Rule에 따른 도메인 기반 패키지 구조와 Bean Validation 적용
 * @modification : 2025.08.21(프레임워크팀) Rule 업데이트에 따른 구현
 * @author       : SK Framework Team
 * @date         : 2025.08.21
 * @version      : 2.0
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

    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }

    // 비즈니스 메서드
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
        return String.format("User[id=%d, username='%s', email='%s', status='%s']",
                getId(), username, email, status);
    }
}
