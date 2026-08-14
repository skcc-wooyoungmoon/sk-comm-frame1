package com.sk.sample.config;

import com.sk.sample.entity.User;
import com.sk.sample.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @className    : DataInitializer
 * @description  : 데모용 초기 사용자 데이터 적재기. 로그인 가능하도록 BCrypt 비밀번호를 함께 시드한다.
 *                 데모 비밀번호는 모두 "password".
 * @modification : 2026.08.14(프레임워크팀) JWT 승격 - 비밀번호 시드 추가
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        String pw = passwordEncoder.encode("password");
        List<User> seeds = List.of(
                User.builder().username("관리자").email("admin@sk.com").phone("010-0000-0001")
                        .status(User.UserStatus.ACTIVE).role(User.UserRole.ADMIN).password(pw).build(),
                User.builder().username("김매니저").email("manager@sk.com").phone("010-0000-0002")
                        .status(User.UserStatus.ACTIVE).role(User.UserRole.MANAGER).password(pw).build(),
                User.builder().username("이사원").email("user1@sk.com").phone("010-0000-0003")
                        .status(User.UserStatus.ACTIVE).role(User.UserRole.USER).password(pw).build(),
                User.builder().username("박정지").email("user2@sk.com").phone("010-0000-0004")
                        .status(User.UserStatus.SUSPENDED).role(User.UserRole.USER).password(pw).build()
        );
        userRepository.saveAll(seeds);
        log.info("데모 사용자 {}명을 초기화했습니다. (비밀번호: password)", seeds.size());
    }
}
