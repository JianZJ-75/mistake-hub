package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>
 * 修改每日复习量 Req
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-28
 */
@Data
public class AccountUpdateDailyLimitReq {

    @NotNull(message = "每日复习量不能为空")
    @Min(value = 1, message = "每日复习量最小为 1")
    private Long dailyLimit;
}
