package com.jianzj.mistake.hub.backend.manager;

import com.jianzj.mistake.hub.backend.dto.req.MistakeAddReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeDeleteReq;
import com.jianzj.mistake.hub.backend.entity.Mistake;
import com.jianzj.mistake.hub.backend.entity.ReviewRecord;
import com.jianzj.mistake.hub.backend.service.MistakeService;
import com.jianzj.mistake.hub.backend.service.ReviewPlanService;
import com.jianzj.mistake.hub.backend.service.ReviewRecordService;
import com.jianzj.mistake.hub.backend.service.ReviewScheduleService;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>错题编排 Manager（协调跨 Service 操作，打破循环依赖）</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Service
@Slf4j
public class MistakeManager {

    private final MistakeService mistakeService;

    private final ReviewRecordService reviewRecordService;

    private final ReviewPlanService reviewPlanService;

    private final ReviewScheduleService reviewScheduleService;

    private final ThreadStorageUtil threadStorageUtil;

    public MistakeManager(MistakeService mistakeService,
                          ReviewRecordService reviewRecordService,
                          ReviewPlanService reviewPlanService,
                          ReviewScheduleService reviewScheduleService,
                          ThreadStorageUtil threadStorageUtil) {

        this.mistakeService = mistakeService;
        this.reviewRecordService = reviewRecordService;
        this.reviewPlanService = reviewPlanService;
        this.reviewScheduleService = reviewScheduleService;
        this.threadStorageUtil = threadStorageUtil;
    }

    // ===== 业务方法 =====

    /**
     * 录入错题（含可选的初始复习记录创建）
     */
    @Transactional(rollbackFor = Exception.class)
    public void add(MistakeAddReq req) {

        Long mistakeId = mistakeService.add(req);

        // 若填写了录入备注，自动创建初始复习记录（不影响调度状态）
        if (StringUtils.isNotBlank(req.getNote())) {
            ReviewRecord initialRecord = ReviewRecord.builder()
                    .accountId(threadStorageUtil.getCurAccountId())
                    .mistakeId(mistakeId)
                    .isCorrect(0)
                    .reviewStageBefore(0)
                    .reviewStageAfter(0)
                    .masteryBefore(0)
                    .masteryAfter(0)
                    .note(req.getNote())
                    .noteImageUrl(req.getNoteImageUrl())
                    .reviewTime(LocalDateTime.now())
                    .build();
            reviewRecordService.createRecord(initialRecord);
        }
    }

    /**
     * 删除错题（软删除 + 联动跳过当天计划 + 清除缓存）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(MistakeDeleteReq req) {

        Mistake mistake = mistakeService.delete(req);

        // 联动：将当天 PENDING 的复习计划标记为 SKIPPED，并清除缓存
        reviewPlanService.skipPendingPlansByMistake(mistake.getId(), LocalDate.now());
        reviewScheduleService.evictDailyCache(mistake.getAccountId());
    }
}
