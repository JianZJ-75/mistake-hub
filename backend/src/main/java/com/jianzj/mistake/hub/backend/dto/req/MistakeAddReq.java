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
     * 题目图片URL
     */
    private String titleImageUrl;

    /**
     * 答案图片URL
     */
    private String answerImageUrl;

    /**
     * 标签ID列表
     */
    @Size(max = 20, message = "标签最多选择20个")
    private List<Long> tagIds;

    /**
     * 录入备注（可选，非空时自动创建初始复习记录）
     */
    private String note;

    /**
     * 备注图片URL（可选）
     */
    private String noteImageUrl;
}
