package com.jianzj.mistake.hub.backend.dto.req;

import lombok.Data;

/**
 * <p>
 * 修改个人资料 Req
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Data
public class AccountModifyProfileReq {

    /** 昵称（非空时更新） */
    private String nickname;

    /** 头像URL（null=不改，""=清除，非空=更新） */
    private String avatarUrl;
}
