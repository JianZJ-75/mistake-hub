package com.jianzj.mistake.hub.backend.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 错题详情 响应对象
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MistakeDetailResp {

    private Long id;

    /** 所属用户ID */
    private Long accountId;

    /** 所属用户昵称 */
    private String accountNickname;

    private String title;

    private String correctAnswer;

    private String errorReason;

    private String imageUrl;

    private Integer reviewStage;

    private Integer masteryLevel;

    private LocalDateTime lastReviewTime;

    private LocalDateTime nextReviewTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    /**
     * 关联的标签列表
     */
    private List<TagResp> tags;
}
