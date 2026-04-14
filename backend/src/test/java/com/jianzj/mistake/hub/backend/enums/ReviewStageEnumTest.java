package com.jianzj.mistake.hub.backend.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>ReviewStage 枚举单元测试</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
class ReviewStageEnumTest {

    // ===== nextStageOnCorrect =====

    @ParameterizedTest
    @CsvSource({"0,1", "1,2", "2,3", "3,4", "4,5", "5,6", "6,6"})
    void nextStageOnCorrect_allStages(int current, int expected) {

        assertThat(ReviewStage.nextStageOnCorrect(current)).isEqualTo(expected);
    }

    // ===== nextStageOnWrong =====

    @ParameterizedTest
    @CsvSource({"0,0", "1,0", "2,0", "3,1", "4,2", "5,3", "6,4"})
    void nextStageOnWrong_boundaries(int current, int expected) {

        assertThat(ReviewStage.nextStageOnWrong(current)).isEqualTo(expected);
    }

    // ===== getIntervalByStage =====

    @ParameterizedTest
    @CsvSource({"0,0", "1,1", "2,2", "3,4", "4,7", "5,15", "6,30"})
    void getIntervalByStage_allStages(int stageIndex, int expectedDays) {

        assertThat(ReviewStage.getIntervalByStage(stageIndex)).isEqualTo(expectedDays);
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "7,0", "100,0"})
    void getIntervalByStage_invalidIndex_shouldReturn0(int stageIndex, int expected) {

        assertThat(ReviewStage.getIntervalByStage(stageIndex)).isEqualTo(expected);
    }

    // ===== fromStageIndex =====

    @Test
    void fromStageIndex_valid_shouldReturnEnum() {

        for (int i = 0; i <= 6; i++) {
            ReviewStage stage = ReviewStage.fromStageIndex(i);
            assertThat(stage).isNotNull();
            assertThat(stage.getStageIndex()).isEqualTo(i);
        }
    }

    @ParameterizedTest
    @CsvSource({"-1", "7", "100"})
    void fromStageIndex_invalid_shouldReturnNull(int stageIndex) {

        assertThat(ReviewStage.fromStageIndex(stageIndex)).isNull();
    }

    // ===== isValid / isInvalid =====

    @Test
    void isValid_validRange() {

        for (int i = 0; i <= 6; i++) {
            assertThat(ReviewStage.isValid(i)).isTrue();
            assertThat(ReviewStage.isInvalid(i)).isFalse();
        }
    }

    @Test
    void isInvalid_outOfRange() {

        assertThat(ReviewStage.isValid(-1)).isFalse();
        assertThat(ReviewStage.isValid(7)).isFalse();
        assertThat(ReviewStage.isInvalid(-1)).isTrue();
        assertThat(ReviewStage.isInvalid(7)).isTrue();
    }

    // ===== listAll =====

    @Test
    void listAll_shouldReturn7() {

        assertThat(ReviewStage.listAll()).hasSize(7);
    }
}
