package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.entity.MistakeTag;
import com.jianzj.mistake.hub.backend.entity.Tag;
import com.jianzj.mistake.hub.backend.mapper.MistakeTagMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final TagService tagService;

    public MistakeTagService(TagService tagService) {

        this.tagService = tagService;
    }

    // ===== 业务方法 =====

    /**
     * 批量保存错题标签关联（先删后插）
     */
    public void saveTagIds(Long mistakeId, List<Long> tagIds) {

        removeByMistakeId(mistakeId);

        if (CollectionUtils.isEmpty(tagIds)) {
            return;
        }

        // 校验 tag ID 全部存在
        List<Long> distinctIds = tagIds.stream().distinct().collect(Collectors.toList());
        List<Tag> existingTags = tagService.listByIds(distinctIds);
        if (existingTags.size() != distinctIds.size()) {
            Set<Long> existingIds = existingTags.stream().map(Tag::getId).collect(Collectors.toSet());
            List<Long> missingIds = distinctIds.stream().filter(id -> !existingIds.contains(id)).collect(Collectors.toList());
            oops("标签不存在：%s", "Tags not found: %s", missingIds);
        }

        List<MistakeTag> relations = distinctIds.stream()
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

    /**
     * 根据多个标签ID查询所有错题ID（层级展开后使用）
     */
    public List<Long> getMistakeIdsByTagIds(java.util.Collection<Long> tagIds) {

        if (CollectionUtils.isEmpty(tagIds)) {
            return new ArrayList<>();
        }

        List<MistakeTag> relations = lambdaQuery().in(MistakeTag::getTagId, tagIds).list();
        return relations.stream().map(MistakeTag::getMistakeId).distinct().collect(Collectors.toList());
    }

    /**
     * 批量查询多个错题的标签ID映射（mistakeId -> tagIds）
     */
    public Map<Long, List<Long>> getTagIdMapByMistakeIds(List<Long> mistakeIds) {

        if (CollectionUtils.isEmpty(mistakeIds)) {
            return Map.of();
        }

        List<MistakeTag> relations = lambdaQuery().in(MistakeTag::getMistakeId, mistakeIds).list();
        return relations.stream()
                .collect(Collectors.groupingBy(
                        MistakeTag::getMistakeId,
                        Collectors.mapping(MistakeTag::getTagId, Collectors.toList())
                ));
    }

    /**
     * 查询错题关联的 SUBJECT 标签名映射（mistakeId -> subjectName）
     */
    public Map<Long, String> getSubjectNamesByMistakeIds(List<Long> mistakeIds) {

        if (CollectionUtils.isEmpty(mistakeIds)) {
            return Map.of();
        }

        Map<Long, List<Long>> tagIdMap = getTagIdMapByMistakeIds(mistakeIds);
        Set<Long> allTagIds = tagIdMap.values().stream().flatMap(List::stream).collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(allTagIds)) {
            return Map.of();
        }

        // 查出所有关联标签，过滤出 SUBJECT 类型
        List<Tag> allTags = tagService.listByIds(new ArrayList<>(allTagIds));
        Map<Long, String> subjectTagNameMap = allTags.stream()
                .filter(tag -> "SUBJECT".equals(tag.getType()))
                .collect(Collectors.toMap(Tag::getId, Tag::getName));

        // 为每个 mistakeId 找到第一个 SUBJECT 标签名
        return tagIdMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(subjectTagNameMap::get)
                                .filter(name -> name != null)
                                .findFirst()
                                .orElse("未分类"),
                        (a, b) -> a
                ));
    }

    // ===== 工具方法 =====

    /**
     * 删除指定错题的所有标签关联（无关联时不做任何操作）
     */
    public void removeByMistakeId(Long mistakeId) {

        boolean success = lambdaUpdate().eq(MistakeTag::getMistakeId, mistakeId).remove();
        if (!success) {
            log.warn("删除错题标签关联无影响行（mistakeId={}），可能无关联数据", mistakeId);
        }
    }
}
