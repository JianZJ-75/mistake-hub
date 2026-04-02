package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * <p>
 * 错题录入 请求对象
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Data
public class MistakeAddReq {

    /**
     * 题干内容
     */
    @NotBlank(message = "题干不能为空")
    private String title;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 错误原因说明
     */
    private String errorReason;

    /**
     * 错题图片URL
     */
    private String imageUrl;

    /**
     * 标签ID列表
     */
    @Size(max = 20, message = "标签最多选择20个")
    private List<Long> tagIds;
}
