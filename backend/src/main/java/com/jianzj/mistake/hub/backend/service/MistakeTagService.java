package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.entity.MistakeTag;
import com.jianzj.mistake.hub.backend.mapper.MistakeTagMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

/**
 * <p>
 * 错题-标签关联 服务类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Service
@Slf4j
public class MistakeTagService extends ServiceImpl<MistakeTagMapper, MistakeTag> {

    // ===== 业务方法 =====

    /**
     * 批量保存错题标签关联（先删后插）
     */
    public void saveTagIds(Long mistakeId, List<Long> tagIds) {

        removeByMistakeId(mistakeId);

        if (CollectionUtils.isEmpty(tagIds)) {
            return;
        }

        List<MistakeTag> relations = tagIds.stream()
                .distinct()
                .map(tagId -> MistakeTag.builder()
                        .mistakeId(mistakeId)
                        .tagId(tagId)
                        .build())
                .collect(Collectors.toList());

        boolean success = saveBatch(relations);
        if (!success) {
            oops("保存标签关联失败", "Failed to save mistake-tag relations.");
        }
    }

    /**
     * 根据错题ID查询所有标签ID
     */
    public List<Long> getTagIdsByMistakeId(Long mistakeId) {

        List<MistakeTag> relations = lambdaQuery().eq(MistakeTag::getMistakeId, mistakeId).list();
        return relations.stream().map(MistakeTag::getTagId).collect(Collectors.toList());
    }

    /**
     * 根据标签ID查询所有错题ID（用于列表按标签筛选）
     */
    public List<Long> getMistakeIdsByTagId(Long tagId) {

        List<MistakeTag> relations = lambdaQuery().eq(MistakeTag::getTagId, tagId).list();
        return relations.stream().map(MistakeTag::getMistakeId).collect(Collectors.toList());
    }

    // ===== 工具方法 =====

    /**
     * 删除指定错题的所有标签关联
     */
    private void removeByMistakeId(Long mistakeId) {

        lambdaUpdate().eq(MistakeTag::getMistakeId, mistakeId).remove();
    }
}
