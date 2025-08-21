package com.sk.sample.repository;

import com.sk.sample.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @className    : UserRepository
 * @description  : 사용자 Repository - Rule에 따른 도메인별 Repository 구현
 * @modification : 2025.08.21(프레임워크팀) Rule 업데이트에 따른 구현
 * @author       : SK Framework Team
 * @date         : 2025.08.21
 * @version      : 2.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);

    /**
     * 사용자명으로 사용자 목록 조회
     */
    List<User> findByUsernameContaining(String username);

    /**
     * 상태별 사용자 목록 조회
     */
    List<User> findByStatus(User.UserStatus status);

    /**
     * 사용자명과 상태로 사용자 존재 여부 확인
     */
    boolean existsByUsernameAndStatus(String username, User.UserStatus status);

    /**
     * 복합 검색 조건으로 사용자 목록 조회
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:username IS NULL OR u.username LIKE %:username%) AND " +
           "(:email IS NULL OR u.email LIKE %:email%) AND " +
           "(:phone IS NULL OR u.phone LIKE %:phone%) AND " +
           "(:status IS NULL OR u.status = :status)")
    List<User> findBySearchConditions(@Param("username") String username,
                                     @Param("email") String email,
                                     @Param("phone") String phone,
                                     @Param("status") User.UserStatus status);
}
