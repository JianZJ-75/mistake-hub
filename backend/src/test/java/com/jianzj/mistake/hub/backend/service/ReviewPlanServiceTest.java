package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.jianzj.mistake.hub.backend.entity.ReviewPlan;
import com.jianzj.mistake.hub.backend.enums.ReviewPlanStatus;
import com.jianzj.mistake.hub.backend.mapper.ReviewPlanMapper;
import com.jianzj.mistake.hub.backend.support.MockChainHelper;
import com.jianzj.mistake.hub.common.convention.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <p>ReviewPlanService 单元测试</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@ExtendWith(MockitoExtension.class)
class ReviewPlanServiceTest {

    @Mock
    private ReviewPlanMapper mockReviewPlanMapper;

    private ReviewPlanService reviewPlanService;

    @BeforeEach
    void setUp() {

        reviewPlanService = spy(new ReviewPlanService());
        ReflectionTestUtils.setField(reviewPlanService, "baseMapper", mockReviewPlanMapper);
    }

    // ===== batchCreate =====

    @Test
    void batchCreate_emptyList_shouldSkip() {

        reviewPlanService.batchCreate(List.of());

        verify(reviewPlanService, never()).saveBatch(any());
    }

    @Test
    void batchCreate_success() {

        ReviewPlan plan = ReviewPlan.builder()
                .accountId(100L).mistakeId(1L).plannedDate(LocalDate.now())
                .status(ReviewPlanStatus.PENDING.getCode()).build();

        doReturn(true).when(reviewPlanService).saveBatch(any());

        reviewPlanService.batchCreate(List.of(plan));

        verify(reviewPlanService).saveBatch(any());
    }

    // ===== updateStatus =====

    @Test
    void updateStatus_success() {

        ReviewPlan plan = ReviewPlan.builder()
                .id(1L).status(ReviewPlanStatus.PENDING.getCode()).build();

        // getByIdOrThrow uses lambdaQuery
        stubLambdaQueryChain(List.of(plan));
        doReturn(true).when(reviewPlanService).updateById(any(ReviewPlan.class));

        reviewPlanService.updateStatus(1L, ReviewPlanStatus.COMPLETED.getCode());

        assertThat(plan.getStatus()).isEqualTo(ReviewPlanStatus.COMPLETED.getCode());
    }

    @Test
    void updateStatus_notExist_shouldThrow() {

        stubLambdaQueryChain(List.of());

        assertThatThrownBy(() -> reviewPlanService.updateStatus(999L, ReviewPlanStatus.COMPLETED.getCode()))
                .isInstanceOf(BaseException.class);
    }

    // ===== getPlannedMistakeIds =====

    @Test
    void getPlannedMistakeIds_shouldReturnCorrectSet() {

        ReviewPlan p1 = ReviewPlan.builder().mistakeId(1L).build();
        ReviewPlan p2 = ReviewPlan.builder().mistakeId(2L).build();
        ReviewPlan p3 = ReviewPlan.builder().mistakeId(1L).build();

        stubLambdaQueryChain(List.of(p1, p2, p3));

        Set<Long> ids = reviewPlanService.getPlannedMistakeIds(100L, LocalDate.now());

        assertThat(ids).containsExactlyInAnyOrder(1L, 2L);
    }

    // ===== skipPendingPlansByMistake =====

    @Test
    void skipPendingPlansByMistake_shouldSetSkipped() {

        ReviewPlan plan = ReviewPlan.builder()
                .id(1L).mistakeId(5L).status(ReviewPlanStatus.PENDING.getCode()).build();

        stubLambdaQueryChain(List.of(plan));
        doReturn(true).when(reviewPlanService).updateById(any(ReviewPlan.class));

        reviewPlanService.skipPendingPlansByMistake(5L, LocalDate.now());

        assertThat(plan.getStatus()).isEqualTo(ReviewPlanStatus.SKIPPED.getCode());
    }

    // ===== listCompletedDates =====

    @Test
    void listCompletedDates_shouldReturnDates() {

        ReviewPlan p1 = ReviewPlan.builder().plannedDate(LocalDate.of(2026, 4, 14)).build();
        ReviewPlan p2 = ReviewPlan.builder().plannedDate(LocalDate.of(2026, 4, 13)).build();

        stubLambdaQueryChain(List.of(p1, p2));

        List<LocalDate> dates = reviewPlanService.listCompletedDates(100L);

        assertThat(dates).hasSize(2);
    }

    // ===== getByMistakeAndDate =====

    @Test
    void getByMistakeAndDate_found() {

        ReviewPlan plan = ReviewPlan.builder()
                .id(1L).accountId(100L).mistakeId(5L)
                .plannedDate(LocalDate.now()).build();

        stubLambdaQueryChain(List.of(plan));

        ReviewPlan result = reviewPlanService.getByMistakeAndDate(100L, 5L, LocalDate.now());

        assertThat(result).isNotNull();
        assertThat(result.getMistakeId()).isEqualTo(5L);
    }

    // ===== 工具方法 =====

    private void stubLambdaQueryChain(List<ReviewPlan> result) {

        LambdaQueryChainWrapper<ReviewPlan> chain = MockChainHelper.mockChain();
        doReturn(chain).when(reviewPlanService).lambdaQuery();
        when(chain.list()).thenReturn(result);
    }
}
