package com.jianzj.mistake.hub.backend.scheduler;

import com.jianzj.mistake.hub.backend.service.ReviewScheduleService;
import com.jianzj.mistake.hub.common.lock.annotation.DistributedLockAnno;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 复习调度 定时任务
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-02
 */
@Component
@Slf4j
public class ReviewScheduler {

    private final ReviewScheduleService reviewScheduleService;

    public ReviewScheduler(ReviewScheduleService reviewScheduleService) {

        this.reviewScheduleService = reviewScheduleService;
    }

    /**
     * 凌晨定时任务：为所有有到期错题的用户生成当日复习计划
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @DistributedLockAnno(key = "'review:daily:generate'", leaseTime = 300)
    public void generateAllDailyPlans() {

        reviewScheduleService.generateAllDailyPlans();
    }
}
