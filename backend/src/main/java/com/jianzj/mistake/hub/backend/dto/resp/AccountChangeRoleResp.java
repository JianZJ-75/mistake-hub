package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 修改用户角色 Resp
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountChangeRoleResp {

    /** AES(原始密码, nonce) 加密后的密码，仅升级为管理员时返回 */
    private String cipherPassword;
}
