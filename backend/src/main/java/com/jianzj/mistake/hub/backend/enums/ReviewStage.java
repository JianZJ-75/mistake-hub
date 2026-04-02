package com.jianzj.mistake.hub.backend.enums;

import com.jianzj.mistake.hub.common.convention.enums.BasicEnumPojo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 复习阶段 枚举类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-02
 */
@Getter
@AllArgsConstructor
public enum ReviewStage {

    STAGE_0(0, "STAGE_0", "新录入（当天）", "New (same day)", 0, 0),

    STAGE_1(1, "STAGE_1", "第1天复习", "Day 1 review", 1, 1),

    STAGE_2(2, "STAGE_2", "第3天复习", "Day 3 review", 2, 2),

    STAGE_3(3, "STAGE_3", "第7天复习", "Day 7 review", 3, 4),

    STAGE_4(4, "STAGE_4", "第14天复习", "Day 14 review", 4, 7),

    STAGE_5(5, "STAGE_5", "第29天复习", "Day 29 review", 5, 15),

    STAGE_6(6, "STAGE_6", "第59天复习", "Day 59 review", 6, 30)

    ;

    /** 阶段序号 0-6 */
    private final Integer stageIndex;

    private final String code;

    private final String displayNameCn;

    private final String displayNameUs;

    private final Integer level;

    /** 复习间隔天数 */
    private final Integer intervalDays;

    /**
     * 根据 stageIndex 获取枚举
     */
    public static ReviewStage fromStageIndex(int stageIndex) {

        return Arrays.stream(values())
                .filter(v -> v.stageIndex == stageIndex)
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据 code 获取枚举
     */
    public static ReviewStage fromCode(String str) {

        return Arrays.stream(values())
                .filter(v -> v.code.equals(str))
                .findFirst()
                .orElse(null);
    }

    /**
     * 是否是合法的复习阶段
     */
    public static boolean isValid(int stageIndex) {

        return Arrays.stream(values())
                .anyMatch(v -> v.stageIndex == stageIndex);
    }

    /**
     * 是否是非法的复习阶段
     */
    public static boolean isInvalid(int stageIndex) {

        return !isValid(stageIndex);
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

    /**
     * 答对后的下一阶段
     */
    public static int nextStageOnCorrect(int currentStage) {

        return Math.min(currentStage + 1, 6);
    }

    /**
     * 答错后的下一阶段
     */
    public static int nextStageOnWrong(int currentStage) {

        return Math.max(currentStage - 2, 0);
    }

    /**
     * 根据阶段序号获取间隔天数
     */
    public static int getIntervalByStage(int stageIndex) {

        ReviewStage stage = fromStageIndex(stageIndex);
        return stage != null ? stage.intervalDays : 0;
    }
}
