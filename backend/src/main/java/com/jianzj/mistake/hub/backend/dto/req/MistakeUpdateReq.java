package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * <p>
 * 错题编辑 请求对象
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Data
public class MistakeUpdateReq {

    /**
     * 错题ID
     */
    @NotNull(message = "错题ID不能为空")
    private Long id;

    /**
     * 题干内容
     */
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
     * 标签ID列表（传 null 表示不修改，传空列表表示清空）
     */
    private List<Long> tagIds;
}
