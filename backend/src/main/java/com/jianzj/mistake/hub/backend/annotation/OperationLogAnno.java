package com.jianzj.mistake.hub.backend.annotation;

import com.jianzj.mistake.hub.backend.enums.OperationAction;
import com.jianzj.mistake.hub.backend.enums.OperationTargetType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * 操作日志注解，标注在需要自动记录操作日志的 Controller 方法上。
 * AOP 切面会在方法成功返回后自动记录操作信息。
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLogAnno {

    /** 操作类型 */
    OperationAction action();

    /** 目标类型 */
    OperationTargetType targetType();
}
