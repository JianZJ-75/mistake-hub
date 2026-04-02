package com.jianzj.mistake.hub.common.lock.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

/**
 * <p>分布式锁注解，标注在方法上实现声明式加锁</p>
 *
 * @author jian.zhong
 * @since 2026-03-23
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLockAnno {

    /**
     * 锁的 key，支持 SpEL 表达式
     *
     * 示例：
     * @DistributedLockAnno(key = "'order:' + #orderId")
     * @DistributedLockAnno(key = "'user:' + #req.userId")
     */
    String key();

    /**
     * 自旋等待时间，默认 0 不等待
     */
    long waitTime() default 0;

    /**
     * 锁的持有时间，默认 30 秒
     */
    long leaseTime() default 30;

    /**
     * 时间单位，默认秒
     */
    ChronoUnit timeUnit() default ChronoUnit.SECONDS;

    /**
     * 获取锁失败时的中文错误信息
     */
    String errorMessageCn() default "获取分布式锁失败，请稍后重试";

    /**
     * 获取锁失败时的英文错误信息
     */
    String errorMessageEn() default "Failed to acquire distributed lock, please try again later.";
}
