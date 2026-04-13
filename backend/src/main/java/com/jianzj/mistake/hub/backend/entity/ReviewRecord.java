package com.jianzj.mistake.hub.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 复习记录 实体类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-09
 */
@Getter
@Setter
@ToString
@TableName("review_record")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRecord implements Serializable {

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
     * 关联的计划ID，主动复习时可为null
     */
    private Long reviewPlanId;

    /**
     * 是否答对：1-答对 0-答错
     */
    private Integer isCorrect;

    /**
     * 复习前阶段
     */
    private Integer reviewStageBefore;

    /**
     * 复习后阶段
     */
    private Integer reviewStageAfter;

    /**
     * 复习前掌握度
     */
    private Integer masteryBefore;

    /**
     * 复习后掌握度
     */
    private Integer masteryAfter;

    /**
     * 答题备注
     */
    private String note;

    /**
     * 答题图片URL
     */
    private String noteImageUrl;

    /**
     * 复习时间
     */
    private LocalDateTime reviewTime;
}
