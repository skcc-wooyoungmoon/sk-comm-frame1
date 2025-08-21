package com.sk.framework.common.service;

import com.sk.framework.common.entity.BaseEntity;
import com.sk.framework.common.exception.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @className    : BaseService
 * @description  : 기본 서비스 클래스 - Rule에 따른 트랜잭션 정책 적용
 * @modification : 2025.08.21(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2025.08.21
 * @version      : 1.0
 */
@Transactional(readOnly = true)
public abstract class BaseService<T extends BaseEntity, ID> {

    /**
     * 해당 서비스가 사용할 Repository를 반환합니다.
     *
     * @return Repository 인스턴스
     */
    protected abstract JpaRepository<T, ID> getRepository();

    /**
     * 엔티티명을 반환합니다. (예외 메시지에 사용)
     *
     * @return 엔티티명
     */
    protected abstract String getEntityName();

    /**
     * 전체 엔티티 목록을 페이징 처리하여 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 페이징된 엔티티 목록
     */
    public Page<T> findAll(Pageable pageable) {
        return getRepository().findAll(pageable);
    }

    /**
     * 전체 엔티티 목록을 조회합니다.
     *
     * @return 전체 엔티티 목록
     */
    public List<T> findAll() {
        return getRepository().findAll();
    }

    /**
     * 특정 ID의 엔티티를 조회합니다.
     *
     * @param id 조회할 엔티티의 ID
     * @return 조회된 엔티티
     * @throws EntityNotFoundException 엔티티를 찾을 수 없는 경우
     */
    public T findById(ID id) {
        return getRepository().findById(id)
                .orElseThrow(() -> new EntityNotFoundException(getEntityName() + " not found with id: " + id));
    }

    /**
     * 엔티티를 저장합니다. (생성/수정)
     * Rule에 따라 쓰기 메소드는 @Transactional로 override
     *
     * @param entity 저장할 엔티티
     * @return 저장된 엔티티
     */
    @Transactional
    public T save(T entity) {
        return getRepository().save(entity);
    }

    /**
     * 엔티티를 삭제합니다.
     * Rule에 따라 쓰기 메소드는 @Transactional로 override
     *
     * @param id 삭제할 엔티티의 ID
     * @throws EntityNotFoundException 엔티티를 찾을 수 없는 경우
     */
    @Transactional
    public void deleteById(ID id) {
        if (!getRepository().existsById(id)) {
            throw new EntityNotFoundException(getEntityName() + " not found with id: " + id);
        }
        getRepository().deleteById(id);
    }

    /**
     * 특정 ID의 엔티티 존재 여부를 확인합니다.
     *
     * @param id 확인할 엔티티의 ID
     * @return 존재 여부
     */
    public boolean existsById(ID id) {
        return getRepository().existsById(id);
    }

    /**
     * 전체 엔티티 개수를 조회합니다.
     *
     * @return 전체 개수
     */
    public long count() {
        return getRepository().count();
    }
}
