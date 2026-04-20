package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>遗忘曲线单段衰减数据（两次复习之间）</p>
 *
 * @author jian.zhong
 * @since 2026-04-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurveSegment {

    /** 该段起点时间（复习发生时刻） */
    private String startTime;

    /** 该段的记忆稳定性 S */
    private Double stability;

    /** 复习后的阶段 */
    private Integer stageAfter;

    /** 该次复习是否答对 */
    private Boolean isCorrect;

    /** 该段终点时间（下次复习时刻或当前时间） */
    private String endTime;
}
