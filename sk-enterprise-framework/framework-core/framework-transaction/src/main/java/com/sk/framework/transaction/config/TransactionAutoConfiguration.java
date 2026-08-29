package com.sk.framework.transaction.config;

import com.sk.framework.transaction.concurrency.RetryOnConflictAspect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.framework.transaction.idempotency.IdempotencyAspect;
import com.sk.framework.transaction.idempotency.IdempotencyStore;
import com.sk.framework.transaction.idempotency.InMemoryIdempotencyStore;
import com.sk.framework.transaction.idempotency.RedisIdempotencyStore;
import com.sk.framework.transaction.lock.DistributedLockAspect;
import com.sk.framework.transaction.lock.DistributedLockManager;
import com.sk.framework.transaction.lock.InMemoryDistributedLockManager;
import com.sk.framework.transaction.lock.RedisDistributedLockManager;
import com.sk.framework.transaction.outbox.KafkaMessagePublisher;
import com.sk.framework.transaction.outbox.LoggingMessagePublisher;
import com.sk.framework.transaction.outbox.MessagePublisher;
import com.sk.framework.transaction.outbox.OutboxProperties;
import com.sk.framework.transaction.outbox.OutboxRecorder;
import com.sk.framework.transaction.outbox.OutboxRelay;
import com.sk.framework.transaction.outbox.OutboxEventRepository;
import com.sk.framework.transaction.support.TransactionSupport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @className    : TransactionAutoConfiguration
 * @description  : 거래(트랜잭션) 패턴 모듈 자동 설정.
 *                 멱등성/재시도/분산락 어드바이스, 아웃박스, 프로그래밍 트랜잭션 지원 빈을 등록합니다.
 *                 모든 빈은 {@code @ConditionalOnMissingBean}이므로 프로젝트에서 손쉽게 교체할 수 있습니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@AutoConfiguration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(OutboxProperties.class)
public class TransactionAutoConfiguration {

    // ===== 멱등성(Idempotency) =====

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "sk.framework.transaction.idempotency", name = "store",
            havingValue = "memory", matchIfMissing = true)
    public IdempotencyStore idempotencyStore() {
        return new InMemoryIdempotencyStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyAspect idempotencyAspect(IdempotencyStore idempotencyStore) {
        return new IdempotencyAspect(idempotencyStore);
    }

    // ===== 동시성 재시도(Optimistic Retry) =====

    @Bean
    @ConditionalOnMissingBean
    public RetryOnConflictAspect retryOnConflictAspect() {
        return new RetryOnConflictAspect();
    }

    // ===== 분산 락(Distributed Lock) =====

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "sk.framework.transaction.lock", name = "type",
            havingValue = "memory", matchIfMissing = true)
    public DistributedLockManager distributedLockManager() {
        return new InMemoryDistributedLockManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedLockAspect distributedLockAspect(DistributedLockManager lockManager) {
        return new DistributedLockAspect(lockManager);
    }

    // ===== 프로그래밍 방식 트랜잭션 지원 =====

    @Bean
    @ConditionalOnBean(PlatformTransactionManager.class)
    @ConditionalOnMissingBean
    public TransactionSupport transactionSupport(PlatformTransactionManager transactionManager) {
        return new TransactionSupport(new TransactionTemplate(transactionManager));
    }

    // ===== 아웃박스(Transactional Outbox) =====

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "sk.framework.transaction.outbox", name = "publisher",
            havingValue = "logging", matchIfMissing = true)
    public MessagePublisher messagePublisher() {
        return new LoggingMessagePublisher();
    }

    // ===== 운영 구현 (선택 활성화): Redis 멱등/락, Kafka 아웃박스 =====

    /**
     * Redis 기반 멱등 저장소. 활성 조건:
     * classpath에 Redis + {@code sk.framework.transaction.idempotency.store=redis} + StringRedisTemplate 빈.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnProperty(prefix = "sk.framework.transaction.idempotency", name = "store", havingValue = "redis")
    static class RedisIdempotencyConfiguration {
        @Bean
        @ConditionalOnBean(StringRedisTemplate.class)
        @ConditionalOnMissingBean(IdempotencyStore.class)
        public IdempotencyStore redisIdempotencyStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
            return new RedisIdempotencyStore(redis, objectMapper, "idem:");
        }
    }

    /**
     * Redis 기반 분산 락. 활성 조건:
     * classpath에 Redis + {@code sk.framework.transaction.lock.type=redis} + StringRedisTemplate 빈.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnProperty(prefix = "sk.framework.transaction.lock", name = "type", havingValue = "redis")
    static class RedisLockConfiguration {
        @Bean
        @ConditionalOnBean(StringRedisTemplate.class)
        @ConditionalOnMissingBean(DistributedLockManager.class)
        public DistributedLockManager redisDistributedLockManager(StringRedisTemplate redis) {
            return new RedisDistributedLockManager(redis, "lock:");
        }
    }

    /**
     * Kafka 기반 아웃박스 발행. 활성 조건:
     * classpath에 spring-kafka + {@code sk.framework.transaction.outbox.publisher=kafka} + KafkaTemplate 빈.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(KafkaTemplate.class)
    @ConditionalOnProperty(prefix = "sk.framework.transaction.outbox", name = "publisher", havingValue = "kafka")
    static class KafkaPublisherConfiguration {
        @Bean
        @ConditionalOnBean(KafkaTemplate.class)
        @ConditionalOnMissingBean(MessagePublisher.class)
        public MessagePublisher kafkaMessagePublisher(KafkaTemplate<String, String> kafkaTemplate) {
            return new KafkaMessagePublisher(kafkaTemplate, "sk.outbox.", 10);
        }
    }

    /**
     * 아웃박스 관련 빈은 애플리케이션이 {@code OutboxEventRepository}를 스캔하도록 구성했을 때만 활성화됩니다.
     * (엔티티/리포지토리 스캔 경로에 {@code com.sk.framework.transaction.outbox} 추가 필요)
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(OutboxEventRepository.class)
    @ConditionalOnProperty(prefix = "sk.framework.transaction.outbox", name = "enabled", matchIfMissing = true)
    @EnableScheduling
    static class OutboxConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public OutboxRecorder outboxRecorder(OutboxEventRepository repository) {
            return new OutboxRecorder(repository);
        }

        @Bean
        @ConditionalOnMissingBean
        public OutboxRelay outboxRelay(OutboxEventRepository repository,
                                       MessagePublisher messagePublisher,
                                       OutboxProperties properties) {
            return new OutboxRelay(repository, messagePublisher, properties);
        }
    }
}
