package com.sk.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Basic CRUD 샘플 애플리케이션
 *
 * <p>SK Enterprise Framework의 공통/거래(트랜잭션) 패턴을 사용한 예제입니다.</p>
 * <p>사용자 관리(CRUD/상태/권한)와 주문 도메인(멱등성·아웃박스·분산락)을 함께 시연합니다.</p>
 *
 * <p>엔티티/리포지토리 스캔에 프레임워크 아웃박스 패키지를 포함하여
 * {@code OutboxRecorder}/{@code OutboxRelay}가 자동 활성화되도록 구성합니다.</p>
 *
 * @author SK Framework Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EntityScan(basePackages = {"com.sk.sample", "com.sk.framework.transaction.outbox"})
@EnableJpaRepositories(basePackages = {"com.sk.sample", "com.sk.framework.transaction.outbox"})
public class BasicCrudSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicCrudSampleApplication.class, args);
    }
}
