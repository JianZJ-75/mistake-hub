package com.jianzj.mistake.hub.common.lock.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * <p>Redis（Redisson）分布式锁实现</p>
 *
 * @author jian.zhong
 * @since 2026-03-23
 */
@Slf4j
public class RedisDistributedLock extends AbstractDistributedLock {

    private final RedissonClient redissonClient;

    public RedisDistributedLock(RedissonClient redissonClient) {

        this.redissonClient = redissonClient;
    }

    /**
     * 重写父类方法，利用 Redisson 原生的等待机制，避免低效自旋
     */
    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, ChronoUnit timeUnit) {

        try {
            RLock lock = redissonClient.getLock(lockKey);
            TimeUnit tu = toTimeUnit(timeUnit);
            return lock.tryLock(waitTime, leaseTime, tu);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取 Redis 锁被中断: {}", lockKey, e);
            return false;
        }
    }

    @Override
    protected boolean doTryLock(String lockKey, long leaseTime, ChronoUnit timeUnit) {

        try {
            RLock lock = redissonClient.getLock(lockKey);
            TimeUnit tu = toTimeUnit(timeUnit);
            return lock.tryLock(0, leaseTime, tu);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取 Redis 锁被中断: {}", lockKey, e);
            return false;
        }
    }

    @Override
    protected void doUnlock(String lockKey) {

        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public boolean isLocked(String lockKey) {

        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }

    @Override
    public void releaseAllLocks() {

        /**
         * Redisson 锁基于 Redis TTL，连接断开后按 leaseTime 自动过期，无需主动清理
         */
        log.info("graceful shutdown: Redis 分布式锁将自动过期释放");
    }

    // ==================== 工具方法 ====================

    /**
     * ChronoUnit 转 TimeUnit（Redisson API 需要）
     */
    private TimeUnit toTimeUnit(ChronoUnit chronoUnit) {

        return switch (chronoUnit) {
            case NANOS -> TimeUnit.NANOSECONDS;
            case MICROS -> TimeUnit.MICROSECONDS;
            case MILLIS -> TimeUnit.MILLISECONDS;
            case MINUTES -> TimeUnit.MINUTES;
            case HOURS -> TimeUnit.HOURS;
            case DAYS -> TimeUnit.DAYS;
            default -> TimeUnit.SECONDS;
        };
    }
}
