package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>未来复习预报（某天到期的错题数量）</p>
 *
 * @author jian.zhong
 * @since 2026-04-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingReview {

    /** 距今天数（0=今天, 1=明天, ...） */
    private Integer daysFromNow;

    /** 预计到期题数 */
    private Integer count;
}
