package com.sk.framework.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @className    : BaseEntity
 * @description  : 모든 엔티티의 기본 클래스 - 공통 필드 제공
 * @modification : 2025.08.21(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2025.08.21
 * @version      : 1.0
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * 낙관적 락(Optimistic Lock) 버전.
     * <p>동시 수정 시 JPA가 이 값을 비교하여 {@code OptimisticLockingFailureException}을 발생시킵니다.
     * {@code @RetryOnConflict} 어노테이션과 함께 사용하면 충돌 시 자동 재시도가 가능합니다.</p>
     */
    @Version
    @Column(name = "version")
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        // TODO: 실제 환경에서는 Spring Security Context에서 사용자 정보 가져오기
        if (this.createdBy == null) {
            this.createdBy = "system";
        }
        if (this.updatedBy == null) {
            this.updatedBy = "system";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        // TODO: 실제 환경에서는 Spring Security Context에서 사용자 정보 가져오기
        if (this.updatedBy == null) {
            this.updatedBy = "system";
        }
    }
}
