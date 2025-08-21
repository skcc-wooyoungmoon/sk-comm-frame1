package com.sk.framework.data.config;

import com.sk.framework.data.repository.BaseRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 데이터 접근 자동 설정 클래스
 * 
 * <p>SK Framework 데이터 접근 기능을 자동으로 설정합니다.</p>
 * <p>JPA Repository, Auditing, 트랜잭션 관리를 구성합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "sk.framework.data", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableJpaRepositories(
        basePackages = {"com.sk"},
        repositoryBaseClass = BaseRepository.class
)
@EntityScan(basePackages = {"com.sk"})
@EnableJpaAuditing
@EnableTransactionManagement
@ComponentScan(basePackages = "com.sk.framework.data")
public class DataAutoConfiguration {
    
}
