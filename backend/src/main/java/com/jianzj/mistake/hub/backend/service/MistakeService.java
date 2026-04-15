package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.client.OssClient;
import com.jianzj.mistake.hub.backend.dto.req.MistakeAddReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeAdminListReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeDeleteReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeDetailReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeUpdateReq;
import com.jianzj.mistake.hub.backend.dto.resp.MistakeDetailResp;
import com.jianzj.mistake.hub.backend.dto.resp.TagResp;
import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.entity.Mistake;
import com.jianzj.mistake.hub.backend.enums.MistakeStatus;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.mapper.MistakeMapper;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

/**
 * <p>
 * 错题 服务类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Service
@Slf4j
public class MistakeService extends ServiceImpl<MistakeMapper, Mistake> {

    private final MistakeTagService mistakeTagService;

    private final TagService tagService;

    private final AccountService accountService;

    private final OssClient ossClient;

    private final ThreadStorageUtil threadStorageUtil;

    public MistakeService(MistakeTagService mistakeTagService,
                          TagService tagService,
                          AccountService accountService,
                          OssClient ossClient,
                          ThreadStorageUtil threadStorageUtil) {

        this.mistakeTagService = mistakeTagService;
        this.tagService = tagService;
        this.accountService = accountService;
        this.ossClient = ossClient;
        this.threadStorageUtil = threadStorageUtil;
    }

    // ===== 业务方法 =====

    /**
     * 录入错题（2.5 + 2.10：自动初始化复习状态），返回错题ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long add(MistakeAddReq req) {

        Long accountId = threadStorageUtil.getCurAccountId();

        Mistake mistake = Mistake.builder()
                .accountId(accountId)
                .title(req.getTitle())
                .correctAnswer(req.getCorrectAnswer())
                .titleImageUrl(req.getTitleImageUrl())
                .answerImageUrl(req.getAnswerImageUrl())
                .reviewStage(0)
                .masteryLevel(0)
                .nextReviewTime(LocalDateTime.now())
                .status(MistakeStatus.VALID.getCode())
                .build();

        boolean success = save(mistake);
        if (!success) {
            oops("录入错题失败", "Failed to add mistake.");
        }

        if (CollectionUtils.isNotEmpty(req.getTagIds())) {
            mistakeTagService.saveTagIds(mistake.getId(), req.getTagIds());
        }

        return mistake.getId();
    }

    /**
     * 分页查询错题列表（2.8）
     */
    public Page<MistakeDetailResp> listPage(Long accountId, String tagIds, Integer masteryFilter, Long pageNum, Long pageSize) {

        boolean admin = isAdmin();

        // 标签筛选：解析逗号分隔的 tagIds，展开每个标签的所有子孙，再查出符合条件的 mistakeId 集合
        List<Long> tagMistakeIds = null;
        List<Long> parsedTagIds = parseTagIds(tagIds);
        if (CollectionUtils.isNotEmpty(parsedTagIds)) {
            Set<Long> expandedTagIds = new HashSet<>();
            for (Long id : parsedTagIds) {
                expandedTagIds.addAll(tagService.expandDescendantTagIds(id));
            }
            tagMistakeIds = mistakeTagService.getMistakeIdsByTagIds(expandedTagIds);
            if (CollectionUtils.isEmpty(tagMistakeIds)) {
                return new Page<>(pageNum, pageSize, 0);
            }
        }

        final List<Long> finalTagMistakeIds = tagMistakeIds;

        LambdaQueryChainWrapper<Mistake> query = lambdaQuery()
                .eq(!admin, Mistake::getAccountId, threadStorageUtil.getCurAccountId())
                .eq(admin && accountId != null, Mistake::getAccountId, accountId)
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .in(finalTagMistakeIds != null, Mistake::getId, finalTagMistakeIds != null ? finalTagMistakeIds : List.of());

        Page<Mistake> page = applyMasteryFilter(query, masteryFilter)
                .orderByDesc(Mistake::getCreatedTime)
                .page(new Page<>(pageNum, pageSize));

        List<Mistake> records = page.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            Page<MistakeDetailResp> respPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            respPage.setRecords(new ArrayList<>());
            return respPage;
        }

        // 批量查询标签关联和标签数据，避免 N+1
        List<Long> mistakeIds = records.stream().map(Mistake::getId).collect(Collectors.toList());
        Map<Long, List<Long>> tagIdMap = mistakeTagService.getTagIdMapByMistakeIds(mistakeIds);

        Set<Long> allTagIds = tagIdMap.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        List<TagResp> allTags = tagService.getByIds(new ArrayList<>(allTagIds));
        Map<Long, TagResp> tagRespMap = allTags.stream().collect(Collectors.toMap(TagResp::getId, t -> t));

        // 批量查询所属用户昵称
        Set<Long> accountIds = records.stream().map(Mistake::getAccountId).collect(Collectors.toSet());
        Map<Long, String> nicknameMap = accountService.listByIds(new ArrayList<>(accountIds)).stream()
                .collect(Collectors.toMap(Account::getId, Account::getNickname));

        List<MistakeDetailResp> respList = records.stream()
                .map(mistake -> {
                    MistakeDetailResp resp = new MistakeDetailResp();
                    BeanUtils.copyProperties(mistake, resp);
                    resp.setAccountNickname(nicknameMap.getOrDefault(mistake.getAccountId(), ""));
                    List<Long> mistakeTagIds = tagIdMap.getOrDefault(mistake.getId(), List.of());
                    List<TagResp> tags = mistakeTagIds.stream()
                            .map(tagRespMap::get)
                            .filter(t -> t != null)
                            .collect(Collectors.toList());
                    resp.setTags(tags);
                    return resp;
                })
                .collect(Collectors.toList());

        Page<MistakeDetailResp> respPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        respPage.setRecords(respList);
        return respPage;
    }

    /**
     * 查询错题详情（2.9）
     */
    public MistakeDetailResp detail(MistakeDetailReq req) {

        Mistake mistake = isAdmin() ? getMistakeValidById(req.getId()) : getMistakeOwnedByCurrentUser(req.getId());
        return toDetailResp(mistake);
    }

    /**
     * 编辑错题（2.9）
     */
    @Transactional(rollbackFor = Exception.class)
    public void modify(MistakeUpdateReq req) {

        Mistake mistake = isAdmin() ? getMistakeValidById(req.getId()) : getMistakeOwnedByCurrentUser(req.getId());

        if (StringUtils.isNotBlank(req.getTitle())) {
            mistake.setTitle(req.getTitle());
        }
        if (req.getCorrectAnswer() != null) {
            mistake.setCorrectAnswer(req.getCorrectAnswer().isEmpty() ? null : req.getCorrectAnswer());
        }
        if (req.getTitleImageUrl() != null) {
            mistake.setTitleImageUrl(req.getTitleImageUrl().isEmpty() ? null : req.getTitleImageUrl());
        }
        if (req.getAnswerImageUrl() != null) {
            mistake.setAnswerImageUrl(req.getAnswerImageUrl().isEmpty() ? null : req.getAnswerImageUrl());
        }
        boolean success = updateById(mistake);
        if (!success) {
            oops("编辑错题失败", "Failed to update mistake.");
        }

        if (req.getTagIds() != null) {
            mistakeTagService.saveTagIds(mistake.getId(), req.getTagIds());
        }
    }

    /**
     * 管理员错题列表（全量查询，含用户信息）
     */
    public Page<MistakeDetailResp> adminListPage(MistakeAdminListReq req) {

        // 标签筛选
        List<Long> tagMistakeIds = null;
        if (req.getTagId() != null) {
            Set<Long> expandedTagIds = new HashSet<>();
            expandedTagIds.addAll(tagService.expandDescendantTagIds(req.getTagId()));
            tagMistakeIds = mistakeTagService.getMistakeIdsByTagIds(expandedTagIds);
            if (CollectionUtils.isEmpty(tagMistakeIds)) {
                return new Page<>(req.getPageNum(), req.getPageSize(), 0);
            }
        }

        final List<Long> finalTagMistakeIds = tagMistakeIds;
        Integer masteryFilter = req.getMasteryFilter();
        Integer statusFilter = req.getStatusFilter();

        LambdaQueryChainWrapper<Mistake> query = lambdaQuery()
                .eq(req.getAccountId() != null, Mistake::getAccountId, req.getAccountId())
                .eq(statusFilter != null, Mistake::getStatus, statusFilter)
                .eq(statusFilter == null, Mistake::getStatus, MistakeStatus.VALID.getCode())
                .in(finalTagMistakeIds != null, Mistake::getId, finalTagMistakeIds != null ? finalTagMistakeIds : List.of());

        Page<Mistake> page = applyMasteryFilter(query, masteryFilter)
                .orderByDesc(Mistake::getCreatedTime)
                .page(new Page<>(req.getPageNum(), req.getPageSize()));

        List<Mistake> records = page.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            Page<MistakeDetailResp> respPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            respPage.setRecords(new ArrayList<>());
            return respPage;
        }

        // 批量查询标签
        List<Long> mistakeIds = records.stream().map(Mistake::getId).collect(Collectors.toList());
        Map<Long, List<Long>> tagIdMap = mistakeTagService.getTagIdMapByMistakeIds(mistakeIds);

        Set<Long> allTagIds = tagIdMap.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        List<TagResp> allTags = tagService.getByIds(new ArrayList<>(allTagIds));
        Map<Long, TagResp> tagRespMap = allTags.stream().collect(Collectors.toMap(TagResp::getId, t -> t));

        // 批量查询用户信息
        Set<Long> accountIds = records.stream().map(Mistake::getAccountId).collect(Collectors.toSet());
        Map<Long, Account> accountMap = accountService.listByIds(new ArrayList<>(accountIds)).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        List<MistakeDetailResp> respList = records.stream()
                .map(mistake -> {
                    MistakeDetailResp resp = new MistakeDetailResp();
                    BeanUtils.copyProperties(mistake, resp);

                    Account account = accountMap.get(mistake.getAccountId());
                    if (account != null) {
                        resp.setAccountCode(account.getCode());
                        resp.setAccountNickname(account.getNickname());
                    }

                    List<Long> mTagIds = tagIdMap.getOrDefault(mistake.getId(), List.of());
                    List<TagResp> tags = mTagIds.stream()
                            .map(tagRespMap::get)
                            .filter(t -> t != null)
                            .collect(Collectors.toList());
                    resp.setTags(tags);
                    return resp;
                })
                .collect(Collectors.toList());

        Page<MistakeDetailResp> respPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        respPage.setRecords(respList);
        return respPage;
    }

    /**
     * 管理员编辑错题（不校验归属，仅校验错题有效）
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminModify(MistakeUpdateReq req) {

        Mistake mistake = getMistakeValidById(req.getId());

        if (StringUtils.isNotBlank(req.getTitle())) {
            mistake.setTitle(req.getTitle());
        }
        if (req.getCorrectAnswer() != null) {
            mistake.setCorrectAnswer(req.getCorrectAnswer().isEmpty() ? null : req.getCorrectAnswer());
        }
        if (req.getTitleImageUrl() != null) {
            mistake.setTitleImageUrl(req.getTitleImageUrl().isEmpty() ? null : req.getTitleImageUrl());
        }
        if (req.getAnswerImageUrl() != null) {
            mistake.setAnswerImageUrl(req.getAnswerImageUrl().isEmpty() ? null : req.getAnswerImageUrl());
        }
        boolean success = updateById(mistake);
        if (!success) {
            oops("编辑错题失败", "Failed to update mistake.");
        }

        if (req.getTagIds() != null) {
            mistakeTagService.saveTagIds(mistake.getId(), req.getTagIds());
        }
    }

    /**
     * 删除错题（软删除 + 标签清理），返回被删除的 Mistake
     */
    @Transactional(rollbackFor = Exception.class)
    public Mistake delete(MistakeDeleteReq req) {

        Mistake mistake = isAdmin() ? getMistakeValidById(req.getId()) : getMistakeOwnedByCurrentUser(req.getId());
        mistake.setStatus(MistakeStatus.PENDING_DELETE.getCode());

        boolean success = updateById(mistake);
        if (!success) {
            oops("删除错题失败", "Failed to delete mistake.");
        }

        mistakeTagService.removeByMistakeId(mistake.getId());

        return mistake;
    }

    /**
     * 校验错题存在且属于当前用户（供复习模块调用）
     */
    public Mistake getValidMistakeForCurrentUser(Long mistakeId) {

        return getMistakeOwnedByCurrentUser(mistakeId);
    }

    /**
     * 校验错题有效：管理员可查任意错题，普通用户只能查自己的
     */
    public Mistake getValidMistake(Long mistakeId) {

        return isAdmin() ? getMistakeValidById(mistakeId) : getMistakeOwnedByCurrentUser(mistakeId);
    }

    // ===== 工具方法 =====

    /**
     * 应用掌握度筛选条件
     */
    private LambdaQueryChainWrapper<Mistake> applyMasteryFilter(
            LambdaQueryChainWrapper<Mistake> query, Integer masteryFilter) {

        if (masteryFilter == null) {
            return query;
        }
        return switch (masteryFilter) {
            case 0 -> query.lt(Mistake::getMasteryLevel, 60);
            case 1 -> query.ge(Mistake::getMasteryLevel, 60).lt(Mistake::getMasteryLevel, 80);
            case 2 -> query.ge(Mistake::getMasteryLevel, 80);
            default -> query;
        };
    }

    /**
     * 解析逗号分隔的 tagIds 字符串
     */
    private List<Long> parseTagIds(String tagIds) {

        if (StringUtils.isBlank(tagIds)) {
            return null;
        }
        return Arrays.stream(tagIds.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(Long::parseLong)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 当前用户是否为管理员
     */
    private boolean isAdmin() {

        return Role.ADMIN.getCode().equals(threadStorageUtil.getCurAccountRole());
    }

    /**
     * 根据ID查询有效错题（不校验归属，管理员专用）
     */
    private Mistake getMistakeValidById(Long mistakeId) {

        List<Mistake> mistakes = lambdaQuery()
                .eq(Mistake::getId, mistakeId)
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .list();

        if (CollectionUtils.isEmpty(mistakes)) {
            oops("错题不存在", "Mistake does not exist.");
        }

        return mistakes.get(0);
    }

    /**
     * 查询当前用户的有效错题，不存在则抛出异常
     */
    private Mistake getMistakeOwnedByCurrentUser(Long mistakeId) {

        Long accountId = threadStorageUtil.getCurAccountId();

        List<Mistake> mistakes = lambdaQuery()
                .eq(Mistake::getId, mistakeId)
                .eq(Mistake::getAccountId, accountId)
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .list();

        if (CollectionUtils.isEmpty(mistakes)) {
            oops("错题不存在或无权操作", "Mistake does not exist or access denied.");
        }

        return mistakes.get(0);
    }

    /**
     * Mistake -> MistakeDetailResp（含标签）
     */
    private MistakeDetailResp toDetailResp(Mistake mistake) {

        MistakeDetailResp resp = new MistakeDetailResp();
        BeanUtils.copyProperties(mistake, resp);

        Account account = accountService.getById(mistake.getAccountId());
        resp.setAccountNickname(account != null ? account.getNickname() : "");

        List<Long> tagIds = mistakeTagService.getTagIdsByMistakeId(mistake.getId());
        List<TagResp> tags = tagService.getByIds(tagIds);
        resp.setTags(tags);

        return resp;
    }

    /**
     * 查询某用户到期需要复习的错题列表
     */
    public List<Mistake> listDueForReview(Long accountId, LocalDateTime deadline) {

        return lambdaQuery()
                .eq(Mistake::getAccountId, accountId)
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .le(Mistake::getNextReviewTime, deadline)
                .list();
    }

    /**
     * 批量查询错题详情（含标签），供复习任务组装使用
     */
    public Map<Long, MistakeDetailResp> listDetailByIds(List<Long> mistakeIds) {

        if (CollectionUtils.isEmpty(mistakeIds)) {
            return Map.of();
        }

        List<Mistake> mistakes = listByIds(mistakeIds);
        if (CollectionUtils.isEmpty(mistakes)) {
            return Map.of();
        }

        Map<Long, List<Long>> tagIdMap = mistakeTagService.getTagIdMapByMistakeIds(mistakeIds);
        Set<Long> allTagIds = tagIdMap.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        List<TagResp> allTags = tagService.getByIds(new ArrayList<>(allTagIds));
        Map<Long, TagResp> tagRespMap = allTags.stream().collect(Collectors.toMap(TagResp::getId, Function.identity()));

        return mistakes.stream()
                .map(mistake -> {
                    MistakeDetailResp resp = new MistakeDetailResp();
                    BeanUtils.copyProperties(mistake, resp);
                    List<Long> tagIds = tagIdMap.getOrDefault(mistake.getId(), List.of());
                    List<TagResp> tags = tagIds.stream()
                            .map(tagRespMap::get)
                            .filter(t -> t != null)
                            .collect(Collectors.toList());
                    resp.setTags(tags);
                    return resp;
                })
                .collect(Collectors.toMap(MistakeDetailResp::getId, Function.identity()));
    }

    /**
     * 统计某用户有效错题总数
     */
    public long countValidByAccountId(Long accountId) {

        return lambdaQuery()
                .eq(Mistake::getAccountId, accountId)
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .count();
    }

    /**
     * 统计某用户掌握度 >= threshold 的错题数
     */
    public long countMasteredByAccountId(Long accountId, int threshold) {

        return lambdaQuery()
                .eq(Mistake::getAccountId, accountId)
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .ge(Mistake::getMasteryLevel, threshold)
                .count();
    }

    /**
     * 统计某用户待复习错题数（nextReviewTime <= deadline）
     */
    public long countPendingReview(Long accountId, LocalDateTime deadline) {

        return lambdaQuery()
                .eq(Mistake::getAccountId, accountId)
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .le(Mistake::getNextReviewTime, deadline)
                .count();
    }

    /**
     * 统计某用户本周新增错题数
     */
    public long countNewThisWeek(Long accountId, LocalDateTime weekStart) {

        return lambdaQuery()
                .eq(Mistake::getAccountId, accountId)
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .ge(Mistake::getCreatedTime, weekStart)
                .count();
    }

    /**
     * 统计某用户掌握度在 [min, max) 区间的错题数，max 为 null 则无上限
     */
    public long countByMasteryRange(Long accountId, int min, Integer max) {

        return lambdaQuery()
                .eq(Mistake::getAccountId, accountId)
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .ge(Mistake::getMasteryLevel, min)
                .lt(max != null, Mistake::getMasteryLevel, max)
                .count();
    }

    /**
     * 查询某用户所有有效错题ID列表
     */
    public List<Long> listValidMistakeIds(Long accountId) {

        List<Mistake> mistakes = lambdaQuery()
                .eq(Mistake::getAccountId, accountId)
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .select(Mistake::getId)
                .list();

        if (CollectionUtils.isEmpty(mistakes)) {
            return new ArrayList<>();
        }

        return mistakes.stream().map(Mistake::getId).collect(Collectors.toList());
    }

    /**
     * 统计全平台掌握度在 [min, max) 区间的有效错题数，max 为 null 则无上限
     */
    public long countAllByMasteryRange(int min, Integer max) {

        return lambdaQuery()
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .ge(Mistake::getMasteryLevel, min)
                .lt(max != null, Mistake::getMasteryLevel, max)
                .count();
    }

    /**
     * 统计全平台有效错题总数
     */
    public long countAllValid() {

        return lambdaQuery()
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .count();
    }

    /**
     * 查询某用户所有有效错题（供统计学科分布使用）
     */
    public List<Mistake> listValidByAccountId(Long accountId) {

        return lambdaQuery()
                .eq(Mistake::getAccountId, accountId)
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .select(Mistake::getId, Mistake::getMasteryLevel)
                .list();
    }

    /**
     * 批量统计每个用户的有效错题总数
     */
    public Map<Long, Integer> countValidGroupByAccountId(Collection<Long> accountIds) {

        if (CollectionUtils.isEmpty(accountIds)) {
            return Collections.emptyMap();
        }

        List<Mistake> mistakes = lambdaQuery()
                .in(Mistake::getAccountId, accountIds)
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .select(Mistake::getAccountId)
                .list();

        if (CollectionUtils.isEmpty(mistakes)) {
            return Collections.emptyMap();
        }

        return mistakes.stream()
                .collect(Collectors.groupingBy(Mistake::getAccountId, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }

    /**
     * 批量统计每个用户掌握度 >= 80 的错题数
     */
    public Map<Long, Integer> countMasteredGroupByAccountId(Collection<Long> accountIds) {

        if (CollectionUtils.isEmpty(accountIds)) {
            return Collections.emptyMap();
        }

        List<Mistake> mistakes = lambdaQuery()
                .in(Mistake::getAccountId, accountIds)
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .ge(Mistake::getMasteryLevel, 80)
                .select(Mistake::getAccountId)
                .list();

        if (CollectionUtils.isEmpty(mistakes)) {
            return Collections.emptyMap();
        }

        return mistakes.stream()
                .collect(Collectors.groupingBy(Mistake::getAccountId, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }

    /**
     * 查询全平台所有有效错题的 id + masteryLevel（供管理端学科统计使用）
     */
    public List<Mistake> listAllValidIdAndMastery() {

        return lambdaQuery()
                .eq(Mistake::getStatus, MistakeStatus.VALID.getCode())
                .select(Mistake::getId, Mistake::getMasteryLevel)
                .list();
    }

    /**
     * 分批查询待删除错题
     */
    public List<Mistake> listPendingDelete(int batchSize) {

        return lambdaQuery()
                .eq(Mistake::getStatus, MistakeStatus.PENDING_DELETE.getCode())
                .last("LIMIT " + batchSize)
                .list();
    }

    /**
     * 将错题状态标记为已删除
     */
    public void markDeleted(Long id) {

        boolean success = lambdaUpdate()
                .eq(Mistake::getId, id)
                .set(Mistake::getStatus, MistakeStatus.DELETED.getCode())
                .update();
        if (!success) {
            oops("标记错题已删除失败（id=%d）", "Failed to mark mistake as deleted (id=%d).", id);
        }
    }

    /**
     * 清理题目图片：先置空字段，再删 OSS；OSS 失败则事务回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupTitleImage(Long id, String imageUrl) {

        lambdaUpdate().eq(Mistake::getId, id).set(Mistake::getTitleImageUrl, null).update();
        ossClient.deleteByUrl(imageUrl);
    }

    /**
     * 清理答案图片：先置空字段，再删 OSS；OSS 失败则事务回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupAnswerImage(Long id, String imageUrl) {

        lambdaUpdate().eq(Mistake::getId, id).set(Mistake::getAnswerImageUrl, null).update();
        ossClient.deleteByUrl(imageUrl);
    }
}
