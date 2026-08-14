package com.sk.framework.transaction.saga;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @className    : SagaContext
 * @description  : SAGA 단계 간 데이터를 공유하는 컨텍스트. 단계 실행 결과를 담아
 *                 이후 단계 및 보상 로직에서 참조합니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public class SagaContext {

    private final String sagaId;
    private final Map<String, Object> attributes = new HashMap<>();

    public SagaContext(String sagaId) {
        this.sagaId = sagaId;
    }

    public String getSagaId() {
        return sagaId;
    }

    public SagaContext put(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        return Optional.ofNullable((T) attributes.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T> T getRequired(String key) {
        Object value = attributes.get(key);
        if (value == null) {
            throw new IllegalStateException("사가 컨텍스트에 '" + key + "' 값이 없습니다.");
        }
        return (T) value;
    }
}
