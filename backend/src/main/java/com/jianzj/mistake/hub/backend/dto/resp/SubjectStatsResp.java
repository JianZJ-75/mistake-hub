package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>学科分布统计 响应对象</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectStatsResp {

    /** 学科名称 */
    private String subject;

    /** 错题数量 */
    private Integer count;

    /** 平均掌握度 */
    private Double avgMastery;
}
