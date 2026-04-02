package com.jianzj.mistake.hub.common.lock.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.common.lock.entity.DistributedLock;
import com.jianzj.mistake.hub.common.lock.mapper.DistributedLockMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * <p>MySQL 分布式锁数据操作服务</p>
 *
 * @author jian.zhong
 * @since 2026-03-23
 */
@Slf4j
public class DistributedLockService extends ServiceImpl<DistributedLockMapper, DistributedLock> {

    // ==================== 业务方法 ====================

    /**
     * 尝试获取锁，成功返回锁 id，失败返回 null
     */
    public Long tryLock(String lockName, long leaseTime, ChronoUnit unit) {

        try {
            DistributedLock existingLock = getByLockName(lockName);

            /**
             * 锁存在且已过期，按 id 删除（避免竞态下误删新锁）
             */
            if (existingLock != null && existingLock.isExpired()) {
                lambdaUpdate()
                        .eq(DistributedLock::getId, existingLock.getId())
                        .remove();
            }

            LocalDateTime expiredTime = LocalDateTime.now().plus(leaseTime, unit);

            DistributedLock newLock = DistributedLock.builder()
                    .lockName(lockName)
                    .expiredTime(expiredTime)
                    .build();

            boolean success = save(newLock);
            if (!success) {
                return null;
            }

            log.info("抢锁成功, id [{}], name [{}]", newLock.getId(), lockName);
            return newLock.getId();

        } catch (org.springframework.dao.DuplicateKeyException e) {

            log.warn("抢锁失败 [{}]: 锁已被占用", lockName);
            return null;

        } catch (Throwable e) {

            log.warn("抢锁失败 [{}]: ", lockName, e);
            return null;
        }
    }

    /**
     * 根据锁名释放锁
     */
    public void releaseLockByName(String lockName) {

        try {
            lambdaUpdate()
                    .eq(DistributedLock::getLockName, lockName)
                    .remove();
            log.info("释放锁成功, name [{}]", lockName);
        } catch (Throwable e) {
            log.error("释放锁失败, name [{}]: ", lockName, e);
        }
    }

    /**
     * 释放所有锁（应用停机时调用）
     */
    public void releaseAllLocks() {

        try {
            long count = count();
            if (count == 0) {
                log.info("graceful shutdown: 无分布式锁需要释放");
                return;
            }

            /**
             * 单实例部署，表中所有锁都是当前实例的，直接清空
             */
            lambdaUpdate().remove();

            log.info("graceful shutdown: 释放 {} 把分布式锁", count);
        } catch (Throwable e) {
            log.error("graceful shutdown: 释放分布式锁失败", e);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 判断锁是否存在且未过期
     */
    public boolean isLocked(String lockName) {

        DistributedLock lock = getByLockName(lockName);
        return lock != null && !lock.isExpired();
    }

    /**
     * 根据锁名查询锁（安全写法，避免 TooManyResultsException）
     */
    private DistributedLock getByLockName(String lockName) {

        List<DistributedLock> locks = lambdaQuery()
                .eq(DistributedLock::getLockName, lockName)
                .list();
        return CollectionUtils.isEmpty(locks) ? null : locks.get(0);
    }
}
