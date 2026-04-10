package com.jianzj.mistake.hub.backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.jianzj.mistake.hub.backend.dto.resp.MistakeDetailResp;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewTaskResp;
import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.entity.Mistake;
import com.jianzj.mistake.hub.backend.entity.ReviewPlan;
import com.jianzj.mistake.hub.backend.enums.ReviewPlanStatus;
import com.jianzj.mistake.hub.backend.enums.ReviewStage;
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

    private final MistakeService mistakeService;

    private final AccountService accountService;

    private final ReviewPlanService reviewPlanService;

    private final RedissonClient redissonClient;

    public ReviewScheduleService(MistakeService mistakeService,
                                 AccountService accountService,
                                 ReviewPlanService reviewPlanService,
                                 RedissonClient redissonClient) {

        this.mistakeService = mistakeService;
        this.accountService = accountService;
        this.reviewPlanService = reviewPlanService;
        this.redissonClient = redissonClient;
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
     * 答对/答错后更新错题复习状态
     */
    public void updateMistakeAfterReview(Long mistakeId, boolean isCorrect) {

        Mistake mistake = mistakeService.getById(mistakeId);
        if (mistake == null) {
            oops("错题不存在", "Mistake does not exist.");
        }

        int currentStage = mistake.getReviewStage() != null ? mistake.getReviewStage() : 0;
        int currentMastery = mistake.getMasteryLevel() != null ? mistake.getMasteryLevel() : 0;

        int newStage;
        int newMastery;

        if (isCorrect) {
            newStage = ReviewStage.nextStageOnCorrect(currentStage);
            newMastery = Math.min(currentMastery + 20, 100);
        } else {
            newStage = ReviewStage.nextStageOnWrong(currentStage);
            newMastery = Math.max(currentMastery - 15, 0);
        }

        int intervalDays = ReviewStage.getIntervalByStage(newStage);
        LocalDateTime now = LocalDateTime.now();

        mistake.setReviewStage(newStage);
        mistake.setMasteryLevel(newMastery);
        mistake.setLastReviewTime(now);
        mistake.setNextReviewTime(now.plusDays(intervalDays));

        boolean success = mistakeService.updateById(mistake);
        if (!success) {
            oops("更新错题复习状态失败", "Failed to update mistake review status.");
        }

        // 失效当天缓存
        evictDailyCache(mistake.getAccountId());
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

    // ===== 工具方法 =====

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
                            .imageUrl(detail.getImageUrl())
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
}
