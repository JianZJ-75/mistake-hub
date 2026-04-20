package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * <p>记忆健康总览 响应对象</p>
 *
 * @author jian.zhong
 * @since 2026-04-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryHealthResp {

    /** 全部活跃错题的平均保持率 */
    private Double overallRetention;

    /** 活跃错题总数 */
    private Integer totalActive;

    /** 保持率 <30% 的题数 */
    private Integer dangerCount;

    /** 保持率 30%~70% 的题数 */
    private Integer warningCount;

    /** 保持率 >70% 的题数 */
    private Integer safeCount;

    /** 保持率分布（5 个桶） */
    private List<RetentionBucket> distribution;

    /** 未来 7 天复习预报 */
    private List<UpcomingReview> upcoming;
}
