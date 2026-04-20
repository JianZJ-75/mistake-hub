package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>记忆保持率分布桶</p>
 *
 * @author jian.zhong
 * @since 2026-04-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetentionBucket {

    /** 区间标签（如 "0-20%"） */
    private String range;

    /** 该区间的错题数量 */
    private Integer count;
}
