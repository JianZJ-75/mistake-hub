package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>
 * 手动生成每日复习任务 请求对象
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-02
 */
@Data
public class ReviewGenerateDailyReq {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long accountId;
}
