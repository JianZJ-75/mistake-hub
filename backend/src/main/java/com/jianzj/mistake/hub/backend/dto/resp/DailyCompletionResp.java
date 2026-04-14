package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>每日复习完成率 响应对象</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyCompletionResp {

    /** 日期 (yyyy-MM-dd) */
    private String date;

    /** 当日计划总数 */
    private Integer totalPlanned;

    /** 当日已完成 */
    private Integer completed;

    /** 完成率 */
    private Double completionRate;
}
