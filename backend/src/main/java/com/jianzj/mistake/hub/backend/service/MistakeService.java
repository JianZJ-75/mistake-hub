package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.dto.req.MistakeAddReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeDeleteReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeDetailReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeUpdateReq;
import com.jianzj.mistake.hub.backend.dto.resp.MistakeDetailResp;
import com.jianzj.mistake.hub.backend.dto.resp.TagResp;
import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.entity.Mistake;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.mapper.MistakeMapper;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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

    private static final int STATUS_VALID = 1;

    private static final int STATUS_DELETED = 0;

    private final MistakeTagService mistakeTagService;

    private final TagService tagService;

    private final AccountService accountService;

    private final ThreadStorageUtil threadStorageUtil;

    private final ReviewPlanService reviewPlanService;

    private final ReviewScheduleService reviewScheduleService;

    public MistakeService(MistakeTagService mistakeTagService,
                          TagService tagService,
                          AccountService accountService,
                          ThreadStorageUtil threadStorageUtil,
                          ReviewPlanService reviewPlanService,
                          @Lazy ReviewScheduleService reviewScheduleService) {

        this.mistakeTagService = mistakeTagService;
        this.tagService = tagService;
        this.accountService = accountService;
        this.threadStorageUtil = threadStorageUtil;
        this.reviewPlanService = reviewPlanService;
        this.reviewScheduleService = reviewScheduleService;
    }

    // ===== 业务方法 =====

    /**
     * 录入错题（2.5 + 2.10：自动初始化复习状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public void add(MistakeAddReq req) {

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
                .status(STATUS_VALID)
                .build();

        boolean success = save(mistake);
        if (!success) {
            oops("录入错题失败", "Failed to add mistake.");
        }

        if (CollectionUtils.isNotEmpty(req.getTagIds())) {
            mistakeTagService.saveTagIds(mistake.getId(), req.getTagIds());
        }
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

        Page<Mistake> page = lambdaQuery()
                .eq(!admin, Mistake::getAccountId, threadStorageUtil.getCurAccountId())
                .eq(admin && accountId != null, Mistake::getAccountId, accountId)
                .eq(Mistake::getStatus, STATUS_VALID)
                .in(finalTagMistakeIds != null, Mistake::getId, finalTagMistakeIds != null ? finalTagMistakeIds : List.of())
                .ge(masteryFilter != null && masteryFilter == 2, Mistake::getMasteryLevel, 80)
                .ge(masteryFilter != null && masteryFilter == 1, Mistake::getMasteryLevel, 60)
                .lt(masteryFilter != null && masteryFilter == 1, Mistake::getMasteryLevel, 80)
                .lt(masteryFilter != null && masteryFilter == 0, Mistake::getMasteryLevel, 60)
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
     * 删除错题（软删除，2.9）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(MistakeDeleteReq req) {

        Mistake mistake = isAdmin() ? getMistakeValidById(req.getId()) : getMistakeOwnedByCurrentUser(req.getId());
        mistake.setStatus(STATUS_DELETED);

        boolean success = updateById(mistake);
        if (!success) {
            oops("删除错题失败", "Failed to delete mistake.");
        }

        mistakeTagService.removeByMistakeId(mistake.getId());

        // 联动：将当天 PENDING 的复习计划标记为 SKIPPED，并清除缓存
        reviewPlanService.skipPendingPlansByMistake(mistake.getId(), LocalDate.now());
        reviewScheduleService.evictDailyCache(mistake.getAccountId());
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
                .eq(Mistake::getStatus, STATUS_VALID)
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
                .eq(Mistake::getStatus, STATUS_VALID)
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
                .eq(Mistake::getStatus, STATUS_VALID)
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
}
