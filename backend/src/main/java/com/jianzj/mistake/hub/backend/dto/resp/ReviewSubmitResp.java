package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <p>复习提交响应（before/after 对比）</p>
 *
 * @author jian.zhong
 * @since 2026-04-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmitResp {

    /** 复习前阶段 */
    private Integer reviewStageBefore;

    /** 复习后阶段 */
    private Integer reviewStageAfter;

    /** 复习前掌握度 */
    private Integer masteryBefore;

    /** 复习后掌握度 */
    private Integer masteryAfter;

    /** 下次复习时间 */
    private LocalDateTime nextReviewTime;
}
