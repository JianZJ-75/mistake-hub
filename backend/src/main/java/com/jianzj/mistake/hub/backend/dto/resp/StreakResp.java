package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>连续复习天数 响应对象</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakResp {

    /** 当前连续天数 */
    private Integer currentStreak;

    /** 历史最长连续天数 */
    private Integer longestStreak;
}
