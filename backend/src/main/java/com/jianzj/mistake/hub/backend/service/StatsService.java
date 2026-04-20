package com.jianzj.mistake.hub.backend.service;

import com.jianzj.mistake.hub.backend.dto.resp.AdminOverviewResp;
import com.jianzj.mistake.hub.backend.dto.resp.CurvePrediction;
import com.jianzj.mistake.hub.backend.dto.resp.CurveSegment;
import com.jianzj.mistake.hub.backend.dto.resp.DailyCompletionResp;
import com.jianzj.mistake.hub.backend.dto.resp.ForgettingCurveResp;
import com.jianzj.mistake.hub.backend.dto.resp.MasteryDistributionResp;
import com.jianzj.mistake.hub.backend.dto.resp.MasteryTrendResp;
import com.jianzj.mistake.hub.backend.dto.resp.MemoryHealthResp;
import com.jianzj.mistake.hub.backend.dto.resp.RetentionBucket;
import com.jianzj.mistake.hub.backend.dto.resp.RetentionTrendResp;
import com.jianzj.mistake.hub.backend.dto.resp.StatsOverviewResp;
import com.jianzj.mistake.hub.backend.dto.resp.StreakResp;
import com.jianzj.mistake.hub.backend.dto.resp.SubjectStatsResp;
import com.jianzj.mistake.hub.backend.dto.resp.UpcomingReview;
import com.jianzj.mistake.hub.backend.entity.Mistake;
import com.jianzj.mistake.hub.backend.entity.ReviewPlan;
import com.jianzj.mistake.hub.backend.entity.ReviewRecord;
import com.jianzj.mistake.hub.backend.enums.ReviewPlanStatus;
import com.jianzj.mistake.hub.backend.enums.ReviewStage;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>统计 编排服务类（不继承 ServiceImpl）</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Service
@Slf4j
public class StatsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String CFG_INTERVALS = "review.intervals";

    private static final String DEFAULT_INTERVALS = "0,1,2,4,7,15,30";

    private static final double STABILITY_FACTOR = 1.44;

    private static final int PREDICTION_DAYS = 14;

    private final MistakeService mistakeService;

    private final ReviewPlanService reviewPlanService;

    private final ReviewRecordService reviewRecordService;

    private final AccountService accountService;

    private final MistakeTagService mistakeTagService;

    private final SystemConfigService systemConfigService;

    private final ThreadStorageUtil threadStorageUtil;

    public StatsService(MistakeService mistakeService,
                        ReviewPlanService reviewPlanService,
                        ReviewRecordService reviewRecordService,
                        AccountService accountService,
                        MistakeTagService mistakeTagService,
                        SystemConfigService systemConfigService,
                        ThreadStorageUtil threadStorageUtil) {

        this.mistakeService = mistakeService;
        this.reviewPlanService = reviewPlanService;
        this.reviewRecordService = reviewRecordService;
        this.accountService = accountService;
        this.mistakeTagService = mistakeTagService;
        this.systemConfigService = systemConfigService;
        this.threadStorageUtil = threadStorageUtil;
    }

    // ===== 业务方法 =====

    /**
     * 5.1 错题总览统计
     */
    public StatsOverviewResp overview() {

        Long accountId = threadStorageUtil.getCurAccountId();
        LocalDateTime now = LocalDateTime.now();

        long totalMistakes = mistakeService.countValidByAccountId(accountId);
        long masteredCount = mistakeService.countMasteredByAccountId(accountId, 80);
        long pendingReviewCount = mistakeService.countPendingReview(accountId, now);

        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long newThisWeek = mistakeService.countNewThisWeek(accountId, monday.atStartOfDay());

        return StatsOverviewResp.builder()
                .totalMistakes((int) totalMistakes)
                .masteredCount((int) masteredCount)
                .pendingReviewCount((int) pendingReviewCount)
                .newThisWeek((int) newThisWeek)
                .build();
    }

    /**
     * 5.2 学科分布统计
     */
    public List<SubjectStatsResp> subject() {

        Long accountId = threadStorageUtil.getCurAccountId();

        List<Mistake> mistakes = mistakeService.listValidByAccountId(accountId);
        if (CollectionUtils.isEmpty(mistakes)) {
            return new ArrayList<>();
        }

        List<Long> mistakeIds = mistakes.stream().map(Mistake::getId).collect(Collectors.toList());
        Map<Long, Integer> masteryMap = mistakes.stream()
                .collect(Collectors.toMap(Mistake::getId, Mistake::getMasteryLevel));

        // 查询每个错题关联的 SUBJECT 标签名
        Map<Long, String> subjectMap = mistakeTagService.getSubjectNamesByMistakeIds(mistakeIds);

        // 按学科聚合
        Map<String, List<Long>> grouped = mistakeIds.stream()
                .collect(Collectors.groupingBy(id -> subjectMap.getOrDefault(id, "未分类")));

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<Long> ids = entry.getValue();
                    double avgMastery = ids.stream()
                            .mapToInt(id -> masteryMap.getOrDefault(id, 0))
                            .average()
                            .orElse(0.0);
                    return SubjectStatsResp.builder()
                            .subject(entry.getKey())
                            .count(ids.size())
                            .avgMastery(Math.round(avgMastery * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparingInt(SubjectStatsResp::getCount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 5.3 掌握度分布
     */
    public MasteryDistributionResp mastery() {

        Long accountId = threadStorageUtil.getCurAccountId();

        long stranger = mistakeService.countByMasteryRange(accountId, 0, 20);
        long beginner = mistakeService.countByMasteryRange(accountId, 20, 60);
        long basic = mistakeService.countByMasteryRange(accountId, 60, 80);
        long proficient = mistakeService.countByMasteryRange(accountId, 80, 100);
        long mastered = mistakeService.countByMasteryRange(accountId, 100, null);

        return MasteryDistributionResp.builder()
                .stranger((int) stranger)
                .beginner((int) beginner)
                .basic((int) basic)
                .proficient((int) proficient)
                .mastered((int) mastered)
                .build();
    }

    /**
     * 5.4 每日复习完成率（近 N 天，默认 30）
     */
    public List<DailyCompletionResp> dailyCompletion(int days) {

        Long accountId = threadStorageUtil.getCurAccountId();
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);

        List<ReviewPlan> plans = reviewPlanService.listByAccountAndDateRange(accountId, start, today);

        Map<LocalDate, List<ReviewPlan>> groupedByDate = plans.stream()
                .collect(Collectors.groupingBy(ReviewPlan::getPlannedDate));

        List<DailyCompletionResp> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            List<ReviewPlan> dayPlans = groupedByDate.get(date);

            if (CollectionUtils.isEmpty(dayPlans)) {
                result.add(DailyCompletionResp.builder()
                        .date(date.format(DATE_FMT))
                        .totalPlanned(0)
                        .completed(0)
                        .completionRate(null)
                        .build());
                continue;
            }

            int totalPlanned = dayPlans.size();
            long completed = dayPlans.stream()
                    .filter(p -> ReviewPlanStatus.COMPLETED.getCode().equals(p.getStatus()))
                    .count();
            double rate = totalPlanned > 0 ? Math.round((double) completed / totalPlanned * 10000.0) / 10000.0 : 0.0;

            result.add(DailyCompletionResp.builder()
                    .date(date.format(DATE_FMT))
                    .totalPlanned(totalPlanned)
                    .completed((int) completed)
                    .completionRate(rate)
                    .build());
        }

        return result;
    }

    /**
     * 5.5 连续复习天数
     */
    public StreakResp streak() {

        Long accountId = threadStorageUtil.getCurAccountId();
        List<LocalDate> completedDates = reviewPlanService.listCompletedDates(accountId);

        if (CollectionUtils.isEmpty(completedDates)) {
            return StreakResp.builder().currentStreak(0).longestStreak(0).build();
        }

        LocalDate yesterday = LocalDate.now().minusDays(1);

        int currentStreak = 0;
        int longestStreak = 0;
        int tempStreak = 0;
        LocalDate prevDate = null;

        for (LocalDate date : completedDates) {
            if (prevDate == null) {
                tempStreak = 1;
            } else if (prevDate.minusDays(1).equals(date)) {
                tempStreak++;
            } else {
                longestStreak = Math.max(longestStreak, tempStreak);
                tempStreak = 1;
            }
            prevDate = date;
        }
        longestStreak = Math.max(longestStreak, tempStreak);

        // 计算当前连续：从昨天或今天开始往前数
        LocalDate today = LocalDate.now();
        LocalDate checkDate = completedDates.get(0);

        if (checkDate.equals(today) || checkDate.equals(yesterday)) {
            currentStreak = 1;
            for (int i = 1; i < completedDates.size(); i++) {
                if (completedDates.get(i).equals(completedDates.get(i - 1).minusDays(1))) {
                    currentStreak++;
                } else {
                    break;
                }
            }
        }

        return StreakResp.builder()
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .build();
    }

    /**
     * 5.6 复习效果趋势（近 N 天，默认 30）
     * 逐日补齐：返回长度恒等于 days；无复习记录日 avgMastery=null，前端按断线处理
     */
    public List<MasteryTrendResp> masteryTrend(int days) {

        Long accountId = threadStorageUtil.getCurAccountId();
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);

        List<ReviewRecord> records = reviewRecordService.listByAccountAndTimeRange(
                accountId, start.atStartOfDay(), today.atTime(LocalTime.MAX));

        Map<LocalDate, List<ReviewRecord>> groupedByDate = CollectionUtils.isEmpty(records)
                ? new HashMap<>()
                : records.stream().collect(Collectors.groupingBy(r -> r.getReviewTime().toLocalDate()));

        List<MasteryTrendResp> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            List<ReviewRecord> dayRecords = groupedByDate.get(date);

            if (CollectionUtils.isEmpty(dayRecords)) {
                result.add(MasteryTrendResp.builder()
                        .date(date.format(DATE_FMT))
                        .avgMastery(null)
                        .build());
                continue;
            }

            double avg = dayRecords.stream()
                    .mapToInt(ReviewRecord::getMasteryAfter)
                    .average()
                    .orElse(0.0);

            result.add(MasteryTrendResp.builder()
                    .date(date.format(DATE_FMT))
                    .avgMastery(Math.round(avg * 100.0) / 100.0)
                    .build());
        }

        return result;
    }

    /**
     * 5.8 管理端每日复习完成率（近30天，全平台）
     */
    public List<DailyCompletionResp> adminDailyCompletion() {

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(29);

        List<ReviewPlan> plans = reviewPlanService.listAllByDateRange(start, today);

        Map<LocalDate, List<ReviewPlan>> groupedByDate = plans.stream()
                .collect(Collectors.groupingBy(ReviewPlan::getPlannedDate));

        List<DailyCompletionResp> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            List<ReviewPlan> dayPlans = groupedByDate.get(date);

            if (CollectionUtils.isEmpty(dayPlans)) {
                result.add(DailyCompletionResp.builder()
                        .date(date.format(DATE_FMT))
                        .totalPlanned(0)
                        .completed(0)
                        .completionRate(null)
                        .build());
                continue;
            }

            int totalPlanned = dayPlans.size();
            long completed = dayPlans.stream()
                    .filter(p -> ReviewPlanStatus.COMPLETED.getCode().equals(p.getStatus()))
                    .count();
            double rate = totalPlanned > 0 ? Math.round((double) completed / totalPlanned * 10000.0) / 10000.0 : 0.0;

            result.add(DailyCompletionResp.builder()
                    .date(date.format(DATE_FMT))
                    .totalPlanned(totalPlanned)
                    .completed((int) completed)
                    .completionRate(rate)
                    .build());
        }

        return result;
    }

    /**
     * 5.9 管理端学科分布统计（全平台）
     */
    public List<SubjectStatsResp> adminSubject() {

        List<Mistake> mistakes = mistakeService.listAllValidIdAndMastery();
        if (CollectionUtils.isEmpty(mistakes)) {
            return new ArrayList<>();
        }

        List<Long> mistakeIds = mistakes.stream().map(Mistake::getId).collect(Collectors.toList());
        Map<Long, Integer> masteryMap = mistakes.stream()
                .collect(Collectors.toMap(Mistake::getId, Mistake::getMasteryLevel));

        Map<Long, String> subjectMap = mistakeTagService.getSubjectNamesByMistakeIds(mistakeIds);

        Map<String, List<Long>> grouped = mistakeIds.stream()
                .collect(Collectors.groupingBy(id -> subjectMap.getOrDefault(id, "未分类")));

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<Long> ids = entry.getValue();
                    double avgMastery = ids.stream()
                            .mapToInt(id -> masteryMap.getOrDefault(id, 0))
                            .average()
                            .orElse(0.0);
                    return SubjectStatsResp.builder()
                            .subject(entry.getKey())
                            .count(ids.size())
                            .avgMastery(Math.round(avgMastery * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparingInt(SubjectStatsResp::getCount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 管理端掌握度分布（全平台）
     */
    public MasteryDistributionResp adminMastery() {

        long stranger = mistakeService.countAllByMasteryRange(0, 20);
        long beginner = mistakeService.countAllByMasteryRange(20, 60);
        long basic = mistakeService.countAllByMasteryRange(60, 80);
        long proficient = mistakeService.countAllByMasteryRange(80, 100);
        long mastered = mistakeService.countAllByMasteryRange(100, null);

        return MasteryDistributionResp.builder()
                .stranger((int) stranger)
                .beginner((int) beginner)
                .basic((int) basic)
                .proficient((int) proficient)
                .mastered((int) mastered)
                .build();
    }

    /**
     * 5.7 管理端全局统计
     */
    public AdminOverviewResp adminOverview() {

        long totalUsers = accountService.count();
        long totalMistakes = mistakeService.countAllValid();

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);

        long activeUsersToday = reviewRecordService.countActiveUsersToday(dayStart, dayEnd);

        // 近 7 天平均完成率
        LocalDate weekStart = today.minusDays(6);
        List<ReviewPlan> weekPlans = reviewPlanService.listAllByDateRange(weekStart, today);

        double avgCompletionRate = 0.0;
        if (CollectionUtils.isNotEmpty(weekPlans)) {
            long completedCount = weekPlans.stream()
                    .filter(p -> ReviewPlanStatus.COMPLETED.getCode().equals(p.getStatus()))
                    .count();
            avgCompletionRate = Math.round((double) completedCount / weekPlans.size() * 10000.0) / 10000.0;
        }

        // 全平台平均记忆保持率
        List<Mistake> allMistakes = mistakeService.listAllValidWithReviewFields();
        double avgRetention = 0.0;
        if (CollectionUtils.isNotEmpty(allMistakes)) {
            LocalDateTime now = LocalDateTime.now();
            double sum = allMistakes.stream().mapToDouble(m -> calcRetentionForMistake(m, now)).sum();
            avgRetention = Math.round((sum / allMistakes.size()) * 10000.0) / 10000.0;
        }

        return AdminOverviewResp.builder()
                .totalUsers((int) totalUsers)
                .activeUsersToday((int) activeUsersToday)
                .totalMistakes((int) totalMistakes)
                .avgCompletionRate(avgCompletionRate)
                .avgRetention(avgRetention)
                .build();
    }

    /**
     * 单题遗忘曲线数据
     */
    public ForgettingCurveResp forgettingCurve(Long mistakeId) {

        Mistake mistake = mistakeService.getById(mistakeId);
        if (mistake == null) {
            return ForgettingCurveResp.builder().mistakeId(mistakeId).segments(new ArrayList<>()).build();
        }

        List<ReviewRecord> records = reviewRecordService.listRawByMistakeAsc(mistakeId);
        LocalDateTime now = LocalDateTime.now();

        List<CurveSegment> segments = new ArrayList<>();
        LocalDateTime segStart = mistake.getCreatedTime();
        int currentStage = 0;

        if (CollectionUtils.isNotEmpty(records)) {
            // 第一段：从创建到第一次复习
            segments.add(CurveSegment.builder()
                    .startTime(segStart.format(DATETIME_FMT))
                    .stability(STABILITY_FACTOR)
                    .stageAfter(0)
                    .isCorrect(null)
                    .endTime(records.get(0).getReviewTime().format(DATETIME_FMT))
                    .build());

            for (int i = 0; i < records.size(); i++) {
                ReviewRecord rec = records.get(i);
                int stageAfter = rec.getReviewStageAfter() != null ? rec.getReviewStageAfter() : 0;
                double stability = calcStability(stageAfter);
                LocalDateTime endTime = (i < records.size() - 1) ? records.get(i + 1).getReviewTime() : now;

                segments.add(CurveSegment.builder()
                        .startTime(rec.getReviewTime().format(DATETIME_FMT))
                        .stability(stability)
                        .stageAfter(stageAfter)
                        .isCorrect(rec.getIsCorrect() != null && rec.getIsCorrect() == 1)
                        .endTime(endTime.format(DATETIME_FMT))
                        .build());

                currentStage = stageAfter;
            }
        } else {
            // 无复习记录：从创建到现在的单段衰减
            segments.add(CurveSegment.builder()
                    .startTime(segStart.format(DATETIME_FMT))
                    .stability(STABILITY_FACTOR)
                    .stageAfter(0)
                    .isCorrect(null)
                    .endTime(now.format(DATETIME_FMT))
                    .build());
        }

        double currentRetention = calcRetentionForMistake(mistake, now);
        double currentStability = calcStability(currentStage);

        CurvePrediction prediction = CurvePrediction.builder()
                .fromTime(now.format(DATETIME_FMT))
                .fromRetention(currentRetention)
                .stability(currentStability)
                .daysToShow(PREDICTION_DAYS)
                .build();

        return ForgettingCurveResp.builder()
                .mistakeId(mistakeId)
                .createdTime(mistake.getCreatedTime().format(DATETIME_FMT))
                .currentRetention(Math.round(currentRetention * 10000.0) / 10000.0)
                .currentStage(currentStage)
                .nextReviewTime(mistake.getNextReviewTime() != null ? mistake.getNextReviewTime().format(DATETIME_FMT) : null)
                .segments(segments)
                .prediction(prediction)
                .build();
    }

    /**
     * 记忆健康总览
     */
    public MemoryHealthResp memoryHealth() {

        Long accountId = threadStorageUtil.getCurAccountId();
        LocalDateTime now = LocalDateTime.now();

        List<Mistake> mistakes = mistakeService.listValidWithReviewFields(accountId);
        if (CollectionUtils.isEmpty(mistakes)) {
            return MemoryHealthResp.builder()
                    .overallRetention(0.0)
                    .totalActive(0)
                    .dangerCount(0)
                    .warningCount(0)
                    .safeCount(0)
                    .distribution(buildEmptyDistribution())
                    .upcoming(buildEmptyUpcoming())
                    .build();
        }

        double retentionSum = 0;
        int danger = 0;
        int warning = 0;
        int safe = 0;
        int[] buckets = new int[5];

        LocalDate today = LocalDate.now();
        int[] upcomingCounts = new int[7];

        for (Mistake m : mistakes) {
            double r = calcRetentionForMistake(m, now);
            retentionSum += r;

            if (r < 0.3) danger++;
            else if (r < 0.7) warning++;
            else safe++;

            int pct = (int) (r * 100);
            int bucket = Math.min(pct / 20, 4);
            buckets[bucket]++;

            if (m.getNextReviewTime() != null) {
                long daysUntil = ChronoUnit.DAYS.between(today, m.getNextReviewTime().toLocalDate());
                if (daysUntil >= 0 && daysUntil < 7) {
                    upcomingCounts[(int) daysUntil]++;
                }
            }
        }

        double avgRetention = retentionSum / mistakes.size();

        String[] labels = {"0-20%", "20-40%", "40-60%", "60-80%", "80-100%"};
        List<RetentionBucket> distribution = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            distribution.add(RetentionBucket.builder().range(labels[i]).count(buckets[i]).build());
        }

        List<UpcomingReview> upcoming = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            upcoming.add(UpcomingReview.builder().daysFromNow(i).count(upcomingCounts[i]).build());
        }

        return MemoryHealthResp.builder()
                .overallRetention(Math.round(avgRetention * 10000.0) / 10000.0)
                .totalActive(mistakes.size())
                .dangerCount(danger)
                .warningCount(warning)
                .safeCount(safe)
                .distribution(distribution)
                .upcoming(upcoming)
                .build();
    }

    /**
     * 管理端记忆保持率趋势（近30天）
     */
    public List<RetentionTrendResp> adminRetentionTrend() {

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(29);

        List<Mistake> allMistakes = mistakeService.listAllValidWithReviewFields();
        if (CollectionUtils.isEmpty(allMistakes)) {
            List<RetentionTrendResp> empty = new ArrayList<>();
            for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
                empty.add(RetentionTrendResp.builder().date(d.format(DATE_FMT)).avgRetention(null).build());
            }
            return empty;
        }

        List<RetentionTrendResp> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            LocalDateTime dateTime = date.atTime(LocalTime.MAX);
            double sum = 0;
            int count = 0;

            for (Mistake m : allMistakes) {
                if (m.getCreatedTime() != null && m.getCreatedTime().toLocalDate().isAfter(date)) {
                    continue;
                }
                double r = calcRetentionForMistake(m, dateTime);
                sum += r;
                count++;
            }

            Double avg = count > 0 ? Math.round((sum / count) * 10000.0) / 10000.0 : null;
            result.add(RetentionTrendResp.builder().date(date.format(DATE_FMT)).avgRetention(avg).build());
        }

        return result;
    }

    // ===== 工具方法 =====

    /**
     * 根据阶段获取间隔天数（动态配置优先）
     */
    private int getIntervalByStage(int stageIndex) {

        String intervalsStr = systemConfigService.getByKey(CFG_INTERVALS, DEFAULT_INTERVALS);
        String[] parts = intervalsStr.split(",");

        if (stageIndex >= 0 && stageIndex < parts.length) {
            try {
                return Integer.parseInt(parts[stageIndex].trim());
            } catch (NumberFormatException e) {
                // 回退到枚举默认值
            }
        }

        return ReviewStage.getIntervalByStage(stageIndex);
    }

    /**
     * 根据阶段计算记忆稳定性 S
     */
    private double calcStability(int stage) {

        int interval = getIntervalByStage(stage);
        return interval > 0 ? interval * STABILITY_FACTOR : STABILITY_FACTOR;
    }

    /**
     * 计算某题在指定时刻的记忆保持率
     */
    private double calcRetentionForMistake(Mistake m, LocalDateTime atTime) {

        LocalDateTime lastReview = m.getLastReviewTime();
        if (lastReview == null) {
            lastReview = m.getCreatedTime();
        }
        if (lastReview == null) {
            return 1.0;
        }

        double t = Duration.between(lastReview, atTime).toMinutes() / (60.0 * 24.0);
        if (t < 0) {
            return 1.0;
        }
        int stage = m.getReviewStage() != null ? m.getReviewStage() : 0;
        double S = calcStability(stage);
        return Math.max(0, Math.min(1, Math.exp(-t / S)));
    }

    private List<RetentionBucket> buildEmptyDistribution() {

        String[] labels = {"0-20%", "20-40%", "40-60%", "60-80%", "80-100%"};
        List<RetentionBucket> list = new ArrayList<>();
        for (String label : labels) {
            list.add(RetentionBucket.builder().range(label).count(0).build());
        }
        return list;
    }

    private List<UpcomingReview> buildEmptyUpcoming() {

        List<UpcomingReview> list = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            list.add(UpcomingReview.builder().daysFromNow(i).count(0).build());
        }
        return list;
    }
}
