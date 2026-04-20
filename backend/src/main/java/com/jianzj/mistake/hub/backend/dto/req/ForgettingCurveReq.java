package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>单题遗忘曲线查询 请求对象</p>
 *
 * @author jian.zhong
 * @since 2026-04-20
 */
@Data
public class ForgettingCurveReq {

    /** 错题ID */
    @NotNull(message = "错题ID不能为空")
    private Long mistakeId;
}
