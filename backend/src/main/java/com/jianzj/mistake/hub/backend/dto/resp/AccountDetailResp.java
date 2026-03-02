package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户详情 Resp
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDetailResp {

    /** 用户 ID */
    private Long id;

    /** 用户 code */
    private String code;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatarUrl;

    /** 角色 */
    private String role;

    /** 每日最大复习量 */
    private Long dailyLimit;

    /** 注册时间 */
    private LocalDateTime createdTime;
}
