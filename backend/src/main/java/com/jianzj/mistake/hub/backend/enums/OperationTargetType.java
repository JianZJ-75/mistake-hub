package com.jianzj.mistake.hub.backend.enums;

import com.jianzj.mistake.hub.common.convention.enums.BasicEnumPojo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 操作日志 — 目标类型 枚举类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Getter
@AllArgsConstructor
public enum OperationTargetType {

    ACCOUNT("ACCOUNT", "用户", "Account", 1),

    MISTAKE("MISTAKE", "错题", "Mistake", 2),

    TAG("TAG", "标签", "Tag", 3),

    CONFIG("CONFIG", "配置", "Config", 4)

    ;

    private final String code;

    private final String displayNameCn;

    private final String displayNameUs;

    private final Integer level;

    /**
     * 根据 code 获取枚举
     */
    public static OperationTargetType fromCode(String str) {

        return Arrays.stream(values())
                .filter(v -> v.code.equals(str))
                .findFirst()
                .orElse(null);
    }

    /**
     * 是否是合法的目标类型
     */
    public static boolean isValid(String code) {

        if (StringUtils.isBlank(code)) {
            return false;
        }

        return Arrays.stream(values())
                .anyMatch(v -> v.code.equals(code));
    }

    /**
     * 是否是非法的目标类型
     */
    public static boolean isInvalid(String code) {

        return !isValid(code);
    }

    /**
     * 返回所有
     */
    public static List<BasicEnumPojo> listAll() {

        return Arrays.stream(values())
                .map(e ->
                        BasicEnumPojo.builder()
                                .code(e.getCode())
                                .displayNameCn(e.getDisplayNameCn())
                                .displayNameUs(e.getDisplayNameUs())
                                .level(e.getLevel())
                                .build()
                )
                .toList();
    }
}
