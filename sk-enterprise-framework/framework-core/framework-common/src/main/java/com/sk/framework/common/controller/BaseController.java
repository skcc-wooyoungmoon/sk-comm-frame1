package com.sk.framework.common.controller;

import com.sk.framework.common.dto.ApiResponse;
import com.sk.framework.common.dto.PageResponse;
import com.sk.framework.common.entity.BaseEntity;
import com.sk.framework.common.service.BaseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @className    : BaseController
 * @description  : 기본 CRUD 컨트롤러 - Rule에 따른 ResponseEntity 기반 표준 응답
 * @modification : 2025.08.21(프레임워크팀) Rule 업데이트에 따른 수정
 * @author       : SK Framework Team
 * @date         : 2025.08.21
 * @version      : 2.0
 */
public abstract class BaseController<T extends BaseEntity, ID> {
    
    /**
     * 해당 컨트롤러가 사용할 서비스를 반환합니다.
     * 
     * @return 서비스 인스턴스
     */
    protected abstract BaseService<T, ID> getService();
    
    /**
     * 전체 엔티티 목록을 페이징 처리하여 조회합니다.
     * 
     * @param pageable 페이징 정보
     * @return 페이징된 엔티티 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<T>>> findAll(Pageable pageable) {
        Page<T> page = getService().findAll(pageable);
        PageResponse<T> pageResponse = PageResponse.of(page);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }
    
    /**
     * 전체 엔티티 목록을 조회합��다. (페이징 없음)
     *
     * @return 전체 엔티티 목록
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<T>>> findAll() {
        List<T> entities = getService().findAll();
        return ResponseEntity.ok(ApiResponse.success(entities));
    }
    
    /**
     * 특정 ID의 엔티티를 조회합니다.
     * 
     * @param id 조회할 엔티티의 ID
     * @return 조회된 엔티티
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<T>> findById(@PathVariable ID id) {
        T entity = getService().findById(id);
        return ResponseEntity.ok(ApiResponse.success(entity));
    }
    
    /**
     * ���로운 엔티티를 생성합니다.
     *
     * @param entity 생성할 엔티티 데이터
     * @return 생성된 엔티티
     */
    @PostMapping
    public ResponseEntity<ApiResponse<T>> create(@Valid @RequestBody T entity) {
        T createdEntity = getService().save(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("엔티티가 성공적으로 생성되었습니다.", createdEntity));
    }
    
    /**
     * 기존 엔티티를 수정합니다.
     * 
     * @param id 수정할 엔티티의 ID
     * @param entity 수정할 엔티티 데이터
     * @return 수정된 엔티티
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<T>> update(@PathVariable ID id, 
                                                @Valid @RequestBody T entity) {
        T updatedEntity = getService().save(entity);
        return ResponseEntity.ok(ApiResponse.success("엔티티가 성공적으로 수정되었습니다.", updatedEntity));
    }
    
    /**
     * 엔티티를 삭제합니다.
     *
     * @param id 삭제할 엔티티의 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable ID id) {
        getService().deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("엔티티가 성공적으로 삭제되었습니다.", null));
    }
    
    /**
     * 특정 ID의 엔티티 존재 여부를 확인합니다.
     * 
     * @param id 확인할 엔티티의 ID
     * @return 존재 여부
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<ApiResponse<Boolean>> existsById(@PathVariable ID id) {
        boolean exists = getService().existsById(id);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }
    
    /**
     * 전체 엔티티 개수를 조회합니다.
     * 
     * @return 전체 개수
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> count() {
        long count = getService().count();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
