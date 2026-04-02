package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>
 * 新增标签 请求对象
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Data
public class TagAddReq {

    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空")
    private String name;

    /**
     * 类型（可选，如 SUBJECT/CHAPTER/KNOWLEDGE）
     */
    private String type;

    /**
     * 父标签ID，顶级标签传 0
     */
    @NotNull(message = "父标签ID不能为空")
    private Long parentId;
}
