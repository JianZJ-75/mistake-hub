package com.jianzj.mistake.hub.common.lock.service;

import java.time.temporal.ChronoUnit;

/**
 * <p>分布式锁服务接口</p>
 *
 * @author jian.zhong
 * @since 2026-03-23
 */
public interface LockService {

    /**
     * 尝试获取锁（无等待）
     */
    boolean tryLock(String lockKey, long leaseTime, ChronoUnit timeUnit);

    /**
     * 尝试获取锁（自旋等待）
     */
    boolean tryLock(String lockKey, long waitTime, long leaseTime, ChronoUnit timeUnit);

    /**
     * 释放锁
     */
    void unlock(String lockKey);

    /**
     * 检查锁是否存在
     */
    boolean isLocked(String lockKey);

    /**
     * 释放当前实例持有的所有锁（应用停机时调用）
     */
    void releaseAllLocks();
}
