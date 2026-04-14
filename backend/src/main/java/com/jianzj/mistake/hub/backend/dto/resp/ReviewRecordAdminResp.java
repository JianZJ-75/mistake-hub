package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <p>复习记录管理员查询 响应对象</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRecordAdminResp {

    private Long id;

    /** 所属用户 code */
    private String accountCode;

    /** 所属用户昵称 */
    private String accountNickname;

    /** 关联错题ID */
    private Long mistakeId;

    /** 关联错题标题（截断80字） */
    private String mistakeTitle;

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
