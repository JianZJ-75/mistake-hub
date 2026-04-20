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

    /** 完全陌生（masteryLevel < 20） */
    private Integer stranger;

    /** 初步了解（20 <= masteryLevel < 60） */
    private Integer beginner;

    /** 基本掌握（60 <= masteryLevel < 80） */
    private Integer basic;

    /** 熟练掌握（80 <= masteryLevel < 100） */
    private Integer proficient;

    /** 彻底掌握（masteryLevel = 100） */
    private Integer mastered;
}
