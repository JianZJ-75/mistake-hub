package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * <p>
 * 用户微信登录 Req
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-27
 */
@Data
public class AccountLoginWxReq {

    @NotBlank(message = "微信登录 code 不能为空")
    private String code;
}
