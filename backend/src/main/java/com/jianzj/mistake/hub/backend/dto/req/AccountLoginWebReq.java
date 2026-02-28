package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>
 * Web 管理端登录 Req
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-28
 */
@Data
public class AccountLoginWebReq {

    @NotBlank(message = "用户名不能为空")
    private String code;

    @NotBlank(message = "加密密码不能为空")
    private String cipherDigest;

    @NotNull(message = "nonceId 不能为空")
    private Long nonceId;
}
