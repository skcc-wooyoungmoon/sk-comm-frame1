package com.sk.framework.data.config;

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
 * <p>SK Framework 데이터 접근 기능(JPA Repository/Auditing/트랜잭션)을 자동으로 설정합니다.</p>
 * <p>참고: {@code repositoryBaseClass}에 인터페이스({@code BaseRepository})를 지정하면 런타임 오류가
 * 발생하므로 제거했습니다. 커스텀 base 구현이 필요하면 {@code SimpleJpaRepository}를 상속한
 * 구현 클래스를 지정해야 합니다. 애플리케이션은 {@code BaseRepository}를 직접 상속해 사용할 수 있습니다.</p>
 *
 * @author SK Framework Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "sk.framework.data", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableJpaRepositories(basePackages = {"com.sk"})
@EntityScan(basePackages = {"com.sk"})
@EnableJpaAuditing
@EnableTransactionManagement
@ComponentScan(basePackages = "com.sk.framework.data")
public class DataAutoConfiguration {

}
