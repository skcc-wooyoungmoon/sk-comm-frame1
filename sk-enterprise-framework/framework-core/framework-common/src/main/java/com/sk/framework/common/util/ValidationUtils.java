package com.sk.framework.common.util;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 검증 유틸리티 클래스
 * 
 * <p>프레임워크에서 자주 사용되는 검증 기능을 제공합니다.</p>
 * <p>null 체크, 빈 값 체크, 범위 검증 등을 지원합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
public final class ValidationUtils {
    
    private ValidationUtils() {
        // 유틸리티 클래스는 인스턴스 생성을 방지
    }
    
    /**
     * 객체가 null이 아님을 검증합니다.
     * 
     * @param <T> 객체 타입
     * @param object 검증할 객체
     * @param message 예외 메시지
     * @return 검증된 객체
     * @throws IllegalArgumentException 객체가 null인 경우
     */
    public static <T> T notNull(T object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
        return object;
    }
    
    /**
     * 객체가 null이 아님을 검증합니다.
     * 
     * @param <T> 객체 타입
     * @param object 검증할 객체
     * @param messageSupplier 예외 메시지 공급자
     * @return 검증된 객체
     * @throws IllegalArgumentException 객체가 null인 경우
     */
    public static <T> T notNull(T object, Supplier<String> messageSupplier) {
        if (object == null) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
        return object;
    }
    
    /**
     * 문자열이 null이거나 빈 문자열이 아님을 검증합니다.
     * 
     * @param str 검증할 문자열
     * @param message 예외 메시지
     * @return 검증된 문자열
     * @throws IllegalArgumentException 문자열이 null이거나 빈 문자열인 경우
     */
    public static String notEmpty(String str, String message) {
        if (StringUtils.isEmpty(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }
    
    /**
     * 문자열이 null이거나 공백이 아님을 검증합니다.
     * 
     * @param str 검증할 문자열
     * @param message 예외 메시지
     * @return 검증된 문자열
     * @throws IllegalArgumentException 문자열이 null이거나 공백인 경우
     */
    public static String notBlank(String str, String message) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }
    
    /**
     * 컬렉션이 null이거나 비어있지 않음을 검증합니다.
     * 
     * @param <T> 컬렉션 타입
     * @param collection 검증할 컬렉션
     * @param message 예외 메시지
     * @return 검증된 컬렉션
     * @throws IllegalArgumentException 컬렉션이 null이거나 비어있는 경우
     */
    public static <T extends Collection<?>> T notEmpty(T collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }
    
    /**
     * 맵이 null이거나 비어있지 않음을 검증합니다.
     * 
     * @param <T> 맵 타입
     * @param map 검증할 맵
     * @param message 예외 메시지
     * @return 검증된 맵
     * @throws IllegalArgumentException 맵이 null이거나 비어있는 경우
     */
    public static <T extends Map<?, ?>> T notEmpty(T map, String message) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }
    
    /**
     * 배열이 null이거나 비어있지 않음을 검증합니다.
     * 
     * @param <T> 배열 요소 타입
     * @param array 검증할 배열
     * @param message 예외 메시지
     * @return 검증된 배열
     * @throws IllegalArgumentException 배열이 null이거나 비어있는 경우
     */
    public static <T> T[] notEmpty(T[] array, String message) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }
        return array;
    }
    
    /**
     * 숫자가 양수임을 검증합니다.
     * 
     * @param number 검증할 숫자
     * @param message 예외 메시지
     * @return 검증된 숫자
     * @throws IllegalArgumentException 숫자가 0 이하인 경우
     */
    public static int positive(int number, String message) {
        if (number <= 0) {
            throw new IllegalArgumentException(message);
        }
        return number;
    }
    
    /**
     * 숫자가 양수임을 검증합니다.
     * 
     * @param number 검증할 숫자
     * @param message 예외 메시지
     * @return 검증된 숫자
     * @throws IllegalArgumentException 숫자가 0 이하인 경우
     */
    public static long positive(long number, String message) {
        if (number <= 0) {
            throw new IllegalArgumentException(message);
        }
        return number;
    }
    
    /**
     * 숫자가 0 이상임을 검증합니다.
     * 
     * @param number 검증할 숫자
     * @param message 예외 메시지
     * @return 검증된 숫자
     * @throws IllegalArgumentException 숫자가 음수인 경우
     */
    public static int notNegative(int number, String message) {
        if (number < 0) {
            throw new IllegalArgumentException(message);
        }
        return number;
    }
    
    /**
     * 숫자가 0 이상임을 검증합니다.
     * 
     * @param number 검증할 숫자
     * @param message 예외 메시지
     * @return 검증된 숫자
     * @throws IllegalArgumentException 숫자가 음수인 경우
     */
    public static long notNegative(long number, String message) {
        if (number < 0) {
            throw new IllegalArgumentException(message);
        }
        return number;
    }
    
    /**
     * 숫자가 지정된 범위 내에 있음을 검증합니다.
     * 
     * @param number 검증할 숫자
     * @param min 최소값 (포함)
     * @param max 최대값 (포함)
     * @param message 예외 메시지
     * @return 검증된 숫자
     * @throws IllegalArgumentException 숫자가 범위를 벗어난 경우
     */
    public static int inRange(int number, int min, int max, String message) {
        if (number < min || number > max) {
            throw new IllegalArgumentException(message);
        }
        return number;
    }
    
    /**
     * 숫자가 지정된 범위 내에 있음을 검증합니다.
     * 
     * @param number 검증할 숫자
     * @param min 최소값 (포함)
     * @param max 최대값 (포함)
     * @param message 예외 메시지
     * @return 검증된 숫자
     * @throws IllegalArgumentException 숫자가 범위를 벗어난 경우
     */
    public static long inRange(long number, long min, long max, String message) {
        if (number < min || number > max) {
            throw new IllegalArgumentException(message);
        }
        return number;
    }
    
    /**
     * 조건이 참임을 검증합니다.
     * 
     * @param condition 검증할 조건
     * @param message 예외 메시지
     * @throws IllegalArgumentException 조건이 거짓인 경우
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * 조건이 참임을 검증합니다.
     * 
     * @param condition 검증할 조건
     * @param messageSupplier 예외 메시지 공급자
     * @throws IllegalArgumentException 조건이 거짓인 경우
     */
    public static void isTrue(boolean condition, Supplier<String> messageSupplier) {
        if (!condition) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }
    
    /**
     * 조건이 거짓임을 검증합니다.
     * 
     * @param condition 검증할 조건
     * @param message 예외 메시지
     * @throws IllegalArgumentException 조건이 참인 경우
     */
    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * 두 객체가 같음을 검증합니다.
     * 
     * @param obj1 첫 번째 객체
     * @param obj2 두 번째 객체
     * @param message 예외 메시지
     * @throws IllegalArgumentException 두 객체가 다른 경우
     */
    public static void equals(Object obj1, Object obj2, String message) {
        if (!Objects.equals(obj1, obj2)) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * 두 객체가 다름을 검증합니다.
     * 
     * @param obj1 첫 번째 객체
     * @param obj2 두 번째 객체
     * @param message 예외 메시지
     * @throws IllegalArgumentException 두 객체가 같은 경우
     */
    public static void notEquals(Object obj1, Object obj2, String message) {
        if (Objects.equals(obj1, obj2)) {
            throw new IllegalArgumentException(message);
        }
    }
    
}
