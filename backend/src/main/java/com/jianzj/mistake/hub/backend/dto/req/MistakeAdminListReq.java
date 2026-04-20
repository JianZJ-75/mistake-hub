package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>
 * 管理员错题列表查询 Req
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Data
public class MistakeAdminListReq {

    /** 按用户筛选 */
    private Long accountId;

    /** 按标签筛选 */
    private Long tagId;

    /** 掌握度筛选：0=完全陌生(<20), 1=初步了解(20~60), 2=基本掌握(60~80), 3=熟练掌握(80~99), 4=彻底掌握(=100) */
    @Min(value = 0, message = "掌握度筛选值最小为0")
    @Max(value = 4, message = "掌握度筛选值最大为4")
    private Integer masteryFilter;

    /** 状态筛选：1=有效, 2=待删除，不传默认查有效 */
    @Min(value = 1, message = "状态筛选值最小为1")
    @Max(value = 2, message = "状态筛选值最大为2")
    private Integer statusFilter;

    /** 页码 */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为1")
    private Long pageNum;

    /** 每页条数 */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Long pageSize;
}
