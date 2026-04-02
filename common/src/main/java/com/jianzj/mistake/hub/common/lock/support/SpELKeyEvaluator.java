package com.jianzj.mistake.hub.common.lock.support;

import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * <p>SpEL 表达式解析器，用于生成分布式锁的动态 key</p>
 *
 * @author jian.zhong
 * @since 2026-03-23
 */
@Component
public class SpELKeyEvaluator {

    private static final String DEFAULT_LOCK_KEY = "default_lock";

    private final ExpressionParser parser;

    public SpELKeyEvaluator() {

        this.parser = new SpelExpressionParser();
    }

    /**
     * 根据 SpEL 表达式和方法参数生成锁的 key
     */
    public String generateKey(String keyExpression, Method method, Object[] args) {

        if (StringUtils.isBlank(keyExpression)) {
            return DEFAULT_LOCK_KEY;
        }

        Object[] safeArgs = (args == null) ? new Object[0] : args;

        StandardEvaluationContext context = new StandardEvaluationContext();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            context.setVariable(parameters[i].getName(), safeArgs[i]);
        }

        Expression expression = parser.parseExpression(keyExpression);
        return expression.getValue(context, String.class);
    }
}
