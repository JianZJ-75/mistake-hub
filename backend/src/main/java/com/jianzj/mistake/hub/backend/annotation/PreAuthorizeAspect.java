package com.jianzj.mistake.hub.backend.annotation;

import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.service.AccountService;
import com.jianzj.mistake.hub.common.convention.exception.ApiAuthorizeException;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * <p>
 * 权限校验切面，拦截 Controller 中 @PreAuthorize 注解进行 Role.level 判断
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-28
 */
@Aspect
@Component
@Slf4j
public class PreAuthorizeAspect {

    private final ThreadStorageUtil threadStorageUtil;

    private final AccountService accountService;

    public PreAuthorizeAspect(ThreadStorageUtil threadStorageUtil, AccountService accountService) {

        this.threadStorageUtil = threadStorageUtil;
        this.accountService = accountService;
    }

    /**
     * 权限检查：检查当前用户是否拥有接口的权限
     */
    @Before("execution(* com.jianzj.mistake.hub..controller.*.*(..))")
    public void beforeControllerMethod(JoinPoint joinPoint) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        /**
         * 接口没有鉴权注解，跳过
         */
        if (preAuthorize == null) {
            return;
        }

        Role requiredRole = preAuthorize.requiredRole();

        Long accountId = threadStorageUtil.getCurAccountId();
        Account account = accountService.getById(accountId);

        /**
         * 用户不存在，报错
         */
        if (account == null) {
            throw new ApiAuthorizeException();
        }

        Role accountRole = Role.fromCode(account.getRole());

        /**
         * 用户权限为空，报错
         */
        if (accountRole == null) {
            throw new ApiAuthorizeException();
        }

        /**
         * 如果用户的权限 level 低于接口要求的 level，报异常
         */
        if (accountRole.getLevel() < requiredRole.getLevel()) {
            throw new ApiAuthorizeException();
        }
    }
}
