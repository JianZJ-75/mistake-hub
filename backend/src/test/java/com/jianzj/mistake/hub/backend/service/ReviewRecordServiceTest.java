package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewRecordResp;
import com.jianzj.mistake.hub.backend.entity.ReviewRecord;
import com.jianzj.mistake.hub.backend.mapper.ReviewRecordMapper;
import com.jianzj.mistake.hub.backend.support.MockChainHelper;
import com.jianzj.mistake.hub.common.convention.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <p>ReviewRecordService 单元测试</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@ExtendWith(MockitoExtension.class)
class ReviewRecordServiceTest {

    @Mock
    private ReviewRecordMapper mockReviewRecordMapper;

    @Mock
    private AccountService accountService;

    @Mock
    private MistakeService mistakeService;

    private ReviewRecordService reviewRecordService;

    @BeforeEach
    void setUp() {

        reviewRecordService = spy(new ReviewRecordService(accountService, mistakeService));
        ReflectionTestUtils.setField(reviewRecordService, "baseMapper", mockReviewRecordMapper);
    }

    // ===== createRecord =====

    @Test
    void createRecord_success() {

        doReturn(true).when(reviewRecordService).save(any(ReviewRecord.class));

        ReviewRecord record = ReviewRecord.builder()
                .accountId(100L).mistakeId(5L).isCorrect(1)
                .note("测试备注").noteImageUrl("note.jpg")
                .build();

        reviewRecordService.createRecord(record);

        verify(reviewRecordService).save(record);
    }

    @Test
    void createRecord_fail_shouldThrow() {

        doReturn(false).when(reviewRecordService).save(any(ReviewRecord.class));

        ReviewRecord record = ReviewRecord.builder()
                .accountId(100L).mistakeId(5L).isCorrect(1).build();

        assertThatThrownBy(() -> reviewRecordService.createRecord(record))
                .isInstanceOf(BaseException.class);
    }

    // ===== listByMistake =====

    @Test
    @SuppressWarnings("unchecked")
    void listByMistake_shouldReturnRespWithNoteFields() {

        LambdaQueryChainWrapper<ReviewRecord> chain = mockChain();
        doReturn(chain).when(reviewRecordService).lambdaQuery();

        LocalDateTime now = LocalDateTime.now();
        ReviewRecord record = ReviewRecord.builder()
                .id(1L).accountId(100L).mistakeId(5L).reviewPlanId(10L)
                .isCorrect(1).reviewStageBefore(2).reviewStageAfter(3)
                .masteryBefore(40).masteryAfter(60)
                .note("复习备注").noteImageUrl("note.jpg")
                .reviewTime(now)
                .build();
        when(chain.list()).thenReturn(List.of(record));

        List<ReviewRecordResp> result = reviewRecordService.listByMistake(100L, 5L);

        assertThat(result).hasSize(1);
        ReviewRecordResp resp = result.get(0);
        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getIsCorrect()).isEqualTo(1);
        assertThat(resp.getReviewStageBefore()).isEqualTo(2);
        assertThat(resp.getReviewStageAfter()).isEqualTo(3);
        assertThat(resp.getMasteryBefore()).isEqualTo(40);
        assertThat(resp.getMasteryAfter()).isEqualTo(60);
        assertThat(resp.getNote()).isEqualTo("复习备注");
        assertThat(resp.getNoteImageUrl()).isEqualTo("note.jpg");
        assertThat(resp.getReviewTime()).isEqualTo(now);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listByMistake_empty_shouldReturnEmptyList() {

        LambdaQueryChainWrapper<ReviewRecord> chain = mockChain();
        doReturn(chain).when(reviewRecordService).lambdaQuery();
        when(chain.list()).thenReturn(new ArrayList<>());

        List<ReviewRecordResp> result = reviewRecordService.listByMistake(100L, 5L);

        assertThat(result).isNotNull().isEmpty();
    }

    // ===== 工具方法 =====

    private LambdaQueryChainWrapper<ReviewRecord> mockChain() {

        return MockChainHelper.mockChain();
    }
}
