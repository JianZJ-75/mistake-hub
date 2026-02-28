package com.jianzj.mistake.hub.backend.annotation;

import com.jianzj.mistake.hub.backend.enums.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * 权限校验注解，通过 Role.level 判断权限
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-28
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreAuthorize {

    Role requiredRole();
}
