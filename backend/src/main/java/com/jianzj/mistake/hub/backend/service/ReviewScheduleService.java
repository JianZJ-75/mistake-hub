package com.jianzj.mistake.hub.backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.jianzj.mistake.hub.backend.dto.req.ReviewHistoryReq;
import com.jianzj.mistake.hub.backend.dto.req.ReviewSkipReq;
import com.jianzj.mistake.hub.backend.dto.req.ReviewSubmitReq;
import com.jianzj.mistake.hub.backend.dto.resp.MistakeDetailResp;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewProgressResp;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewRecordResp;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewSubmitResp;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewTaskResp;
import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.entity.Mistake;
import com.jianzj.mistake.hub.backend.entity.ReviewPlan;
import com.jianzj.mistake.hub.backend.entity.ReviewRecord;
import com.jianzj.mistake.hub.backend.enums.ReviewPlanStatus;
import com.jianzj.mistake.hub.backend.enums.ReviewStage;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

/**
 * <p>
 * 复习调度 服务类（核心调度算法 + Redis 缓存）
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-02
 */
@Service
@Slf4j
public class ReviewScheduleService {

    private static final String CACHE_KEY_PREFIX = "review:daily:";

    private static final String CFG_INTERVALS = "review.intervals";

    private static final String CFG_MASTERY_CORRECT_DELTA = "review.mastery_correct_delta";

    private static final String CFG_MASTERY_WRONG_DELTA = "review.mastery_wrong_delta";

    private static final String CFG_STAGE_WRONG_BACK = "review.stage_wrong_back";

    private static final String DEFAULT_INTERVALS = "0,1,2,4,7,15,30";

    private final MistakeService mistakeService;

    private final AccountService accountService;

    private final ReviewPlanService reviewPlanService;

    private final ReviewRecordService reviewRecordService;

    private final SystemConfigService systemConfigService;

    private final RedissonClient redissonClient;

    private final ThreadStorageUtil threadStorageUtil;

    public ReviewScheduleService(MistakeService mistakeService,
                                 AccountService accountService,
                                 ReviewPlanService reviewPlanService,
                                 ReviewRecordService reviewRecordService,
                                 SystemConfigService systemConfigService,
                                 RedissonClient redissonClient,
                                 ThreadStorageUtil threadStorageUtil) {

        this.mistakeService = mistakeService;
        this.accountService = accountService;
        this.reviewPlanService = reviewPlanService;
        this.reviewRecordService = reviewRecordService;
        this.systemConfigService = systemConfigService;
        this.redissonClient = redissonClient;
        this.threadStorageUtil = threadStorageUtil;
    }

    // ===== 业务方法 =====

