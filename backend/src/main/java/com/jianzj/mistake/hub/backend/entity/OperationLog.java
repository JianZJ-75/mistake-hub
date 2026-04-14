package com.jianzj.mistake.hub.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 操作日志 实体类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Getter
@Setter
@ToString
@TableName("operation_log")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID */
    private Long accountId;

    /** 操作人 code */
    private String accountCode;

    /** 操作类型 */
    private String action;

    /** 目标类型(ACCOUNT/MISTAKE/TAG/CONFIG) */
    private String targetType;

    /** 目标ID */
    private String targetId;

    /** 操作详情(JSON) */
    private String detail;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
