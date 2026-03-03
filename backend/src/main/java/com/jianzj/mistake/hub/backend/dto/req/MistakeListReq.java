package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>
 * 错题列表查询 请求对象
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Data
public class MistakeListReq {

    /**
     * 学科筛选（可空）
     */
    private String subject;

    /**
     * 标签ID筛选（可空）
     */
    private Long tagId;

    /**
     * 掌握度筛选：0-未掌握(<60) 1-掌握中(60-79) 2-已掌握(>=80)，可空
     */
    private Integer masteryFilter;

    /**
     * 页码
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为1")
    private Long pageNum;

    /**
     * 每页条数
     */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数最小为1")
    private Long pageSize;
}
