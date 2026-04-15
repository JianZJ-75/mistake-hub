package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 标签（含子标签）响应对象，用于树形结构
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagResp {

    private Long id;

    private String name;

    private String type;

    private Long parentId;

    private Long accountId;

    /** 所属用户昵称（仅自定义标签，管理员查询时填充） */
    private String accountNickname;

    private LocalDateTime createdTime;

    /**
     * 子标签列表（树形结构时使用）
     */
    private List<TagResp> children;
}
