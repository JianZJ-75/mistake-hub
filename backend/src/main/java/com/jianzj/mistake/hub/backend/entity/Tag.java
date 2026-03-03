package com.jianzj.mistake.hub.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 标签 实体类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Getter
@Setter
@ToString
@TableName("tag")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 类型: SUBJECT/CHAPTER/KNOWLEDGE
     */
    private String type;

    /**
     * 父标签ID，0为顶级
     */
    private Long parentId;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
