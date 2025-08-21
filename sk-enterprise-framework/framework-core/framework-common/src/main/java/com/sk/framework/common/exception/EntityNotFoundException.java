package com.sk.framework.common.exception;

/**
 * @className    : EntityNotFoundException
 * @description  : 엔티티를 찾을 수 없을 때 발생하는 예외
 * @modification : 2025.08.21(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2025.08.21
 * @version      : 1.0
 */
public class EntityNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String message) {
        super(message);
    }
    
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public EntityNotFoundException(String entityName, Object id) {
        super(String.format("%s not found with id: %s", entityName, id));
    }
}
