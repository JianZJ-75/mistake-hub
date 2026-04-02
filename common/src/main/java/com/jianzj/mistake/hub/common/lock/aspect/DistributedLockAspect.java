package com.jianzj.mistake.hub.common.lock.aspect;

import com.jianzj.mistake.hub.common.lock.annotation.DistributedLockAnno;
import com.jianzj.mistake.hub.common.lock.service.LockService;
import com.jianzj.mistake.hub.common.lock.support.SpELKeyEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

/**
 * <p>分布式锁 AOP 切面，拦截 @DistributedLockAnno 注解</p>
 *
 * @author jian.zhong
 * @since 2026-03-23
 */
@Aspect
@Component
@Slf4j
public class DistributedLockAspect {

    private final LockService lockService;

    private final SpELKeyEvaluator spELKeyEvaluator;

    public DistributedLockAspect(LockService lockService, SpELKeyEvaluator spELKeyEvaluator) {

        this.lockService = lockService;
        this.spELKeyEvaluator = spELKeyEvaluator;
    }

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLockAnno distributedLock) throws Throwable {

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] args = joinPoint.getArgs();

        String lockKey = spELKeyEvaluator.generateKey(distributedLock.key(), method, args);

        boolean locked = false;
        try {
            locked = lockService.tryLock(
                    lockKey,
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!locked) {
                oops(distributedLock.errorMessageCn(), distributedLock.errorMessageEn());
            }

            return joinPoint.proceed();
        } finally {
            if (locked) {
                lockService.unlock(lockKey);
            }
        }
    }
}
