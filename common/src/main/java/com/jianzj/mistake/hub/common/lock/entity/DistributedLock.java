package com.jianzj.mistake.hub.common.lock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>分布式锁 实体类</p>
 *
 * @author jian.zhong
 * @since 2026-03-23
 */
@Getter
@Setter
@ToString
@TableName("distributed_lock")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributedLock implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 锁名称（唯一约束）
     */
    private String lockName;

    /**
     * 过期时间
     */
    private LocalDateTime expiredTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 判断锁是否已过期
     */
    public boolean isExpired() {

        if (expiredTime == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(expiredTime);
    }
}
