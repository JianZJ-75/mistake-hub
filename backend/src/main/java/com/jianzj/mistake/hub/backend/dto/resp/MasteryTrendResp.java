package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>复习效果趋势 响应对象</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasteryTrendResp {

    /** 日期 (yyyy-MM-dd) */
    private String date;

    /** 当日所有复习题目的 mastery_after 平均值；无复习记录当日为 null */
    private Double avgMastery;
}
