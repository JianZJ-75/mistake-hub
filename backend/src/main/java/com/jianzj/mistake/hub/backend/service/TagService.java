package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.dto.req.TagAddReq;
import com.jianzj.mistake.hub.backend.dto.resp.TagResp;
import com.jianzj.mistake.hub.backend.entity.Tag;
import com.jianzj.mistake.hub.backend.enums.TagType;
import com.jianzj.mistake.hub.backend.mapper.TagMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

/**
 * <p>
 * 标签 服务类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Service
@Slf4j
public class TagService extends ServiceImpl<TagMapper, Tag> {

    // ===== 业务方法 =====

    /**
     * 查询标签树（全量加载后在内存中递归构建）
     */
    public List<TagResp> tree() {

        List<Tag> all = list();
        return buildTree(all, 0L);
    }

    /**
     * 新增标签
     */
    public void add(TagAddReq req) {

        if (TagType.isInvalid(req.getType())) {
            oops("无效的标签类型：%s", "Invalid tag type: %s", req.getType());
        }

        if (req.getParentId() != 0L) {
            Tag parent = getById(req.getParentId());
            if (parent == null) {
                oops("父标签不存在", "Parent tag does not exist.");
            }
        }

        Tag tag = Tag.builder()
                .name(req.getName())
                .type(req.getType())
                .parentId(req.getParentId())
                .build();

        boolean success = save(tag);
        if (!success) {
            oops("新增标签失败", "Failed to add tag.");
        }
    }

    // ===== 工具方法 =====

    /**
     * 根据ID列表查询标签列表（不含children，用于详情回显）
     */
    public List<TagResp> getByIds(List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        return listByIds(ids).stream()
                .map(this::toResp)
                .collect(Collectors.toList());
    }

    /**
     * 从全量标签中递归构建指定 parentId 下的子树
     */
    private List<TagResp> buildTree(List<Tag> all, Long parentId) {

        Map<Long, List<Tag>> groupByParent = all.stream()
                .collect(Collectors.groupingBy(Tag::getParentId));

        return buildChildren(groupByParent, parentId);
    }

    /**
     * 递归构建子节点
     */
    private List<TagResp> buildChildren(Map<Long, List<Tag>> groupByParent, Long parentId) {

        List<Tag> children = groupByParent.getOrDefault(parentId, new ArrayList<>());
        return children.stream()
                .map(tag -> {
                    TagResp resp = toResp(tag);
                    resp.setChildren(buildChildren(groupByParent, tag.getId()));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    /**
     * Tag -> TagResp
     */
    private TagResp toResp(Tag tag) {

        TagResp resp = new TagResp();
        BeanUtils.copyProperties(tag, resp);
        return resp;
    }
}
