package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>管理端全局统计 响应对象</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewResp {

    /** 总用户数 */
    private Integer totalUsers;

    /** 今日活跃用户数 */
    private Integer activeUsersToday;

    /** 全平台错题总数 */
    private Integer totalMistakes;

    /** 全平台近7天平均复习完成率 */
    private Double avgCompletionRate;
}
