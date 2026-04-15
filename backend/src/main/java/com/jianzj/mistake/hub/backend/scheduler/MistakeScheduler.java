package com.jianzj.mistake.hub.backend.scheduler;

import com.jianzj.mistake.hub.backend.manager.MistakeManager;
import com.jianzj.mistake.hub.common.lock.annotation.DistributedLockAnno;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 错题清理 定时任务
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-15
 */
@Component
@Slf4j
public class MistakeScheduler {

    private final MistakeManager mistakeManager;

    public MistakeScheduler(MistakeManager mistakeManager) {

        this.mistakeManager = mistakeManager;
    }

    /**
     * 凌晨定时任务：清理待删除错题的 OSS 图片和关联数据后物理删除
     */
    @Scheduled(cron = "0 30 2 * * ?")
    @DistributedLockAnno(key = "'mistake:cleanup:pending-delete'", leaseTime = 600)
    public void cleanupPendingDelete() {

        mistakeManager.cleanupPendingDelete(50);
    }
}
