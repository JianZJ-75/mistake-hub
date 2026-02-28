package com.jianzj.mistake.hub.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 随机数 实体类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Getter
@Setter
@ToString
@TableName("nonce")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Nonce implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 随机数
     */
    private String nonce;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
