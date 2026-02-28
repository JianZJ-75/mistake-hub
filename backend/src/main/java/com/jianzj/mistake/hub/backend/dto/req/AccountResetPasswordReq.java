package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>
 * 重置密码 Req
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-28
 */
@Data
public class AccountResetPasswordReq {

    @NotNull(message = "用户 ID 不能为空")
    private Long accountId;
}
