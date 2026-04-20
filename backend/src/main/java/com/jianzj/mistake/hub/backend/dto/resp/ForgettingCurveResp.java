package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * <p>单题遗忘曲线 响应对象</p>
 *
 * @author jian.zhong
 * @since 2026-04-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgettingCurveResp {

    /** 错题ID */
    private Long mistakeId;

    /** 错题创建时间 */
    private String createdTime;

    /** 当前记忆保持率 0~1 */
    private Double currentRetention;

    /** 当前复习阶段 */
    private Integer currentStage;

    /** 下次复习时间 */
    private String nextReviewTime;

    /** 每段衰减曲线（两次复习之间） */
    private List<CurveSegment> segments;

    /** 从当前时间往后的预测衰减 */
    private CurvePrediction prediction;
}
