package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>掌握度分布 响应对象</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasteryDistributionResp {

    /** 未掌握（masteryLevel < 60） */
    private Integer notMastered;

    /** 掌握中（60 <= masteryLevel < 80） */
    private Integer learning;

    /** 已掌握（masteryLevel >= 80） */
    private Integer mastered;
}
