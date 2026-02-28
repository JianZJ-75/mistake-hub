package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>
 * 修改密码 Req
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-28
 */
@Data
public class AccountChangePasswordReq {

    @NotBlank(message = "旧密码不能为空")
    private String oldCipherDigest;

    @NotNull(message = "旧密码 nonceId 不能为空")
    private Long oldNonceId;

    @NotBlank(message = "新密码不能为空")
    private String newCipherDigest;

    @NotNull(message = "新密码 nonceId 不能为空")
    private Long newNonceId;
}
