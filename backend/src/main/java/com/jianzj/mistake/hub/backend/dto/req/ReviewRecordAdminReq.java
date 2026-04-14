package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>复习记录管理员查询 请求对象</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Data
public class ReviewRecordAdminReq {

    /** 按用户筛选（可选） */
    private Long accountId;

    /** 按错题筛选（可选） */
    private Long mistakeId;

    /** 答题结果筛选：0-答错 1-答对（可选） */
    private Integer isCorrect;

    /** 开始时间（可选） */
    private LocalDateTime startTime;

    /** 结束时间（可选） */
    private LocalDateTime endTime;

    /** 页码 */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为1")
    private Long pageNum;

    /** 每页大小 */
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Long pageSize;
}
