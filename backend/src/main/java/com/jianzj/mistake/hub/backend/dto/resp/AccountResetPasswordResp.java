package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 重置密码 Resp
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResetPasswordResp {

    /** nonce ID，前端用于解密 cipherPassword */
    private Long nonceId;

    /** AES(新密码, nonce) 加密后的密码 */
    private String cipherPassword;
}
