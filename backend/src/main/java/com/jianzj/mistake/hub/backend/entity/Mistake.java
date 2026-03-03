package com.jianzj.mistake.hub.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 错题 实体类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Getter
@Setter
@ToString
@TableName("mistake")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mistake implements Serializable {

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
     * 题干内容
     */
    private String title;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 错误原因说明
     */
    private String errorReason;

    /**
     * 错题图片URL
     */
    private String imageUrl;

    /**
     * 学科
     */
    private String subject;

    /**
     * 复习阶段 0-6
     */
    private Integer reviewStage;

    /**
     * 掌握度 0-100
     */
    private Integer masteryLevel;

    /**
     * 最近复习时间
     */
    private LocalDateTime lastReviewTime;

    /**
     * 下次复习时间
     */
    private LocalDateTime nextReviewTime;

    /**
     * 状态：1-有效 0-删除
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
