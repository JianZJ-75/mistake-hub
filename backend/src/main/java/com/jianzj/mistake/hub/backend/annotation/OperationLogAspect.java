package com.jianzj.mistake.hub.backend.annotation;

import com.alibaba.fastjson2.JSON;
import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.service.AccountService;
import com.jianzj.mistake.hub.backend.service.OperationLogService;
import com.jianzj.mistake.hub.backend.entity.OperationLog;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * <p>
 * 操作日志 AOP 切面，拦截 @OperationLogAnno 注解的 Controller 方法，
 * 方法成功返回后自动记录操作日志
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    private final AccountService accountService;

    private final ThreadStorageUtil threadStorageUtil;

    public OperationLogAspect(OperationLogService operationLogService,
                              AccountService accountService,
                              ThreadStorageUtil threadStorageUtil) {

        this.operationLogService = operationLogService;
        this.accountService = accountService;
        this.threadStorageUtil = threadStorageUtil;
    }

    /**
     * 方法成功返回后记录操作日志
     */
    @AfterReturning("@annotation(com.jianzj.mistake.hub.backend.annotation.OperationLogAnno)")
    public void afterReturning(JoinPoint joinPoint) {

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            OperationLogAnno anno = method.getAnnotation(OperationLogAnno.class);

            Long accountId = threadStorageUtil.getCurAccountId();
            String accountCode = null;

            if (accountId != null) {
                Account account = accountService.getById(accountId);
                if (account != null) {
                    accountCode = account.getCode();
                }
            }

            String targetId = extractTargetId(joinPoint.getArgs());
            String detail = extractDetail(joinPoint.getArgs());

            OperationLog operationLog = OperationLog.builder()
                    .accountId(accountId)
                    .accountCode(accountCode)
                    .action(anno.action())
                    .targetType(anno.targetType())
                    .targetId(targetId)
                    .detail(detail)
                    .build();

            operationLogService.record(operationLog);
        } catch (Exception e) {
            log.error("记录操作日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从方法参数中提取目标ID（尝试读取请求对象的 id / accountId / configKey 字段）
     */
    private String extractTargetId(Object[] args) {

        if (args == null || args.length == 0) {
            return null;
        }

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            try {
                // 优先尝试 id 字段
                Method getId = arg.getClass().getMethod("getId");
                Object id = getId.invoke(arg);
                if (id != null) {
                    return id.toString();
                }
            } catch (NoSuchMethodException ignored) {
                // 无 getId 方法
            } catch (Exception e) {
                log.debug("提取 targetId 失败: {}", e.getMessage());
            }

            try {
                Method getAccountId = arg.getClass().getMethod("getAccountId");
                Object accountId = getAccountId.invoke(arg);
                if (accountId != null) {
                    return accountId.toString();
                }
            } catch (NoSuchMethodException ignored) {
                // 无 getAccountId 方法
            } catch (Exception e) {
                log.debug("提取 targetId 失败: {}", e.getMessage());
            }

            try {
                Method getConfigKey = arg.getClass().getMethod("getConfigKey");
                Object configKey = getConfigKey.invoke(arg);
                if (configKey != null) {
                    return configKey.toString();
                }
            } catch (NoSuchMethodException ignored) {
                // 无 getConfigKey 方法
            } catch (Exception e) {
                log.debug("提取 targetId 失败: {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * 将第一个请求体参数 JSON 序列化为 detail
     */
    private String extractDetail(Object[] args) {

        if (args == null || args.length == 0) {
            return null;
        }

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            // 跳过基本类型和字符串
            if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                continue;
            }
            try {
                String json = JSON.toJSONString(arg);
                // 截断过长的 detail
                if (json.length() > 2000) {
                    json = json.substring(0, 2000);
                }
                return json;
            } catch (Exception e) {
                log.debug("序列化操作详情失败: {}", e.getMessage());
            }
        }

        return null;
    }
}
