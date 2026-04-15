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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
     * 删除错题（标记为待删除 + 联动跳过当天计划 + 清除缓存）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(MistakeDeleteReq req) {

        Mistake mistake = mistakeService.delete(req);

        // 联动：将当天 PENDING 的复习计划标记为 SKIPPED，并清除缓存
        reviewPlanService.skipPendingPlansByMistake(mistake.getId(), LocalDate.now());
        reviewScheduleService.evictDailyCache(mistake.getAccountId());
    }

    /**
     * 批量清理待删除错题的 OSS 图片（定时任务入口）：逐条处理，单条失败跳过，下次继续
     */
    public void cleanupPendingDelete(int batchSize) {

        List<Mistake> pendingList = mistakeService.listPendingDelete(batchSize);
        if (CollectionUtils.isEmpty(pendingList)) {
            log.info("无待清理的错题");
            return;
        }

        log.info("开始清理待删除错题，本批数量：{}", pendingList.size());
        int successCount = 0;

        for (Mistake mistake : pendingList) {
            try {
                cleanupOssImages(mistake);
                mistakeService.markDeleted(mistake.getId());
                successCount++;
            } catch (Exception e) {
                log.warn("错题（id={}）OSS 清理未完成，跳过：{}", mistake.getId(), e.getMessage());
            }
        }

        log.info("待删除错题清理完成，成功 {} / 总计 {}", successCount, pendingList.size());
    }

    // ===== 工具方法 =====

    /**
     * 逐个清理错题及其复习记录的 OSS 图片；每个图片是独立事务单元（Service 层控制），
     * 先置空 DB 字段再删 OSS，OSS 失败则该单元回滚。任一单元失败则异常冒泡，跳过该错题。
     */
    private void cleanupOssImages(Mistake mistake) {

        if (StringUtils.isNotBlank(mistake.getTitleImageUrl())) {
            mistakeService.cleanupTitleImage(mistake.getId(), mistake.getTitleImageUrl());
        }

        if (StringUtils.isNotBlank(mistake.getAnswerImageUrl())) {
            mistakeService.cleanupAnswerImage(mistake.getId(), mistake.getAnswerImageUrl());
        }

        List<ReviewRecord> records = reviewRecordService.listEntityByMistakeId(mistake.getId());
        if (CollectionUtils.isNotEmpty(records)) {
            for (ReviewRecord record : records) {
                if (StringUtils.isNotBlank(record.getNoteImageUrl())) {
                    reviewRecordService.cleanupNoteImage(record.getId(), record.getNoteImageUrl());
                }
            }
        }
    }
}
