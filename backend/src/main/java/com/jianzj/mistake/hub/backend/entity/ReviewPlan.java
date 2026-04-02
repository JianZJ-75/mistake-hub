package com.jianzj.mistake.hub.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 复习计划 实体类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-02
 */
@Getter
@Setter
@ToString
@TableName("review_plan")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户
     */
    private Long accountId;

    /**
     * 关联错题
     */
    private Long mistakeId;

    /**
     * 计划复习日期
     */
    private LocalDate plannedDate;

    /**
     * 状态：PENDING/COMPLETED/SKIPPED
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
