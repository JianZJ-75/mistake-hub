package com.jianzj.mistake.hub.backend.enums;

import com.jianzj.mistake.hub.common.convention.enums.BasicEnumPojo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 错题状态 枚举类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-15
 */
@Getter
@AllArgsConstructor
public enum MistakeStatus {

    DELETED(0, "已删除", "Deleted", 0),

    VALID(1, "有效", "Valid", 1),

    PENDING_DELETE(2, "待删除", "Pending Delete", 2)

    ;

    private final Integer code;

    private final String displayNameCn;

    private final String displayNameUs;

    private final Integer level;

    /**
     * 根据 code 获取枚举
     */
    public static MistakeStatus fromCode(Integer code) {

        if (code == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(v -> v.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 是否是合法的状态
     */
    public static boolean isValid(Integer code) {

        if (code == null) {
            return false;
        }

        return Arrays.stream(values())
                .anyMatch(v -> v.code.equals(code));
    }

    /**
     * 是否是非法的状态
     */
    public static boolean isInvalid(Integer code) {

        return !isValid(code);
    }

    /**
     * 返回所有
     */
    public static List<BasicEnumPojo> listAll() {

        return Arrays.stream(values())
                .map(type ->
                        BasicEnumPojo.builder()
                                .code(String.valueOf(type.getCode()))
                                .displayNameCn(type.getDisplayNameCn())
                                .displayNameUs(type.getDisplayNameUs())
                                .level(type.getLevel())
                                .build()
                )
                .toList();
    }
}
