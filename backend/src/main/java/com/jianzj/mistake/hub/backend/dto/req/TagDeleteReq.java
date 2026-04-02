package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>
 * 删除标签 请求对象
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-20
 */
@Data
public class TagDeleteReq {

    /**
     * 标签ID
     */
    @NotNull(message = "标签ID不能为空")
    private Long id;
}
