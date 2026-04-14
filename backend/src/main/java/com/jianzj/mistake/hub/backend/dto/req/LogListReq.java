package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 操作日志查询 Req
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Data
public class LogListReq {

    /** 操作人ID */
    private Long accountId;

    /** 操作类型 */
    private String action;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 页码 */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为1")
    private Long pageNum;

    /** 每页条数 */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Long pageSize;
}
