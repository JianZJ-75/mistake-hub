package com.jianzj.mistake.hub.backend.enums;

import com.jianzj.mistake.hub.common.convention.enums.BasicEnumPojo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 用户角色 枚举类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Getter
@AllArgsConstructor
public enum Role {

    ADMIN("admin", "管理员", "Admin", 2),

    STUDENT("student", "学生", "Student", 1)

    ;
    
    private final String code;
    
    private final String displayNameCn;
    
    private final String displayNameUs;
    
    private final Integer level;

    /**
     * 根据 code 获取枚举类
     * String -> Role
     */
    public static Role fromCode(String str) {

        return Arrays.stream(values())
                .filter(v -> v.code.equals(str))
                .findFirst()
                .orElse(null);
    }

    /**
     * 是否是合法的用户角色
     */
    public static boolean isValid(String code) {

        if (StringUtils.isBlank(code)) {
            return false;
        }

        return Arrays.stream(Role.values())
                .anyMatch(v -> v.code.equals(code));
    }

    /**
     * 是否是非法的用户角色
     */
    public static boolean isInvalid(String code) {

        return !isValid(code);
    }

    /**
     * 返回所有
     */
    public static List<BasicEnumPojo> listAll() {

        return Arrays.stream(values())
                .map(type ->
                        BasicEnumPojo.builder()
                                .code(type.getCode())
                                .displayNameCn(type.getDisplayNameCn())
                                .displayNameUs(type.getDisplayNameUs())
                                .level(type.getLevel())
                                .build()
                )
                .toList();
    }
}
