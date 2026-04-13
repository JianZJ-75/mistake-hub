package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>复习提交请求</p>
 *
 * @author jian.zhong
 * @since 2026-04-10
 */
@Data
public class ReviewSubmitReq {

    @NotNull(message = "错题ID不能为空")
    private Long mistakeId;

    @NotNull(message = "作答结果不能为空")
    private Boolean isCorrect;

    /** 答题备注（可选） */
    private String note;

    /** 答题图片URL（可选） */
    private String noteImageUrl;
}
