package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.dto.req.TagAddReq;
import com.jianzj.mistake.hub.backend.dto.req.TagCustomAddReq;
import com.jianzj.mistake.hub.backend.dto.req.TagDeleteReq;
import com.jianzj.mistake.hub.backend.dto.req.TagUpdateReq;
import com.jianzj.mistake.hub.backend.dto.resp.TagResp;
import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.entity.MistakeTag;
import com.jianzj.mistake.hub.backend.entity.Tag;
import com.jianzj.mistake.hub.backend.enums.TagType;
import com.jianzj.mistake.hub.backend.mapper.MistakeTagMapper;
import com.jianzj.mistake.hub.backend.mapper.TagMapper;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final MistakeTagMapper mistakeTagMapper;

    private final AccountService accountService;

    private final ThreadStorageUtil threadStorageUtil;

    public TagService(MistakeTagMapper mistakeTagMapper,
                      AccountService accountService,
                      ThreadStorageUtil threadStorageUtil) {

        this.mistakeTagMapper = mistakeTagMapper;
        this.accountService = accountService;
        this.threadStorageUtil = threadStorageUtil;
    }

    // ===== 业务方法 =====

    /**
     * 查询全局标签树（account_id IS NULL）
     */
    public List<TagResp> tree() {

        List<Tag> all = lambdaQuery().isNull(Tag::getAccountId).list();
        return buildTree(all, 0L);
    }

    /**
     * 新增全局标签（管理员）
     */
    public void add(TagAddReq req) {

        if (!req.getParentId().equals(0L)) {
            Tag parent = getById(req.getParentId());
            if (parent == null) {
                oops("父标签不存在", "Parent tag does not exist.");
            }
        }

        // 同名 + 同父级去重
        List<Tag> duplicates = lambdaQuery()
                .eq(Tag::getName, req.getName())
                .eq(Tag::getParentId, req.getParentId())
                .isNull(Tag::getAccountId)
                .list();
        if (CollectionUtils.isNotEmpty(duplicates)) {
            oops("同级下已存在同名标签：%s", "Duplicate tag name under the same parent: %s", req.getName());
        }

        Tag tag = Tag.builder()
                .name(req.getName())
                .type(req.getType())
                .parentId(req.getParentId())
                .accountId(null)
                .build();

        boolean success = save(tag);
        if (!success) {
            oops("新增标签失败", "Failed to add tag.");
        }
    }

    /**
     * 编辑全局标签（管理员）
     */
    public void modify(TagUpdateReq req) {

        Tag tag = getTagById(req.getId());

        if (req.getParentId().equals(req.getId())) {
            oops("不允许将自己设为自己的子标签", "Cannot set a tag as its own child.");
        }

        if (!req.getParentId().equals(0L)) {
            Tag parent = getById(req.getParentId());
            if (parent == null) {
                oops("父标签不存在", "Parent tag does not exist.");
            }
        }

        // 同名 + 同父级去重（排除自身）
        List<Tag> duplicates = lambdaQuery()
                .eq(Tag::getName, req.getName())
                .eq(Tag::getParentId, req.getParentId())
                .isNull(Tag::getAccountId)
                .ne(Tag::getId, req.getId())
                .list();
        if (CollectionUtils.isNotEmpty(duplicates)) {
            oops("同级下已存在同名标签：%s", "Duplicate tag name under the same parent: %s", req.getName());
        }

        tag.setName(req.getName());
        tag.setType(req.getType());
        tag.setParentId(req.getParentId());

        boolean success = updateById(tag);
        if (!success) {
            oops("编辑标签失败", "Failed to update tag.");
        }
    }

    /**
     * 删除全局标签（管理员）
     */
    public void delete(TagDeleteReq req) {

        Tag tag = getTagById(req.getId());

        // 检查是否有子标签
        List<Tag> children = lambdaQuery().eq(Tag::getParentId, tag.getId()).list();
        if (CollectionUtils.isNotEmpty(children)) {
            oops("该标签下有子标签，请先删除子标签", "This tag has children, please delete them first.");
        }

        // 检查是否被错题引用
        long refCount = mistakeTagMapper.selectCount(
                Wrappers.<MistakeTag>lambdaQuery().eq(MistakeTag::getTagId, tag.getId()));
        if (refCount > 0) {
            oops("该标签已被错题引用，无法删除", "This tag is referenced by mistakes and cannot be deleted.");
        }

        boolean success = removeById(tag.getId());
        if (!success) {
            oops("删除标签失败", "Failed to delete tag.");
        }
    }

    /**
     * 查询当前用户的自定义标签列表
     */
    public List<TagResp> listCustom() {

        Long accountId = threadStorageUtil.getCurAccountId();
        List<Tag> tags = lambdaQuery()
                .eq(Tag::getAccountId, accountId)
                .orderByDesc(Tag::getCreatedTime)
                .list();

        return tags.stream().map(this::toResp).collect(Collectors.toList());
    }

    /**
     * 查询所有自定义标签（管理员，不过滤用户）
     */
    public List<TagResp> listAllCustom() {

        List<Tag> tags = lambdaQuery()
                .isNotNull(Tag::getAccountId)
                .orderByDesc(Tag::getCreatedTime)
                .list();

        List<TagResp> respList = tags.stream().map(this::toResp).collect(Collectors.toList());

        // 批量填充所属用户昵称
        Set<Long> accountIds = tags.stream().map(Tag::getAccountId).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(accountIds)) {
            Map<Long, String> nicknameMap = accountService.listByIds(accountIds).stream()
                    .collect(Collectors.toMap(Account::getId, a -> a.getNickname() != null ? a.getNickname() : a.getCode()));
            for (TagResp resp : respList) {
                if (resp.getAccountId() != null) {
                    resp.setAccountNickname(nicknameMap.get(resp.getAccountId()));
                }
            }
        }

        return respList;
    }

    /**
     * 新增自定义标签（学生）
     */
    public void addCustom(TagCustomAddReq req) {

        Long accountId = threadStorageUtil.getCurAccountId();

        // 同名去重（同一用户下）
        List<Tag> duplicates = lambdaQuery()
                .eq(Tag::getAccountId, accountId)
                .eq(Tag::getName, req.getName())
                .list();
        if (CollectionUtils.isNotEmpty(duplicates)) {
            oops("已存在同名自定义标签：%s", "Duplicate custom tag name: %s", req.getName());
        }

        Tag tag = Tag.builder()
                .name(req.getName())
                .type(TagType.CUSTOM.getCode())
                .parentId(0L)
                .accountId(accountId)
                .build();

        boolean success = save(tag);
        if (!success) {
            oops("新增自定义标签失败", "Failed to add custom tag.");
        }
    }

    /**
     * 删除自定义标签（学生，校验归属）
     */
    public void deleteCustom(TagDeleteReq req) {

        Long accountId = threadStorageUtil.getCurAccountId();
        Tag tag = getTagById(req.getId());

        if (!accountId.equals(tag.getAccountId())) {
            oops("无权删除该标签", "Access denied for deleting this tag.");
        }

        // 检查是否被错题引用
        long refCount = mistakeTagMapper.selectCount(
                Wrappers.<MistakeTag>lambdaQuery().eq(MistakeTag::getTagId, tag.getId()));
        if (refCount > 0) {
            oops("该标签已被错题引用，无法删除", "This tag is referenced by mistakes and cannot be deleted.");
        }

        boolean success = removeById(tag.getId());
        if (!success) {
            oops("删除自定义标签失败", "Failed to delete custom tag.");
        }
    }

    /**
     * 展开标签ID为其自身+所有子孙标签ID（用于层级查询）
     */
    public Set<Long> expandDescendantTagIds(Long tagId) {

        List<Tag> all = lambdaQuery().isNull(Tag::getAccountId).list();
        Map<Long, List<Tag>> groupByParent = all.stream()
                .collect(Collectors.groupingBy(Tag::getParentId));

        Set<Long> result = new HashSet<>();
        collectDescendants(groupByParent, tagId, result);
        return result;
    }

    // ===== 工具方法 =====

    /**
     * 根据ID查询标签，不存在则抛出异常
     */
    private Tag getTagById(Long id) {

        List<Tag> tags = lambdaQuery().eq(Tag::getId, id).list();
        if (CollectionUtils.isEmpty(tags)) {
            oops("标签不存在", "Tag does not exist.");
        }
        return tags.get(0);
    }

    /**
     * 根据ID列表查询标签列表（不含children，用于详情回显）
     */
    public List<TagResp> getByIds(List<Long> ids) {

        if (CollectionUtils.isEmpty(ids)) {
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
     * 递归收集指定节点及所有子孙节点的ID
     */
    private void collectDescendants(Map<Long, List<Tag>> groupByParent, Long parentId, Set<Long> result) {

        result.add(parentId);
        List<Tag> children = groupByParent.getOrDefault(parentId, new ArrayList<>());
        for (Tag child : children) {
            collectDescendants(groupByParent, child.getId(), result);
        }
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
