package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <p>
 * 复习记录 响应对象
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRecordResp {

    private Long id;

    /** 是否答对：1-答对 0-答错 */
    private Integer isCorrect;

    /** 复习前阶段 */
    private Integer reviewStageBefore;

    /** 复习后阶段 */
    private Integer reviewStageAfter;

    /** 复习前掌握度 */
    private Integer masteryBefore;

    /** 复习后掌握度 */
    private Integer masteryAfter;

    /** 答题备注 */
    private String note;

    /** 答题图片URL */
    private String noteImageUrl;

    /** 复习时间 */
    private LocalDateTime reviewTime;
}