    /**
     * 生成每日复习任务（核心调度）
     */
    public List<ReviewTaskResp> generateDailyTasks(Long accountId) {

        Account account = accountService.getById(accountId);
        if (account == null) {
            oops("用户不存在", "Account does not exist.");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        // 查询到期错题
        List<Mistake> dueMistakes = mistakeService.listDueForReview(accountId, todayEnd);

        if (CollectionUtils.isNotEmpty(dueMistakes)) {
            // 幂等排除：已有计划的 mistakeId 不再创建
            Set<Long> existingPlanMistakeIds = reviewPlanService.getPlannedMistakeIds(accountId, today);
            List<Mistake> newMistakes = dueMistakes.stream()
                    .filter(m -> !existingPlanMistakeIds.contains(m.getId()))
                    .collect(Collectors.toList());

            // 计算优先级并取 top N
            int dailyLimit = account.getDailyLimit() != null ? account.getDailyLimit().intValue() : 30;
            int remaining = dailyLimit - existingPlanMistakeIds.size();

            if (remaining > 0 && CollectionUtils.isNotEmpty(newMistakes)) {
                List<Mistake> topMistakes = newMistakes.stream()
                        .sorted(Comparator.comparingDouble(this::calculatePriority).reversed())
                        .limit(remaining)
                        .collect(Collectors.toList());

                // 批量创建 ReviewPlan
                List<ReviewPlan> plans = topMistakes.stream()
                        .map(m -> ReviewPlan.builder()
                                .accountId(accountId)
                                .mistakeId(m.getId())
                                .plannedDate(today)
                                .status(ReviewPlanStatus.PENDING.getCode())
                                .build())
                        .collect(Collectors.toList());

                reviewPlanService.batchCreate(plans);
            }
        }

        // 组装返回（包含新建 + 已有的所有计划）
        List<ReviewTaskResp> tasks = buildTasksFromExistingPlans(accountId, today);

        // 写入缓存
        cacheDaily(accountId, tasks);

        return tasks;
    }

    /**
     * 答对/答错后更新错题复习状态（参数从 system_config 动态读取）
     */
    public void updateMistakeAfterReview(Long mistakeId, boolean isCorrect) {

        Mistake mistake = mistakeService.getById(mistakeId);
        if (mistake == null) {
            oops("错题不存在", "Mistake does not exist.");
        }

        int currentStage = mistake.getReviewStage() != null ? mistake.getReviewStage() : 0;
        int currentMastery = mistake.getMasteryLevel() != null ? mistake.getMasteryLevel() : 0;

        int masteryCorrectDelta = systemConfigService.getIntByKey(CFG_MASTERY_CORRECT_DELTA, 20);
        int masteryWrongDelta = systemConfigService.getIntByKey(CFG_MASTERY_WRONG_DELTA, 15);
        int stageWrongBack = systemConfigService.getIntByKey(CFG_STAGE_WRONG_BACK, 2);

        int newStage;
        int newMastery;

        if (isCorrect) {
            newStage = Math.min(currentStage + 1, 6);
            newMastery = Math.min(currentMastery + masteryCorrectDelta, 100);
        } else {
            newStage = Math.max(currentStage - stageWrongBack, 0);
            newMastery = Math.max(currentMastery - masteryWrongDelta, 0);
        }

        int intervalDays = getIntervalByStage(newStage);
        LocalDateTime now = LocalDateTime.now();

        mistake.setReviewStage(newStage);
        mistake.setMasteryLevel(newMastery);
        mistake.setLastReviewTime(now);
        mistake.setNextReviewTime(now.plusDays(intervalDays));

        boolean success = mistakeService.updateById(mistake);
        if (!success) {
            oops("更新错题复习状态失败", "Failed to update mistake review status.");
        }
    }

    /**
     * 写入 Redis 缓存
     */
    public void cacheDaily(Long accountId, List<ReviewTaskResp> tasks) {

        String key = buildCacheKey(accountId);
        String json = JSON.toJSONString(tasks);

        RBucket<String> bucket = redissonClient.getBucket(key);

        // TTL：到次日 00:00:00 的秒数
        LocalDateTime nextMidnight = LocalDate.now().plusDays(1).atStartOfDay();
        long ttlSeconds = Duration.between(LocalDateTime.now(), nextMidnight).getSeconds();

        bucket.set(json, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 读取 Redis 缓存
     */
    public List<ReviewTaskResp> getCachedDaily(Long accountId) {

        String key = buildCacheKey(accountId);
        RBucket<String> bucket = redissonClient.getBucket(key);
        String json = bucket.get();

        if (StringUtils.isBlank(json)) {
            return null;
        }

        return JSON.parseObject(json, new TypeReference<List<ReviewTaskResp>>() {});
    }

    /**
     * 失效当天缓存（供编辑错题等场景调用）
     */
    public void evictDailyCache(Long accountId) {

        String key = buildCacheKey(accountId);
        redissonClient.getBucket(key).delete();
    }

    /**
     * 为所有有到期错题的用户生成当日复习计划（供定时任务调用）
     */
    public void generateAllDailyPlans() {

        long start = System.currentTimeMillis();
        log.info("[复习调度] 定时任务开始执行");

        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        // 查询有到期错题的用户ID（groupBy 去重，避免加载冗余行）
        List<Mistake> dueMistakes = mistakeService.lambdaQuery()
                .eq(Mistake::getStatus, 1)
                .le(Mistake::getNextReviewTime, todayEnd)
                .select(Mistake::getAccountId)
                .groupBy(Mistake::getAccountId)
                .list();

        if (CollectionUtils.isEmpty(dueMistakes)) {
            log.info("[复习调度] 无待复习用户，跳过");
            return;
        }

        Set<Long> accountIds = dueMistakes.stream()
                .map(Mistake::getAccountId)
                .collect(Collectors.toSet());

        int totalTasks = 0;
        int failCount = 0;

        for (Long accountId : accountIds) {
            try {
                List<ReviewTaskResp> tasks = generateDailyTasks(accountId);
                totalTasks += CollectionUtils.isNotEmpty(tasks) ? tasks.size() : 0;
            } catch (Exception e) {
                failCount++;
                log.error("[复习调度] 用户 {} 生成失败: {}", accountId, e.getMessage(), e);
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("[复习调度] 定时任务完成，用户数={}，失败={}，任务数={}，耗时={}ms",
                accountIds.size(), failCount, totalTasks, elapsed);
    }

    /**
     * 获取当前用户今日复习任务列表
     */
    public List<ReviewTaskResp> todayTasks() {

        Long accountId = threadStorageUtil.getCurAccountId();

        // 优先读缓存
        List<ReviewTaskResp> cached = getCachedDaily(accountId);
        if (cached != null) {
            return cached;
        }

        // 缓存 miss → 生成（内含写缓存）
        return generateDailyTasks(accountId);
    }

    /**
     * 提交复习结果
     */
    public ReviewSubmitResp submitReview(ReviewSubmitReq req) {

        Long accountId = threadStorageUtil.getCurAccountId();
        Long mistakeId = req.getMistakeId();
        LocalDate today = LocalDate.now();

        // 校验错题存在且属于当前用户
        mistakeService.getValidMistakeForCurrentUser(mistakeId);

        // 查今日计划
        ReviewPlan plan = reviewPlanService.getByMistakeAndDate(accountId, mistakeId, today);
        if (plan == null) {
            oops("该题今日无复习计划", "No review plan for this mistake today.");
        }
        if (ReviewPlanStatus.COMPLETED.getCode().equals(plan.getStatus())) {
            oops("该题今日已完成复习", "This mistake has already been reviewed today.");
        }

        // 记录 before 快照
        Mistake before = mistakeService.getById(mistakeId);
        int stageBefore = before.getReviewStage() != null ? before.getReviewStage() : 0;
        int masteryBefore = before.getMasteryLevel() != null ? before.getMasteryLevel() : 0;

        // 更新错题复习状态
        updateMistakeAfterReview(mistakeId, req.getIsCorrect());

        // 获取 after 快照
        Mistake after = mistakeService.getById(mistakeId);
        int stageAfter = after.getReviewStage() != null ? after.getReviewStage() : 0;
        int masteryAfter = after.getMasteryLevel() != null ? after.getMasteryLevel() : 0;

        // 创建复习记录
        ReviewRecord record = ReviewRecord.builder()
                .accountId(accountId)
                .mistakeId(mistakeId)
                .reviewPlanId(plan.getId())
                .isCorrect(req.getIsCorrect() ? 1 : 0)
                .reviewStageBefore(stageBefore)
                .reviewStageAfter(stageAfter)
                .masteryBefore(masteryBefore)
                .masteryAfter(masteryAfter)
                .note(req.getNote())
                .noteImageUrl(req.getNoteImageUrl())
                .reviewTime(LocalDateTime.now())
                .build();
        reviewRecordService.createRecord(record);

        // 更新计划状态为 COMPLETED
        reviewPlanService.updateStatus(plan.getId(), ReviewPlanStatus.COMPLETED.getCode());

        // 精确更新缓存
        updateCachedTaskStatus(accountId, mistakeId, ReviewPlanStatus.COMPLETED.getCode());

        return ReviewSubmitResp.builder()
                .reviewStageBefore(stageBefore)
                .reviewStageAfter(stageAfter)
                .masteryBefore(masteryBefore)
                .masteryAfter(masteryAfter)
                .nextReviewTime(after.getNextReviewTime())
                .build();
    }

    /**
     * 跳过复习
     */
    public void skipReview(ReviewSkipReq req) {

        Long accountId = threadStorageUtil.getCurAccountId();
        Long mistakeId = req.getMistakeId();
        LocalDate today = LocalDate.now();

        ReviewPlan plan = reviewPlanService.getByMistakeAndDate(accountId, mistakeId, today);
        if (plan == null) {
            oops("该题今日无复习计划", "No review plan for this mistake today.");
        }
        if (ReviewPlanStatus.COMPLETED.getCode().equals(plan.getStatus())) {
            oops("该题今日已完成复习，无法跳过", "This mistake has already been reviewed today.");
        }
        // 幂等：已 SKIPPED 直接返回
        if (ReviewPlanStatus.SKIPPED.getCode().equals(plan.getStatus())) {
            return;
        }

        reviewPlanService.updateStatus(plan.getId(), ReviewPlanStatus.SKIPPED.getCode());
        updateCachedTaskStatus(accountId, mistakeId, ReviewPlanStatus.SKIPPED.getCode());
    }

    /**
     * 获取当前用户今日复习进度
     */
    public ReviewProgressResp getProgress() {

        Long accountId = threadStorageUtil.getCurAccountId();
        LocalDate today = LocalDate.now();

        List<ReviewPlan> plans = reviewPlanService.listByAccountAndDate(accountId, today);

        int total = CollectionUtils.isNotEmpty(plans) ? plans.size() : 0;
        int completed = 0;
        int skipped = 0;

        if (CollectionUtils.isNotEmpty(plans)) {
            for (ReviewPlan plan : plans) {
                if (ReviewPlanStatus.COMPLETED.getCode().equals(plan.getStatus())) {
                    completed++;
                } else if (ReviewPlanStatus.SKIPPED.getCode().equals(plan.getStatus())) {
                    skipped++;
                }
            }
        }

        int streakDays = calculateStreakDays(accountId, today);

        return ReviewProgressResp.builder()
                .totalToday(total)
                .completedToday(completed)
                .skippedToday(skipped)
                .streakDays(streakDays)
                .build();
    }

    /**
     * 查询某道错题的复习历史记录
     */
    public List<ReviewRecordResp> getReviewHistory(ReviewHistoryReq req) {

        // 管理员可查任意错题，普通用户只能查自己的
        Mistake mistake = mistakeService.getValidMistake(req.getMistakeId());

        return reviewRecordService.listByMistake(mistake.getAccountId(), req.getMistakeId());
    }

    // ===== 工具方法 =====

    /**
     * 根据阶段序号获取间隔天数（从 system_config 动态读取）
     */
    private int getIntervalByStage(int stageIndex) {

        String intervalsStr = systemConfigService.getByKey(CFG_INTERVALS, DEFAULT_INTERVALS);
        String[] parts = intervalsStr.split(",");

        if (stageIndex >= 0 && stageIndex < parts.length) {
            try {
                return Integer.parseInt(parts[stageIndex].trim());
            } catch (NumberFormatException e) {
                log.warn("复习间隔配置解析失败，stageIndex={}，使用枚举默认值", stageIndex);
            }
        }

        return ReviewStage.getIntervalByStage(stageIndex);
    }

    /**
     * 计算错题优先级分数（Mistake 入参版本，供排序用）
     */
    private double calculatePriority(Mistake mistake) {

        int overdueDays = calculateOverdueDays(mistake.getNextReviewTime(), LocalDate.now());
        return calculatePriorityScore(overdueDays, mistake.getMasteryLevel(), mistake.getReviewStage());
    }

    /**
     * 计算逾期天数
     */
    private int calculateOverdueDays(LocalDateTime nextReviewTime, LocalDate baseDate) {

        if (nextReviewTime == null) {
            return 0;
        }
        int days = (int) ChronoUnit.DAYS.between(nextReviewTime.toLocalDate(), baseDate);
        return Math.max(days, 0);
    }

    /**
     * 优先级公式：overdueDays × 3.0 + (100 - mastery) × 0.5 + (6 - stage) × 2.0
     */
    private double calculatePriorityScore(int overdueDays, Integer mastery, Integer stage) {

        int m = mastery != null ? mastery : 0;
        int s = stage != null ? stage : 0;
        return overdueDays * 3.0 + (100 - m) * 0.5 + (6 - s) * 2.0;
    }

    /**
     * 根据已有的复习计划组装 ReviewTaskResp 列表
     */
    private List<ReviewTaskResp> buildTasksFromExistingPlans(Long accountId, LocalDate date) {

        List<ReviewPlan> allPlans = reviewPlanService.listByAccountAndDate(accountId, date);
        if (CollectionUtils.isEmpty(allPlans)) {
            return new ArrayList<>();
        }

        List<Long> mistakeIds = allPlans.stream()
                .map(ReviewPlan::getMistakeId)
                .collect(Collectors.toList());

        Map<Long, MistakeDetailResp> detailMap = mistakeService.listDetailByIds(mistakeIds);

        return allPlans.stream()
                .map(plan -> {
                    MistakeDetailResp detail = detailMap.get(plan.getMistakeId());
                    if (detail == null) {
                        return null;
                    }

                    int overdueDays = calculateOverdueDays(detail.getNextReviewTime(), date);
                    double priority = calculatePriorityScore(overdueDays, detail.getMasteryLevel(), detail.getReviewStage());

                    return ReviewTaskResp.builder()
                            .mistakeId(plan.getMistakeId())
                            .title(detail.getTitle())
                            .titleImageUrl(detail.getTitleImageUrl())
                            .correctAnswer(detail.getCorrectAnswer())
                            .answerImageUrl(detail.getAnswerImageUrl())
                            .reviewStage(detail.getReviewStage())
                            .masteryLevel(detail.getMasteryLevel())
                            .overdueDays(overdueDays)
                            .priority(priority)
                            .tags(detail.getTags())
                            .planStatus(plan.getStatus())
                            .reviewPlanId(plan.getId())
                            .build();
                })
                .filter(t -> t != null)
                .sorted(Comparator.comparingDouble(ReviewTaskResp::getPriority).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 构建 Redis 缓存 key
     */
    private String buildCacheKey(Long accountId) {

        return CACHE_KEY_PREFIX + accountId + ":" + LocalDate.now();
    }

    /**
     * 精确更新缓存中指定错题的计划状态（避免 evict 重建）
     */
    private void updateCachedTaskStatus(Long accountId, Long mistakeId, String newStatus) {

        List<ReviewTaskResp> cached = getCachedDaily(accountId);
        if (cached == null) {
            return;
        }

        for (ReviewTaskResp task : cached) {
            if (mistakeId.equals(task.getMistakeId())) {
                task.setPlanStatus(newStatus);
                break;
            }
        }

        cacheDaily(accountId, cached);
    }

    /**
     * 计算连续复习天数
     */
    private int calculateStreakDays(Long accountId, LocalDate today) {

        List<LocalDate> completedDates = reviewPlanService.listCompletedDates(accountId);
        if (CollectionUtils.isEmpty(completedDates)) {
            return 0;
        }

        Set<LocalDate> dateSet = new HashSet<>(completedDates);

        // 起始日：今天在集合中 → 从今天算；否则从昨天算
        LocalDate current = dateSet.contains(today) ? today : today.minusDays(1);

        int streak = 0;
        while (dateSet.contains(current)) {
            streak++;
            current = current.minusDays(1);
        }

        return streak;
    }
}
