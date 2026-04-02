package com.jianzj.mistake.hub.common.lock.service.impl;

import com.jianzj.mistake.hub.common.lock.service.DistributedLockService;
import lombok.extern.slf4j.Slf4j;

import java.time.temporal.ChronoUnit;

/**
 * <p>MySQL 分布式锁实现</p>
 *
 * @author jian.zhong
 * @since 2026-03-23
 */
@Slf4j
public class MySQLDistributedLock extends AbstractDistributedLock {

    private final DistributedLockService distributedLockService;

    public MySQLDistributedLock(DistributedLockService distributedLockService) {

        this.distributedLockService = distributedLockService;
    }

    @Override
    protected boolean doTryLock(String lockKey, long leaseTime, ChronoUnit timeUnit) {

        Long lockId = distributedLockService.tryLock(lockKey, leaseTime, timeUnit);
        return lockId != null;
    }

    @Override
    protected void doUnlock(String lockKey) {

        distributedLockService.releaseLockByName(lockKey);
    }

    @Override
    public boolean isLocked(String lockKey) {

        return distributedLockService.isLocked(lockKey);
    }

    @Override
    public void releaseAllLocks() {

        distributedLockService.releaseAllLocks();
    }
}
