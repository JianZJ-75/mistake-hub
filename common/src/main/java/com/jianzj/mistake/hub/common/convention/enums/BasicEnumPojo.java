package com.jianzj.mistake.hub.common.convention.enums;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 基础枚举类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BasicEnumPojo {

    /**
     * 必填项
     */
    private String code;

    private String displayNameCn;

    private String displayNameUs;

    /**
     * 可选项
     */
    private Integer level;
}
