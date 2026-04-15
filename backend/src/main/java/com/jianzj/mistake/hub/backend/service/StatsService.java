package com.jianzj.mistake.hub.backend.service;

import com.jianzj.mistake.hub.backend.dto.resp.AdminOverviewResp;
import com.jianzj.mistake.hub.backend.dto.resp.DailyCompletionResp;
import com.jianzj.mistake.hub.backend.dto.resp.MasteryDistributionResp;
import com.jianzj.mistake.hub.backend.dto.resp.MasteryTrendResp;
import com.jianzj.mistake.hub.backend.dto.resp.StatsOverviewResp;
import com.jianzj.mistake.hub.backend.dto.resp.StreakResp;
import com.jianzj.mistake.hub.backend.dto.resp.SubjectStatsResp;
import com.jianzj.mistake.hub.backend.entity.Mistake;
import com.jianzj.mistake.hub.backend.entity.ReviewPlan;
import com.jianzj.mistake.hub.backend.entity.ReviewRecord;
import com.jianzj.mistake.hub.backend.enums.ReviewPlanStatus;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
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

    private final MistakeService mistakeService;

    private final ReviewPlanService reviewPlanService;

    private final ReviewRecordService reviewRecordService;

    private final AccountService accountService;

    private final MistakeTagService mistakeTagService;

    private final ThreadStorageUtil threadStorageUtil;

    public StatsService(MistakeService mistakeService,
                        ReviewPlanService reviewPlanService,
                        ReviewRecordService reviewRecordService,
                        AccountService accountService,
                        MistakeTagService mistakeTagService,
                        ThreadStorageUtil threadStorageUtil) {

        this.mistakeService = mistakeService;
        this.reviewPlanService = reviewPlanService;
        this.reviewRecordService = reviewRecordService;
        this.accountService = accountService;
        this.mistakeTagService = mistakeTagService;
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

        long notMastered = mistakeService.countByMasteryRange(accountId, 0, 60);
        long learning = mistakeService.countByMasteryRange(accountId, 60, 80);
        long mastered = mistakeService.countByMasteryRange(accountId, 80, null);

        return MasteryDistributionResp.builder()
                .notMastered((int) notMastered)
                .learning((int) learning)
                .mastered((int) mastered)
                .build();
    }

    /**
     * 5.4 每日复习完成率（近30天）
     */
    public List<DailyCompletionResp> dailyCompletion() {

        Long accountId = threadStorageUtil.getCurAccountId();
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(29);

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
     * 5.6 复习效果趋势（近30天）
     */
    public List<MasteryTrendResp> masteryTrend() {

        Long accountId = threadStorageUtil.getCurAccountId();
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(29);

        List<ReviewRecord> records = reviewRecordService.listByAccountAndTimeRange(
                accountId, start.atStartOfDay(), today.atTime(LocalTime.MAX));

        if (CollectionUtils.isEmpty(records)) {
            return new ArrayList<>();
        }

        Map<String, List<ReviewRecord>> groupedByDate = records.stream()
                .collect(Collectors.groupingBy(r -> r.getReviewTime().toLocalDate().format(DATE_FMT)));

        return groupedByDate.entrySet().stream()
                .map(entry -> {
                    double avg = entry.getValue().stream()
                            .mapToInt(ReviewRecord::getMasteryAfter)
                            .average()
                            .orElse(0.0);
                    return MasteryTrendResp.builder()
                            .date(entry.getKey())
                            .avgMastery(Math.round(avg * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparing(MasteryTrendResp::getDate))
                .collect(Collectors.toList());
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

        long notMastered = mistakeService.countAllByMasteryRange(0, 60);
        long learning = mistakeService.countAllByMasteryRange(60, 80);
        long mastered = mistakeService.countAllByMasteryRange(80, null);

        return MasteryDistributionResp.builder()
                .notMastered((int) notMastered)
                .learning((int) learning)
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

        return AdminOverviewResp.builder()
                .totalUsers((int) totalUsers)
                .activeUsersToday((int) activeUsersToday)
                .totalMistakes((int) totalMistakes)
                .avgCompletionRate(avgCompletionRate)
                .build();
    }
}
