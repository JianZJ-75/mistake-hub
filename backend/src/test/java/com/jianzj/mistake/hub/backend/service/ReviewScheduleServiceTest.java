package com.jianzj.mistake.hub.backend.service;

import com.jianzj.mistake.hub.backend.dto.req.ReviewHistoryReq;
import com.jianzj.mistake.hub.backend.dto.req.ReviewSubmitReq;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewRecordResp;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewSubmitResp;
import com.jianzj.mistake.hub.backend.entity.Mistake;
import com.jianzj.mistake.hub.backend.entity.ReviewPlan;
import com.jianzj.mistake.hub.backend.entity.ReviewRecord;
import com.jianzj.mistake.hub.backend.enums.ReviewPlanStatus;
import com.jianzj.mistake.hub.common.convention.exception.BaseException;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <p>ReviewScheduleService 单元测试</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@ExtendWith(MockitoExtension.class)
class ReviewScheduleServiceTest {

    @InjectMocks
    private ReviewScheduleService reviewScheduleService;

    @Mock
    private MistakeService mistakeService;

    @Mock
    private AccountService accountService;

    @Mock
    private ReviewPlanService reviewPlanService;

    @Mock
    private ReviewRecordService reviewRecordService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private ThreadStorageUtil threadStorageUtil;

    // ===== submitReview =====

    @Test
    void submitReview_shouldSaveNoteAndNoteImageUrl() {

        Long accountId = 100L;
        Long mistakeId = 5L;
        stubSubmitReviewCommon(accountId, mistakeId, true, 2, 40);

        ReviewSubmitReq req = new ReviewSubmitReq();
        req.setMistakeId(mistakeId);
        req.setIsCorrect(true);
        req.setNote("这题要注意符号");
        req.setNoteImageUrl("note.jpg");

        reviewScheduleService.submitReview(req);

        ArgumentCaptor<ReviewRecord> captor = ArgumentCaptor.forClass(ReviewRecord.class);
        verify(reviewRecordService).createRecord(captor.capture());
        ReviewRecord record = captor.getValue();

        assertThat(record.getNote()).isEqualTo("这题要注意符号");
        assertThat(record.getNoteImageUrl()).isEqualTo("note.jpg");
    }

    @Test
    void submitReview_noNote_shouldSaveNullNote() {

        Long accountId = 100L;
        Long mistakeId = 5L;
        stubSubmitReviewCommon(accountId, mistakeId, true, 2, 40);

        ReviewSubmitReq req = new ReviewSubmitReq();
        req.setMistakeId(mistakeId);
        req.setIsCorrect(true);
        // 不传 note 和 noteImageUrl

        reviewScheduleService.submitReview(req);

        ArgumentCaptor<ReviewRecord> captor = ArgumentCaptor.forClass(ReviewRecord.class);
        verify(reviewRecordService).createRecord(captor.capture());
        ReviewRecord record = captor.getValue();

        assertThat(record.getNote()).isNull();
        assertThat(record.getNoteImageUrl()).isNull();
    }

    @Test
    void submitReview_correct_stageUpMasteryUp() {

        Long accountId = 100L;
        Long mistakeId = 5L;
        // before: stage=2, mastery=40 → correct → stage=3, mastery=60
        stubSubmitReviewCommon(accountId, mistakeId, true, 2, 40);

        ReviewSubmitReq req = new ReviewSubmitReq();
        req.setMistakeId(mistakeId);
        req.setIsCorrect(true);

        ReviewSubmitResp resp = reviewScheduleService.submitReview(req);

        assertThat(resp.getReviewStageBefore()).isEqualTo(2);
        assertThat(resp.getMasteryBefore()).isEqualTo(40);
        // after 快照：stage=3, mastery=60
        assertThat(resp.getReviewStageAfter()).isEqualTo(3);
        assertThat(resp.getMasteryAfter()).isEqualTo(60);
    }

    @Test
    void submitReview_wrong_stageDownMasteryDown() {

        Long accountId = 100L;
        Long mistakeId = 5L;
        // before: stage=3, mastery=40 → wrong → stage=1, mastery=25
        stubSubmitReviewCommon(accountId, mistakeId, false, 3, 40);

        ReviewSubmitReq req = new ReviewSubmitReq();
        req.setMistakeId(mistakeId);
        req.setIsCorrect(false);

        ReviewSubmitResp resp = reviewScheduleService.submitReview(req);

        assertThat(resp.getReviewStageBefore()).isEqualTo(3);
        assertThat(resp.getMasteryBefore()).isEqualTo(40);
        assertThat(resp.getReviewStageAfter()).isEqualTo(1);
        assertThat(resp.getMasteryAfter()).isEqualTo(25);
    }

