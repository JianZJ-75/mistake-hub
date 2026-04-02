package com.jianzj.mistake.hub.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;

/**
 * <p>
 * 错题-标签关联 实体类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Getter
@Setter
@ToString
@TableName("mistake_tag")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MistakeTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 错题ID
     */
    private Long mistakeId;

    /**
     * 标签ID
     */
    private Long tagId;
}
