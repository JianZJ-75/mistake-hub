package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>复习进度响应</p>
 *
 * @author jian.zhong
 * @since 2026-04-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewProgressResp {

    /** 今日总任务数 */
    private Integer totalToday;

    /** 今日已完成数 */
    private Integer completedToday;

    /** 今日已跳过数 */
    private Integer skippedToday;

    /** 连续复习天数 */
    private Integer streakDays;
}
