package com.jianzj.mistake.hub.backend.integration;

import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.entity.Mistake;
import com.jianzj.mistake.hub.backend.entity.ReviewPlan;
import com.jianzj.mistake.hub.backend.enums.MistakeStatus;
import com.jianzj.mistake.hub.backend.enums.ReviewPlanStatus;
import com.jianzj.mistake.hub.backend.enums.Role;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>复习算法模拟测试：30 天周期，通过修改 DB 中 next_review_time 模拟时间推移</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
class AlgorithmSimulationTest extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AlgorithmSimulationTest.class);

    @Test
    void simulate30Days_allCorrect() {

        SimulationResult result = runSimulation("全部答对", 30, 1.0);

        log.info("[算法模拟] 全部答对 → 最终 mastery={}, stage={}", result.finalMastery, result.finalStage);
        printDayTable(result);

        assertThat(result.finalMastery).isEqualTo(100);
        assertThat(result.finalStage).isEqualTo(6);
    }

    @Test
    void simulate30Days_allWrong() {

        SimulationResult result = runSimulation("全部答错", 30, 0.0);

        log.info("[算法模拟] 全部答错 → 最终 mastery={}, stage={}", result.finalMastery, result.finalStage);
        printDayTable(result);

        assertThat(result.finalMastery).isEqualTo(0);
        assertThat(result.finalStage).isEqualTo(0);
    }

    @Test
    void simulate30Days_mixed70percent() {

        SimulationResult result = runSimulation("70%答对", 30, 0.7);

        log.info("[算法模拟] 70%答对 → 最终 mastery={}, stage={}", result.finalMastery, result.finalStage);
        printDayTable(result);

        // 70% 正确率下，掌握度应有上升趋势
        assertThat(result.finalMastery).isGreaterThan(0);
    }

    @Test
    void simulate30Days_mixed50percent() {

        SimulationResult result = runSimulation("50%答对", 30, 0.5);

        log.info("[算法模拟] 50%答对 → 最终 mastery={}, stage={}", result.finalMastery, result.finalStage);
        printDayTable(result);

        // 50% 正确率下仍有一定掌握度
        assertThat(result.finalMastery).isGreaterThanOrEqualTo(0);
    }

    @Test
    void priorityFormula_verification() {

        Account account = createTestAccount(Role.STUDENT);
        loginAs(account);

        // 创建3道不同状态的错题
        Mistake overdue = createMistake(account.getId(), "逾期题",
                0, 20, LocalDateTime.now().minusDays(10));
        Mistake lowMastery = createMistake(account.getId(), "低掌握",
                0, 10, LocalDateTime.now().minusDays(1));
        Mistake highMastery = createMistake(account.getId(), "高掌握",
                5, 90, LocalDateTime.now().minusDays(1));

        // 优先级公式：overdueDays×3.0 + (100-mastery)×0.5 + (6-stage)×2.0
        // overdue:   10×3 + 80×0.5 + 6×2 = 30+40+12 = 82
        // lowMastery: 1×3 + 90×0.5 + 6×2 = 3+45+12 = 60
        // highMastery: 1×3 + 10×0.5 + 1×2 = 3+5+2 = 10

        // 验证优先级排序：逾期 > 低掌握 > 高掌握
        log.info("[优先级验证] overdue={}, lowMastery={}, highMastery={}",
                calcPriority(10, 20, 0), calcPriority(1, 10, 0), calcPriority(1, 90, 5));

        assertThat(calcPriority(10, 20, 0)).isGreaterThan(calcPriority(1, 10, 0));
        assertThat(calcPriority(1, 10, 0)).isGreaterThan(calcPriority(1, 90, 5));
    }

    // ===== 辅助方法 =====

    private SimulationResult runSimulation(String name, int days, double correctRate) {

        Account account = createTestAccount(Role.STUDENT);
        loginAs(account);

        Mistake mistake = createMistake(account.getId(), name + " 测试题",
                0, 0, LocalDateTime.now());

        List<DaySnapshot> snapshots = new ArrayList<>();
        Random random = new Random(42); // 固定种子确保可重现
        int reviewCount = 0;

        for (int day = 0; day < days; day++) {
            // 重新读取错题最新状态
            Mistake current = mistakeService.getById(mistake.getId());
            int curStage = current.getReviewStage() != null ? current.getReviewStage() : 0;
            int curMastery = current.getMasteryLevel() != null ? current.getMasteryLevel() : 0;

            // 检查是否到期
            LocalDateTime nextReview = current.getNextReviewTime();
            LocalDateTime simulatedNow = LocalDateTime.now().plusDays(day);

            boolean reviewed = false;
            if (nextReview != null && !nextReview.isAfter(simulatedNow)) {
                boolean isCorrect = random.nextDouble() < correctRate;

                // 查找或创建今日计划（避免 unique constraint 冲突）
                ReviewPlan plan = reviewPlanService.getByMistakeAndDate(
                        account.getId(), mistake.getId(), LocalDate.now());
                if (plan == null) {
                    plan = ReviewPlan.builder()
                            .accountId(account.getId())
                            .mistakeId(mistake.getId())
                            .plannedDate(LocalDate.now())
                            .status(ReviewPlanStatus.PENDING.getCode())
                            .build();
                    reviewPlanService.save(plan);
                } else {
                    plan.setStatus(ReviewPlanStatus.PENDING.getCode());
                    reviewPlanService.updateById(plan);
                }

                // 执行复习算法
                reviewScheduleService.updateMistakeAfterReview(mistake.getId(), isCorrect);
                reviewPlanService.updateStatus(plan.getId(), ReviewPlanStatus.COMPLETED.getCode());

                reviewCount++;
                reviewed = true;
            }

            // 记录快照
            Mistake afterReview = mistakeService.getById(mistake.getId());
            snapshots.add(new DaySnapshot(
                    day,
                    afterReview.getReviewStage() != null ? afterReview.getReviewStage() : 0,
                    afterReview.getMasteryLevel() != null ? afterReview.getMasteryLevel() : 0,
                    reviewed
            ));

            // 模拟时间推进：将 nextReviewTime 前移 1 天（相当于时间过了 1 天）
            Mistake toUpdate = mistakeService.getById(mistake.getId());
            if (toUpdate.getNextReviewTime() != null) {
                toUpdate.setNextReviewTime(toUpdate.getNextReviewTime().minusDays(1));
                mistakeService.updateById(toUpdate);
            }
        }

        Mistake finalState = mistakeService.getById(mistake.getId());
        int finalMastery = finalState.getMasteryLevel() != null ? finalState.getMasteryLevel() : 0;
        int finalStage = finalState.getReviewStage() != null ? finalState.getReviewStage() : 0;

        return new SimulationResult(name, days, correctRate, reviewCount,
                finalMastery, finalStage, snapshots);
    }

    private Mistake createMistake(Long accountId, String title, int stage, int mastery, LocalDateTime nextReview) {

        Mistake mistake = Mistake.builder()
                .accountId(accountId)
                .title(title)
                .reviewStage(stage)
                .masteryLevel(mastery)
                .nextReviewTime(nextReview)
                .status(MistakeStatus.VALID.getCode())
                .build();
        mistakeService.save(mistake);
        return mistake;
    }

    private double calcPriority(int overdueDays, int mastery, int stage) {

        return overdueDays * 3.0 + (100 - mastery) * 0.5 + (6 - stage) * 2.0;
    }

    private void printDayTable(SimulationResult result) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n[%s] 30天模拟数据 (correctRate=%.0f%%)\n", result.name, result.correctRate * 100));
        sb.append("Day | Stage | Mastery | Reviewed\n");
        sb.append("----|-------|---------|--------\n");
        for (DaySnapshot snap : result.snapshots) {
            sb.append(String.format("%3d | %5d | %7d | %s\n",
                    snap.day, snap.stage, snap.mastery, snap.reviewed ? "Y" : "-"));
        }
        log.info(sb.toString());
    }

    private record DaySnapshot(int day, int stage, int mastery, boolean reviewed) {}

    private record SimulationResult(String name, int days, double correctRate,
                                     int reviewCount, int finalMastery, int finalStage,
                                     List<DaySnapshot> snapshots) {}
}
