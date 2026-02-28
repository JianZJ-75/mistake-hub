package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>
 * 修改用户角色 Req
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-28
 */
@Data
public class AccountChangeRoleReq {

    @NotNull(message = "用户 ID 不能为空")
    private Long accountId;

    @NotBlank(message = "目标角色不能为空")
    private String role;

    /** 升级为管理员时必填，降级时不需要 */
    private Long nonceId;
}
