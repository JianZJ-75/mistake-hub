package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 复习任务 响应对象
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTaskResp {

    /** 错题ID */
    private Long mistakeId;

    /** 题干内容 */
    private String title;

    /** 题目图片URL */
    private String titleImageUrl;

    /** 正确答案 */
    private String correctAnswer;

    /** 答案图片URL */
    private String answerImageUrl;

    /** 复习阶段 0-6 */
    private Integer reviewStage;

    /** 掌握度 0-100 */
    private Integer masteryLevel;

    /** 上次复习时间 */
    private LocalDateTime lastReviewTime;

    /** 逾期天数 */
    private Integer overdueDays;

    /** 优先级分数 */
    private Double priority;

    /** 关联的标签列表 */
    private List<TagResp> tags;

    /** 复习计划状态 */
    private String planStatus;

    /** 复习计划ID */
    private Long reviewPlanId;

    /** 复习前掌握度（仅已完成任务有值，供前端展示"X% → Y%"反馈） */
    private Integer masteryBefore;

    /** 复习前阶段（仅已完成任务有值，供前端展示阶段变化） */
    private Integer reviewStageBefore;

    /** 本次复习产生的下次间隔天数（仅已完成任务有值，供前端展示"下次复习: N天后"） */
    private Integer intervalDays;
}
