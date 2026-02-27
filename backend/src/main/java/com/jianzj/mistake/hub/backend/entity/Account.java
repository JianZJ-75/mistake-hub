package com.jianzj.mistake.hub.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户 实体类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Getter
@Setter
@ToString
@TableName("account")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String code;

    /**
     * 昵称
     */
    private String name;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 加密密码（仅管理员）
     */
    private String cipherPassword;

    /**
     * 角色
     */
    private String role;

    /**
     * 微信 OpenID
     */
    private String wechatOpenId;

    /**
     * 每日最大复习量
     */
    private Long dailyLimit;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
