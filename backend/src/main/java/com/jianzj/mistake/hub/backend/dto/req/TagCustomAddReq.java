package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * <p>
 * 新增自定义标签 请求对象
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-01
 */
@Data
public class TagCustomAddReq {

    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空")
    private String name;
}
