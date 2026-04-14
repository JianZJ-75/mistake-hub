package com.jianzj.mistake.hub.backend.enums;

import com.jianzj.mistake.hub.common.convention.enums.BasicEnumPojo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 操作日志 — 操作类型 枚举类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Getter
@AllArgsConstructor
public enum OperationAction {

    CHANGE_ROLE("account/change-role", "角色变更", "Change Role", 1),

    RESET_PASSWORD("account/reset-password", "重置密码", "Reset Password", 2),

    TAG_ADD("tag/add", "新增标签", "Add Tag", 3),

    TAG_UPDATE("tag/update", "更新标签", "Update Tag", 4),

    TAG_DELETE("tag/delete", "删除标签", "Delete Tag", 5),

    CONFIG_UPDATE("config/update", "更新配置", "Update Config", 6),

    MISTAKE_ADMIN_UPDATE("mistake/admin-update", "管理员编辑错题", "Admin Update Mistake", 7)

    ;

    private final String code;

    private final String displayNameCn;

    private final String displayNameUs;

    private final Integer level;

    /**
     * 根据 code 获取枚举
     */
    public static OperationAction fromCode(String str) {

        return Arrays.stream(values())
                .filter(v -> v.code.equals(str))
                .findFirst()
                .orElse(null);
    }

    /**
     * 是否是合法的操作类型
     */
    public static boolean isValid(String code) {

        if (StringUtils.isBlank(code)) {
            return false;
        }

        return Arrays.stream(values())
                .anyMatch(v -> v.code.equals(code));
    }

    /**
     * 是否是非法的操作类型
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