    @Test
    void submitReview_alreadyCompleted_shouldThrow() {

        Long accountId = 100L;
        Long mistakeId = 5L;
        when(threadStorageUtil.getCurAccountId()).thenReturn(accountId);

        when(mistakeService.getValidMistakeForCurrentUser(mistakeId))
                .thenReturn(Mistake.builder().id(mistakeId).accountId(accountId).build());

        ReviewPlan plan = ReviewPlan.builder()
                .id(10L).accountId(accountId).mistakeId(mistakeId)
                .plannedDate(LocalDate.now()).status(ReviewPlanStatus.COMPLETED.getCode()).build();
        when(reviewPlanService.getByMistakeAndDate(accountId, mistakeId, LocalDate.now())).thenReturn(plan);

        ReviewSubmitReq req = new ReviewSubmitReq();
        req.setMistakeId(mistakeId);
        req.setIsCorrect(true);

        assertThatThrownBy(() -> reviewScheduleService.submitReview(req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void submitReview_noPlan_shouldThrow() {

        Long accountId = 100L;
        Long mistakeId = 5L;
        when(threadStorageUtil.getCurAccountId()).thenReturn(accountId);

        when(mistakeService.getValidMistakeForCurrentUser(mistakeId))
                .thenReturn(Mistake.builder().id(mistakeId).accountId(accountId).build());

        when(reviewPlanService.getByMistakeAndDate(accountId, mistakeId, LocalDate.now())).thenReturn(null);

        ReviewSubmitReq req = new ReviewSubmitReq();
        req.setMistakeId(mistakeId);
        req.setIsCorrect(true);

        assertThatThrownBy(() -> reviewScheduleService.submitReview(req))
                .isInstanceOf(BaseException.class);
    }

    // ===== getReviewHistory =====

    @Test
    void getReviewHistory_shouldDelegateToRecordService() {

        Long accountId = 100L;

        when(mistakeService.getValidMistake(5L))
                .thenReturn(Mistake.builder().id(5L).accountId(accountId).build());

        when(reviewRecordService.listByMistake(accountId, 5L)).thenReturn(List.of());

        ReviewHistoryReq req = new ReviewHistoryReq();
        req.setMistakeId(5L);

        reviewScheduleService.getReviewHistory(req);

        verify(mistakeService).getValidMistake(5L);
        verify(reviewRecordService).listByMistake(100L, 5L);
    }

    @Test
    void getReviewHistory_shouldReturnResults() {

        Long accountId = 100L;

        when(mistakeService.getValidMistake(5L))
                .thenReturn(Mistake.builder().id(5L).accountId(accountId).build());

        ReviewRecordResp resp1 = ReviewRecordResp.builder()
                .id(1L).isCorrect(1).note("备注1").noteImageUrl("n1.jpg").build();
        ReviewRecordResp resp2 = ReviewRecordResp.builder()
                .id(2L).isCorrect(0).note("备注2").noteImageUrl("n2.jpg").build();
        when(reviewRecordService.listByMistake(accountId, 5L)).thenReturn(List.of(resp1, resp2));

        ReviewHistoryReq req = new ReviewHistoryReq();
        req.setMistakeId(5L);

        List<ReviewRecordResp> result = reviewScheduleService.getReviewHistory(req);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNote()).isEqualTo("备注1");
        assertThat(result.get(1).getNoteImageUrl()).isEqualTo("n2.jpg");
    }

    // ===== updateMistakeAfterReview 边界测试 =====

    @Test
    void submitReview_correct_stage6_shouldCapAt6() {

        // stage=6, correct → 仍然=6（上限）
        stubSubmitReviewCommon(100L, 5L, true, 6, 80);

        ReviewSubmitReq req = new ReviewSubmitReq();
        req.setMistakeId(5L);
        req.setIsCorrect(true);

        ReviewSubmitResp resp = reviewScheduleService.submitReview(req);

        assertThat(resp.getReviewStageBefore()).isEqualTo(6);
        assertThat(resp.getReviewStageAfter()).isEqualTo(6);
    }

    @Test
    void submitReview_wrong_stage0_shouldCapAt0() {

        // stage=0, wrong → 仍然=0（下限）
        stubSubmitReviewCommon(100L, 5L, false, 0, 30);

        ReviewSubmitReq req = new ReviewSubmitReq();
        req.setMistakeId(5L);
        req.setIsCorrect(false);

        ReviewSubmitResp resp = reviewScheduleService.submitReview(req);

        assertThat(resp.getReviewStageBefore()).isEqualTo(0);
        assertThat(resp.getReviewStageAfter()).isEqualTo(0);
    }

    @Test
    void submitReview_correct_mastery100_shouldCapAt100() {

        // mastery=90, correct +20 → cap at 100
        stubSubmitReviewCommon(100L, 5L, true, 3, 90);

        ReviewSubmitReq req = new ReviewSubmitReq();
        req.setMistakeId(5L);
        req.setIsCorrect(true);

        ReviewSubmitResp resp = reviewScheduleService.submitReview(req);

        assertThat(resp.getMasteryBefore()).isEqualTo(90);
        assertThat(resp.getMasteryAfter()).isEqualTo(100);
    }

    @Test
    void submitReview_wrong_mastery0_shouldCapAt0() {

        // mastery=10, wrong -15 → cap at 0
        stubSubmitReviewCommon(100L, 5L, false, 3, 10);

        ReviewSubmitReq req = new ReviewSubmitReq();
        req.setMistakeId(5L);
        req.setIsCorrect(false);

        ReviewSubmitResp resp = reviewScheduleService.submitReview(req);

        assertThat(resp.getMasteryBefore()).isEqualTo(10);
        assertThat(resp.getMasteryAfter()).isEqualTo(0);
    }

    // ===== 工具方法 =====

    /**
     * submitReview 公共 stub：3 次 getById（before 快照 / updateMistakeAfterReview / after 快照）
     */
    @SuppressWarnings("unchecked")
    private void stubSubmitReviewCommon(Long accountId, Long mistakeId,
                                        boolean isCorrect, int stageBefore, int masteryBefore) {

        when(threadStorageUtil.getCurAccountId()).thenReturn(accountId);

        when(mistakeService.getValidMistakeForCurrentUser(mistakeId))
                .thenReturn(Mistake.builder().id(mistakeId).accountId(accountId).build());

        ReviewPlan plan = ReviewPlan.builder()
                .id(10L).accountId(accountId).mistakeId(mistakeId)
                .plannedDate(LocalDate.now()).status(ReviewPlanStatus.PENDING.getCode()).build();
        when(reviewPlanService.getByMistakeAndDate(accountId, mistakeId, LocalDate.now())).thenReturn(plan);

        // 计算期望的 after 值
        int stageAfter;
        int masteryAfter;
        if (isCorrect) {
            stageAfter = Math.min(stageBefore + 1, 6);
            masteryAfter = Math.min(masteryBefore + 20, 100);
        } else {
            stageAfter = Math.max(stageBefore - 2, 0);
            masteryAfter = Math.max(masteryBefore - 15, 0);
        }

        // 3 次 getById 调用：before 快照 / updateMistakeAfterReview 内部 / after 快照
        when(mistakeService.getById(mistakeId)).thenReturn(
                Mistake.builder().id(mistakeId).reviewStage(stageBefore).masteryLevel(masteryBefore).build(),
                Mistake.builder().id(mistakeId).reviewStage(stageBefore).masteryLevel(masteryBefore).build(),
                Mistake.builder().id(mistakeId).reviewStage(stageAfter).masteryLevel(masteryAfter)
                        .nextReviewTime(LocalDateTime.now().plusDays(4)).build()
        );
        when(mistakeService.updateById(any())).thenReturn(true);

        // 动态配置 mock
        when(systemConfigService.getIntByKey(eq("review.mastery_correct_delta"), anyInt())).thenReturn(20);
        when(systemConfigService.getIntByKey(eq("review.mastery_wrong_delta"), anyInt())).thenReturn(15);
        when(systemConfigService.getIntByKey(eq("review.stage_wrong_back"), anyInt())).thenReturn(2);
        when(systemConfigService.getByKey(eq("review.intervals"), anyString())).thenReturn("0,1,2,4,7,15,30");

        RBucket<String> bucket = mock(RBucket.class);
        doReturn(bucket).when(redissonClient).getBucket(any());
        when(bucket.get()).thenReturn(null);
    }
}
