package com.sk.framework.data.specification;

import com.sk.framework.common.entity.BaseEntity;
import com.sk.framework.common.util.StringUtils;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 기본 Specification 유틸리티
 * 
 * <p>동적 쿼리 생성을 위한 공통 Specification 메서드들을 제공합니다.</p>
 * <p>재사용 가능한 쿼리 조건들을 정적 메서드로 제공합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
public final class BaseSpecification {
    
    private BaseSpecification() {
        // 유틸리티 클래스는 인스턴스 생성을 방지
    }
    
    /**
     * 필드가 특정 값과 같은지 확인하는 Specification을 생성합니다.
     * 
     * @param <T> 엔티티 타입
     * @param fieldName 필드명
     * @param value 비교할 값
     * @return Specification
     */
    public static <T extends BaseEntity> Specification<T> equal(String fieldName, Object value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(fieldName), value);
        };
    }
    
    /**
     * 필드가 특정 값과 다른지 확인하는 Specification을 생성합니다.
     * 
     * @param <T> 엔티티 타입
     * @param fieldName 필드명
     * @param value 비교할 값
     * @return Specification
     */
    public static <T extends BaseEntity> Specification<T> notEqual(String fieldName, Object value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.notEqual(root.get(fieldName), value);
        };
    }
    
    /**
     * 문자열 필드가 특정 값을 포함하는지 확인하는 Specification을 생성합니다.
     * 
     * @param <T> 엔티티 타입
     * @param fieldName 필드명
     * @param value 검색할 문자열
     * @return Specification
     */
    public static <T extends BaseEntity> Specification<T> like(String fieldName, String value) {
        return (root, query, criteriaBuilder) -> {
            if (StringUtils.isBlank(value)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(fieldName)), 
                    "%" + value.toLowerCase() + "%"
            );
        };
    }
    
    /**
     * 필드가 특정 값들 중 하나인지 확인하는 Specification을 생성합니다.
     * 
     * @param <T> 엔티티 타입
     * @param fieldName 필드명
     * @param values 비교할 값들
     * @return Specification
     */
    public static <T extends BaseEntity> Specification<T> in(String fieldName, Collection<?> values) {
        return (root, query, criteriaBuilder) -> {
            if (values == null || values.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get(fieldName).in(values);
        };
    }
    
    /**
     * 숫자 필드가 특정 값보다 큰지 확인하는 Specification을 생성합니다.
     * 
     * @param <T> 엔티티 타입
     * @param <Y> 비교할 값의 타입
     * @param fieldName 필드명
     * @param value 비교할 값
     * @return Specification
     */
    public static <T extends BaseEntity, Y extends Comparable<? super Y>> 
    Specification<T> greaterThan(String fieldName, Y value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThan(root.get(fieldName), value);
        };
    }
    
    /**
     * 숫자 필드가 특정 값보다 크거나 같은지 확인하는 Specification을 생성합니다.
     * 
     * @param <T> 엔티티 타입
     * @param <Y> 비교할 값의 타입
     * @param fieldName 필드명
     * @param value 비교할 값
     * @return Specification
     */
    public static <T extends BaseEntity, Y extends Comparable<? super Y>> 
    Specification<T> greaterThanOrEqual(String fieldName, Y value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName), value);
        };
    }
    
    /**
     * 숫자 필드가 특정 값보다 작은지 확인하는 Specification을 생성합니다.
     * 
     * @param <T> 엔티티 타입
     * @param <Y> 비교할 값의 타입
     * @param fieldName 필드명
     * @param value 비교할 값
     * @return Specification
     */
    public static <T extends BaseEntity, Y extends Comparable<? super Y>> 
    Specification<T> lessThan(String fieldName, Y value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThan(root.get(fieldName), value);
        };
    }
    
    /**
     * 숫자 필드가 특정 값보다 작거나 같은지 확인하는 Specification을 생성합니다.
     * 
     * @param <T> 엔티티 타입
     * @param <Y> 비교할 값의 타입
     * @param fieldName 필드명
     * @param value 비교할 값
     * @return Specification
     */
    public static <T extends BaseEntity, Y extends Comparable<? super Y>> 
    Specification<T> lessThanOrEqual(String fieldName, Y value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get(fieldName), value);
        };
    }
    
    /**
     * 필드가 특정 범위 안에 있는지 확인하는 Specification을 생성합니다.
     * 
     * @param <T> 엔티티 타입
     * @param <Y> 비교할 값의 타입
     * @param fieldName 필드명
     * @param from 시작값
     * @param to 끝값
     * @return Specification
     */
    public static <T extends BaseEntity, Y extends Comparable<? super Y>> 
    Specification<T> between(String fieldName, Y from, Y to) {
        return (root, query, criteriaBuilder) -> {
            if (from == null && to == null) {
                return criteriaBuilder.conjunction();
            }
            if (from == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get(fieldName), to);
            }
            if (to == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName), from);
            }
            return criteriaBuilder.between(root.get(fieldName), from, to);
        };
    }
    
    /**
     * 날짜 필드가 특정 날짜 범위 안에 있는지 확인하는 Specification을 생성합니다.
     * 
     * @param <T> 엔티티 타입
     * @param fieldName 필드명
     * @param from 시작 날짜
     * @param to 끝 날짜
     * @return Specification
     */
    public static <T extends BaseEntity> Specification<T> dateBetween(String fieldName, 
                                                                      LocalDateTime from, 
                                                                      LocalDateTime to) {
        return between(fieldName, from, to);
    }
    
    /**
     * 필드가 null인지 확인하는 Specification을 생성합니다.
     * 
     * @param <T> 엔티티 타입
     * @param fieldName 필드명
     * @return Specification
     */
    public static <T extends BaseEntity> Specification<T> isNull(String fieldName) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get(fieldName));
    }
    
    /**
     * 필드가 null이 아닌지 확인하는 Specification을 생성합니다.
     * 
     * @param <T> 엔티티 타입
     * @param fieldName 필드명
     * @return Specification
     */
    public static <T extends BaseEntity> Specification<T> isNotNull(String fieldName) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get(fieldName));
    }
    
    /**
     * 여러 Specification을 AND 조건으로 결합합니다.
     * 
     * @param <T> 엔티티 타입
     * @param specifications 결합할 Specification들
     * @return 결합된 Specification
     */
    @SafeVarargs
    public static <T extends BaseEntity> Specification<T> and(Specification<T>... specifications) {
        return (root, query, criteriaBuilder) -> {
            Predicate[] predicates = new Predicate[specifications.length];
            for (int i = 0; i < specifications.length; i++) {
                predicates[i] = specifications[i].toPredicate(root, query, criteriaBuilder);
            }
            return criteriaBuilder.and(predicates);
        };
    }
    
    /**
     * 여러 Specification을 OR 조건으로 결합합니다.
     * 
     * @param <T> 엔티티 타입
     * @param specifications 결합할 Specification들
     * @return 결합된 Specification
     */
    @SafeVarargs
    public static <T extends BaseEntity> Specification<T> or(Specification<T>... specifications) {
        return (root, query, criteriaBuilder) -> {
            Predicate[] predicates = new Predicate[specifications.length];
            for (int i = 0; i < specifications.length; i++) {
                predicates[i] = specifications[i].toPredicate(root, query, criteriaBuilder);
            }
            return criteriaBuilder.or(predicates);
        };
    }
    
}
