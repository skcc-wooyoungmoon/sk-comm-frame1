package com.sk.framework.transaction.support;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * @className    : SpelKeyResolver
 * @description  : 어드바이스(멱등성/분산락 등)에서 SpEL 표현식으로 키를 계산하는 공통 유틸리티.
 *                 메소드 파라미터를 이름과 {@code #p0, #p1} 인덱스로 모두 바인딩합니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public final class SpelKeyResolver {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private SpelKeyResolver() {
    }

    /**
     * 조인포인트의 인자를 바탕으로 SpEL 표현식을 평가하여 문자열 키를 반환합니다.
     *
     * @param joinPoint  AOP 조인포인트
     * @param expression SpEL 표현식(빈 문자열이면 null 반환)
     * @return 평가된 키(문자열). 표현식이 비어있으면 null
     */
    public static String resolve(ProceedingJoinPoint joinPoint, String expression) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
            if (paramNames != null && i < paramNames.length) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        Expression exp = PARSER.parseExpression(expression);
        Object value = exp.getValue(context);
        return value != null ? value.toString() : null;
    }
}
