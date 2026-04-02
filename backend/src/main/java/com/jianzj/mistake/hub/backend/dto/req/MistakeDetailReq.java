package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>
 * 错题详情查询 请求对象
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Data
public class MistakeDetailReq {

    /**
     * 错题ID
     */
    @NotNull(message = "错题ID不能为空")
    private Long id;
}
