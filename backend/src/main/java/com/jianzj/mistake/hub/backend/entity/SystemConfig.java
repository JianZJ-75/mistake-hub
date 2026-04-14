package com.jianzj.mistake.hub.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统配置 实体类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Getter
@Setter
@ToString
@TableName("system_config")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置说明 */
    private String description;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
