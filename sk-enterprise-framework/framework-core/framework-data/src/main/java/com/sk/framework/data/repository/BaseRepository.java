package com.sk.framework.data.repository;

import com.sk.framework.common.entity.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * 기본 Repository 인터페이스
 * 
 * <p>모든 Repository가 상속받을 기본 인터페이스입니다.</p>
 * <p>JPA Repository, Specification, QueryDSL 기능을 모두 제공합니다.</p>
 * 
 * @param <T> 엔티티 타입
 * @param <ID> 식별자 타입
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity, ID> 
        extends JpaRepository<T, ID>, 
                JpaSpecificationExecutor<T>, 
                QuerydslPredicateExecutor<T> {
    
}
