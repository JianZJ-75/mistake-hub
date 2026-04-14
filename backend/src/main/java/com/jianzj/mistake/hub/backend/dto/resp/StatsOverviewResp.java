package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>错题总览统计 响应对象</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsOverviewResp {

    /** 总错题数（status=1） */
    private Integer totalMistakes;

    /** 已掌握（masteryLevel >= 80） */
    private Integer masteredCount;

    /** 待复习（nextReviewTime <= now） */
    private Integer pendingReviewCount;

    /** 本周新增 */
    private Integer newThisWeek;
}
