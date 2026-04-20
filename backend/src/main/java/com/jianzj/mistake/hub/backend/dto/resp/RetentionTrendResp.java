package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>记忆保持率趋势（按日）</p>
 *
 * @author jian.zhong
 * @since 2026-04-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetentionTrendResp {

    /** 日期（yyyy-MM-dd） */
    private String date;

    /** 当日平均记忆保持率（0~1），无数据时为 null */
    private Double avgRetention;
}
