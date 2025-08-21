package com.sk.framework.common.util;

import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 문자열 유틸리티 클래스
 * 
 * <p>프레임워크에서 자주 사용되는 문자열 처리 기능을 제공합니다.</p>
 * 
 * @author SK Framework Team
 * @since 1.0.0
 */
public final class StringUtils {
    
    private StringUtils() {
        // 유틸리티 클래스는 인스턴스 생성을 방지
    }
    
    /**
     * 문자열이 null이거나 빈 문자열인지 확인합니다.
     * 
     * @param str 확인할 문자열
     * @return null이거나 빈 문자열인 경우 true
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * 문자열이 null이 아니고 빈 문자열이 아닌지 확인합니다.
     * 
     * @param str 확인할 문자열
     * @return null이 아니고 빈 문자열이 아닌 경우 true
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 문자열이 null이거나 공백만 있는지 확인합니다.
     * 
     * @param str 확인할 문자열
     * @return null이거나 공백만 있는 경우 true
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 문자열이 null이 아니고 공백이 아닌지 확인합니다.
     * 
     * @param str 확인할 문자열
     * @return null이 아니고 공백이 아닌 경우 true
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * 문자열을 안전하게 trim합니다. (null 안전)
     * 
     * @param str trim할 문자열
     * @return trim된 문자열 (null인 경우 null 반환)
     */
    public static String safeTrim(String str) {
        return str != null ? str.trim() : null;
    }
    
    /**
     * 문자열의 첫 글자를 대문자로 변환합니다.
     * 
     * @param str 변환할 문자열
     * @return 첫 글자가 대문자인 문자열
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    /**
     * 문자열의 첫 글자를 소문자로 변환합니다.
     * 
     * @param str 변환할 문자열
     * @return 첫 글자가 소문자인 문자열
     */
    public static String uncapitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
    
    /**
     * CamelCase를 snake_case로 변환합니다.
     * 
     * @param camelCase CamelCase 문자열
     * @return snake_case 문자열
     */
    public static String camelToSnake(String camelCase) {
        if (isEmpty(camelCase)) {
            return camelCase;
        }
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    /**
     * snake_case를 CamelCase로 변환합니다.
     * 
     * @param snakeCase snake_case 문자열
     * @return CamelCase 문자열
     */
    public static String snakeToCamel(String snakeCase) {
        if (isEmpty(snakeCase)) {
            return snakeCase;
        }
        
        StringBuilder result = new StringBuilder();
        boolean capitalize = false;
        
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalize = true;
            } else if (capitalize) {
                result.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * 단수형을 복수형으로 변환합니다. (영어 기본 규칙)
     * 
     * @param singular 단수형 문자열
     * @return 복수형 문자열
     */
    public static String pluralize(String singular) {
        if (isEmpty(singular)) {
            return singular;
        }
        
        String lower = singular.toLowerCase();
        
        // 불규칙 변화
        if (lower.equals("person")) {
            return "people";
        } else if (lower.equals("child")) {
            return "children";
        } else if (lower.equals("man")) {
            return "men";
        } else if (lower.equals("woman")) {
            return "women";
        }
        
        // 규칙 변화
        if (lower.endsWith("y")) {
            return singular.substring(0, singular.length() - 1) + "ies";
        } else if (lower.endsWith("s") || lower.endsWith("sh") || 
                   lower.endsWith("ch") || lower.endsWith("x") || lower.endsWith("z")) {
            return singular + "es";
        } else if (lower.endsWith("f")) {
            return singular.substring(0, singular.length() - 1) + "ves";
        } else if (lower.endsWith("fe")) {
            return singular.substring(0, singular.length() - 2) + "ves";
        } else {
            return singular + "s";
        }
    }
    
    /**
     * 문자열을 지정된 길이로 자릅니다.
     * 
     * @param str 자를 문자열
     * @param maxLength 최대 길이
     * @return 잘린 문자열
     */
    public static String truncate(String str, int maxLength) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }
    
    /**
     * 문자열을 지정된 길이로 자르고 생략 표시를 추가합니다.
     * 
     * @param str 자를 문자열
     * @param maxLength 최대 길이
     * @param ellipsis 생략 표시
     * @return 잘린 문자열 + 생략 표시
     */
    public static String truncate(String str, int maxLength, String ellipsis) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }
        
        ellipsis = Objects.requireNonNullElse(ellipsis, "...");
        int truncateLength = Math.max(0, maxLength - ellipsis.length());
        
        return str.substring(0, truncateLength) + ellipsis;
    }
    
    /**
     * 컬렉션의 요소들을 구분자로 연결합니다.
     * 
     * @param collection 연결할 컬렉션
     * @param delimiter 구분자
     * @return 연결된 문자열
     */
    public static String join(Collection<?> collection, String delimiter) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (Object obj : collection) {
            if (!first) {
                sb.append(delimiter);
            }
            sb.append(obj.toString());
            first = false;
        }
        
        return sb.toString();
    }
    
    /**
     * 이메일 형식이 유효한지 확인합니다.
     * 
     * @param email 확인할 이메일 주소
     * @return 유효한 이메일 형식인 경우 true
     */
    public static boolean isValidEmail(String email) {
        if (isBlank(email)) {
            return false;
        }
        
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.matches(emailPattern, email);
    }
    
    /**
     * 전화번호 형식이 유효한지 확인합니다. (한국 형식)
     * 
     * @param phoneNumber 확인할 전화번호
     * @return 유효한 전화번호 형식인 경우 true
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (isBlank(phoneNumber)) {
            return false;
        }
        
        // 한국 전화번호 패턴 (010-1234-5678, 02-123-4567 등)
        String phonePattern = "^(01[016789]|02|0[3-9][0-9])-?[0-9]{3,4}-?[0-9]{4}$";
        return Pattern.matches(phonePattern, phoneNumber.replaceAll("[\\s-]", ""));
    }
    
}
