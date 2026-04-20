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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * <p>StatsService 单元测试</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @InjectMocks
    private StatsService statsService;

    @Mock
    private MistakeService mistakeService;

    @Mock
    private ReviewPlanService reviewPlanService;

    @Mock
    private ReviewRecordService reviewRecordService;

    @Mock
    private AccountService accountService;

    @Mock
    private MistakeTagService mistakeTagService;

    @Mock
    private ThreadStorageUtil threadStorageUtil;

    // ===== overview =====

    @Test
    void overview_normal_shouldReturnCorrectCounts() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);
        when(mistakeService.countValidByAccountId(100L)).thenReturn(50L);
        when(mistakeService.countMasteredByAccountId(eq(100L), anyInt())).thenReturn(20L);
        when(mistakeService.countPendingReview(eq(100L), any(LocalDateTime.class))).thenReturn(10L);
        when(mistakeService.countNewThisWeek(eq(100L), any(LocalDateTime.class))).thenReturn(5L);

        StatsOverviewResp resp = statsService.overview();

        assertThat(resp.getTotalMistakes()).isEqualTo(50);
        assertThat(resp.getMasteredCount()).isEqualTo(20);
        assertThat(resp.getPendingReviewCount()).isEqualTo(10);
        assertThat(resp.getNewThisWeek()).isEqualTo(5);
    }

    @Test
    void overview_noData_shouldReturnZeros() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);
        when(mistakeService.countValidByAccountId(100L)).thenReturn(0L);
        when(mistakeService.countMasteredByAccountId(eq(100L), anyInt())).thenReturn(0L);
        when(mistakeService.countPendingReview(eq(100L), any(LocalDateTime.class))).thenReturn(0L);
        when(mistakeService.countNewThisWeek(eq(100L), any(LocalDateTime.class))).thenReturn(0L);

        StatsOverviewResp resp = statsService.overview();

        assertThat(resp.getTotalMistakes()).isEqualTo(0);
        assertThat(resp.getMasteredCount()).isEqualTo(0);
    }

    // ===== subject =====

    @Test
    void subject_groupBySubject_shouldAggregateCorrectly() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);

        Mistake m1 = Mistake.builder().id(1L).masteryLevel(60).build();
        Mistake m2 = Mistake.builder().id(2L).masteryLevel(80).build();
        Mistake m3 = Mistake.builder().id(3L).masteryLevel(40).build();
        when(mistakeService.listValidByAccountId(100L)).thenReturn(List.of(m1, m2, m3));

        when(mistakeTagService.getSubjectNamesByMistakeIds(List.of(1L, 2L, 3L)))
                .thenReturn(Map.of(1L, "数学", 2L, "数学", 3L, "英语"));

        List<SubjectStatsResp> result = statsService.subject();

        assertThat(result).hasSize(2);
        SubjectStatsResp math = result.stream()
                .filter(s -> "数学".equals(s.getSubject())).findFirst().orElseThrow();
        assertThat(math.getCount()).isEqualTo(2);
        assertThat(math.getAvgMastery()).isEqualTo(70.0);
    }

    // ===== mastery =====

    @Test
    void mastery_threeSegments_shouldReturnCorrectDistribution() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);
        when(mistakeService.countByMasteryRange(100L, 0, 60)).thenReturn(10L);
        when(mistakeService.countByMasteryRange(100L, 60, 80)).thenReturn(5L);
        when(mistakeService.countByMasteryRange(100L, 80, null)).thenReturn(3L);

        MasteryDistributionResp resp = statsService.mastery();

        assertThat(resp.getNotMastered()).isEqualTo(10);
        assertThat(resp.getLearning()).isEqualTo(5);
        assertThat(resp.getMastered()).isEqualTo(3);
    }

    // ===== dailyCompletion =====

    @Test
    void dailyCompletion_shouldCalculateRate() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);

        LocalDate today = LocalDate.now();
        ReviewPlan p1 = ReviewPlan.builder().plannedDate(today).status(ReviewPlanStatus.COMPLETED.getCode()).build();
        ReviewPlan p2 = ReviewPlan.builder().plannedDate(today).status(ReviewPlanStatus.PENDING.getCode()).build();

        when(reviewPlanService.listByAccountAndDateRange(eq(100L), any(LocalDate.class), eq(today)))
                .thenReturn(List.of(p1, p2));

        List<DailyCompletionResp> result = statsService.dailyCompletion(30);

        DailyCompletionResp todayResp = result.stream()
                .filter(d -> d.getDate().equals(today.toString())).findFirst().orElseThrow();
        assertThat(todayResp.getTotalPlanned()).isEqualTo(2);
        assertThat(todayResp.getCompleted()).isEqualTo(1);
        assertThat(todayResp.getCompletionRate()).isEqualTo(0.5);
    }

    // ===== streak =====

    @Test
    void streak_consecutive_shouldCalculateCorrectly() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);

        LocalDate today = LocalDate.now();
        // 降序：今天、昨天、前天
        when(reviewPlanService.listCompletedDates(100L))
                .thenReturn(List.of(today, today.minusDays(1), today.minusDays(2)));

        StreakResp resp = statsService.streak();

        assertThat(resp.getCurrentStreak()).isEqualTo(3);
        assertThat(resp.getLongestStreak()).isEqualTo(3);
    }

    @Test
    void streak_noDates_shouldReturnZeros() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);
        when(reviewPlanService.listCompletedDates(100L)).thenReturn(List.of());

        StreakResp resp = statsService.streak();

        assertThat(resp.getCurrentStreak()).isEqualTo(0);
        assertThat(resp.getLongestStreak()).isEqualTo(0);
    }

    // ===== masteryTrend =====

    @Test
    void masteryTrend_sparseRecords_shouldZeroPadMissingDays() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);

        LocalDate today = LocalDate.now();
        ReviewRecord r1 = ReviewRecord.builder()
                .reviewTime(today.minusDays(2).atTime(10, 0)).masteryAfter(30).build();
        ReviewRecord r2 = ReviewRecord.builder()
                .reviewTime(today.minusDays(1).atTime(10, 0)).masteryAfter(90).build();
        ReviewRecord r3 = ReviewRecord.builder()
                .reviewTime(today.atTime(10, 0)).masteryAfter(50).build();
        when(reviewRecordService.listByAccountAndTimeRange(eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(r1, r2, r3));

        List<MasteryTrendResp> result = statsService.masteryTrend(30);

        assertThat(result).hasSize(30);
        // 倒数第三天 = 30
        assertThat(result.get(27).getAvgMastery()).isEqualTo(30.0);
        // 倒数第二天 = 90
        assertThat(result.get(28).getAvgMastery()).isEqualTo(90.0);
        // 今天 = 50
        assertThat(result.get(29).getAvgMastery()).isEqualTo(50.0);
        // 其余日期都是 null
        assertThat(result.get(0).getAvgMastery()).isNull();
        assertThat(result.get(15).getAvgMastery()).isNull();
    }

    @Test
    void masteryTrend_noRecords_shouldReturnAllNull() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);
        when(reviewRecordService.listByAccountAndTimeRange(eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<MasteryTrendResp> result = statsService.masteryTrend(7);

        assertThat(result).hasSize(7);
        assertThat(result).allMatch(r -> r.getAvgMastery() == null);
    }

    // ===== adminOverview =====

    @Test
    void adminOverview_shouldReturnGlobalStats() {

        when(accountService.count()).thenReturn(100L);
        when(mistakeService.countAllValid()).thenReturn(500L);
        when(reviewRecordService.countActiveUsersToday(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(20L);

        ReviewPlan p1 = ReviewPlan.builder().status(ReviewPlanStatus.COMPLETED.getCode()).build();
        ReviewPlan p2 = ReviewPlan.builder().status(ReviewPlanStatus.PENDING.getCode()).build();
        when(reviewPlanService.listAllByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(p1, p2));

        AdminOverviewResp resp = statsService.adminOverview();

        assertThat(resp.getTotalUsers()).isEqualTo(100);
        assertThat(resp.getTotalMistakes()).isEqualTo(500);
        assertThat(resp.getActiveUsersToday()).isEqualTo(20);
        assertThat(resp.getAvgCompletionRate()).isEqualTo(0.5);
    }
}
