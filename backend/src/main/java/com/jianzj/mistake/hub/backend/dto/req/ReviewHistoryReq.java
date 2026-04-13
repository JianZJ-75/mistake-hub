package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>复习历史查询请求</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Data
public class ReviewHistoryReq {

    @NotNull(message = "错题ID不能为空")
    private Long mistakeId;
}
