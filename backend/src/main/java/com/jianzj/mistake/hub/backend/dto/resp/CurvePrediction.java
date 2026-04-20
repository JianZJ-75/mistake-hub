package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>遗忘曲线未来衰减预测</p>
 *
 * @author jian.zhong
 * @since 2026-04-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurvePrediction {

    /** 预测起点（当前时间） */
    private String fromTime;

    /** 起点保持率 */
    private Double fromRetention;

    /** 当前稳定性 */
    private Double stability;

    /** 预测展示天数 */
    private Integer daysToShow;
}
