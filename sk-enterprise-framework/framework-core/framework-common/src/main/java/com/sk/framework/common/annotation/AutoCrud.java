package com.sk.framework.common.annotation;

import java.lang.annotation.*;

/**
 * 자동 CRUD API 생성을 위한 어노테이션
 * 
 * <p>이 어노테이션을 Entity 클래스에 추가하면 자동으로 CRUD API가 생성됩니다.</p>
 * 
 * <pre>
 * {@code
 * @Entity
 * @AutoCrud(path = "/api/users", enableDelete = false)
 * public class User extends BaseEntity {
 *     // 필드들...
 * }
 * }
 * </pre>
 * 
 * 생성되는 API:
 * <ul>
 *   <li>GET /api/users - 전체 조회</li>
 *   <li>GET /api/users/{id} - 단건 조회</li>
 *   <li>POST /api/users - 생성</li>
 *   <li>PUT /api/users/{id} - 수정</li>
 *   <li>DELETE /api/users/{id} - 삭제 (enableDelete가 true인 경우)</li>
 * </ul>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoCrud {
    
    /**
     * API 경로를 지정합니다.
     * 기본값은 엔티티명의 소문자 복수형입니다.
     * 
     * @return API 경로
     */
    String path() default "";
    
    /**
     * 생성(Create) API 활성화 여부
     * 
     * @return 생성 API 활성화 여부
     */
    boolean enableCreate() default true;
    
    /**
     * 조회(Read) API 활성화 여부
     * 
     * @return 조회 API 활성화 여부
     */
    boolean enableRead() default true;
    
    /**
     * 수정(Update) API 활성화 여부
     * 
     * @return 수정 API 활성화 여부
     */
    boolean enableUpdate() default true;
    
    /**
     * 삭제(Delete) API 활성화 여부
     * 
     * @return 삭제 API 활성화 여부
     */
    boolean enableDelete() default true;
    
    /**
     * 페이징 지원 여부
     * 
     * @return 페이징 지원 여부
     */
    boolean enablePaging() default true;
    
    /**
     * 정렬 지원 여부
     * 
     * @return 정렬 지원 여부
     */
    boolean enableSorting() default true;
    
    /**
     * 검색 지원 여부
     * 
     * @return 검색 지원 여부
     */
    boolean enableSearch() default true;
    
}
