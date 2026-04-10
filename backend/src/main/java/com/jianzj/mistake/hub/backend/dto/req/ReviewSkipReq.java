package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>跳过复习请求</p>
 *
 * @author jian.zhong
 * @since 2026-04-10
 */
@Data
public class ReviewSkipReq {

    @NotNull(message = "错题ID不能为空")
    private Long mistakeId;
}
