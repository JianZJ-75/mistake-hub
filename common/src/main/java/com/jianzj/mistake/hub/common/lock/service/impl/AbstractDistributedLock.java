package com.jianzj.mistake.hub.common.lock.service.impl;

import com.jianzj.mistake.hub.common.lock.service.LockService;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * <p>分布式锁抽象模板，封装自旋等待和解锁安全校验</p>
 *
 * @author jian.zhong
 * @since 2026-03-23
 */
@Slf4j
public abstract class AbstractDistributedLock implements LockService {

    private static final long SPIN_INTERVAL_NANOS = 200_000_000L;

    @Override
    public boolean tryLock(String lockKey, long leaseTime, ChronoUnit timeUnit) {

        return tryLock(lockKey, 0, leaseTime, timeUnit);
    }

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, ChronoUnit timeUnit) {

        long waitTimeNanos = Duration.of(waitTime, timeUnit).toNanos();
        long startTime = System.nanoTime();

        while (true) {
            if (doTryLock(lockKey, leaseTime, timeUnit)) {
                return true;
            }

            if (System.nanoTime() - startTime >= waitTimeNanos) {
                return false;
            }

            LockSupport.parkNanos(SPIN_INTERVAL_NANOS);
        }
    }

    @Override
    public void unlock(String lockKey) {

        if (!isLocked(lockKey)) {
            return;
        }

        doUnlock(lockKey);
    }

    /**
     * 子类实现：尝试获取锁
     */
    protected abstract boolean doTryLock(String lockKey, long leaseTime, ChronoUnit timeUnit);

    /**
     * 子类实现：释放锁
     */
    protected abstract void doUnlock(String lockKey);
}
