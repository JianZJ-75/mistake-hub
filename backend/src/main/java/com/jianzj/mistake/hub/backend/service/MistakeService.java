package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.dto.req.MistakeAddReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeDeleteReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeDetailReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeListReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeUpdateReq;
import com.jianzj.mistake.hub.backend.dto.resp.MistakeDetailResp;
import com.jianzj.mistake.hub.backend.dto.resp.TagResp;
import com.jianzj.mistake.hub.backend.entity.Mistake;
import com.jianzj.mistake.hub.backend.mapper.MistakeMapper;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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

    private final ThreadStorageUtil threadStorageUtil;

    public MistakeService(MistakeTagService mistakeTagService,
                          TagService tagService,
                          ThreadStorageUtil threadStorageUtil) {

        this.mistakeTagService = mistakeTagService;
        this.tagService = tagService;
        this.threadStorageUtil = threadStorageUtil;
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
                .errorReason(req.getErrorReason())
                .imageUrl(req.getImageUrl())
                .subject(req.getSubject())
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
    public Page<MistakeDetailResp> list(MistakeListReq req) {

        Long accountId = threadStorageUtil.getCurAccountId();

        // 标签筛选：先查出符合条件的 mistakeId 集合，再作为 in 条件，保证分页 total 准确
        List<Long> tagMistakeIds = null;
        if (req.getTagId() != null) {
            tagMistakeIds = mistakeTagService.getMistakeIdsByTagId(req.getTagId());
            if (CollectionUtils.isEmpty(tagMistakeIds)) {
                return new Page<>(req.getPageNum(), req.getPageSize(), 0);
            }
        }

        final List<Long> finalTagMistakeIds = tagMistakeIds;

        Page<Mistake> page = lambdaQuery()
                .eq(Mistake::getAccountId, accountId)
                .eq(Mistake::getStatus, STATUS_VALID)
                .eq(StringUtils.isNotBlank(req.getSubject()), Mistake::getSubject, req.getSubject())
                .in(finalTagMistakeIds != null, Mistake::getId, finalTagMistakeIds != null ? finalTagMistakeIds : List.of())
                .ge(req.getMasteryFilter() != null && req.getMasteryFilter() == 2, Mistake::getMasteryLevel, 80)
                .ge(req.getMasteryFilter() != null && req.getMasteryFilter() == 1, Mistake::getMasteryLevel, 60)
                .lt(req.getMasteryFilter() != null && req.getMasteryFilter() == 1, Mistake::getMasteryLevel, 80)
                .lt(req.getMasteryFilter() != null && req.getMasteryFilter() == 0, Mistake::getMasteryLevel, 60)
                .orderByDesc(Mistake::getCreatedTime)
                .page(new Page<>(req.getPageNum(), req.getPageSize()));

        List<MistakeDetailResp> respList = page.getRecords().stream()
                .map(this::toDetailResp)
                .collect(Collectors.toList());

        Page<MistakeDetailResp> respPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        respPage.setRecords(respList);
        return respPage;
    }

    /**
     * 查询错题详情（2.9）
     */
    public MistakeDetailResp detail(MistakeDetailReq req) {

        Mistake mistake = getMistakeOwnedByCurrentUser(req.getId());
        return toDetailResp(mistake);
    }

    /**
     * 编辑错题（2.9）
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(MistakeUpdateReq req) {

        Mistake mistake = getMistakeOwnedByCurrentUser(req.getId());

        if (StringUtils.isNotBlank(req.getTitle())) {
            mistake.setTitle(req.getTitle());
        }
        if (req.getCorrectAnswer() != null) {
            mistake.setCorrectAnswer(req.getCorrectAnswer());
        }
        if (req.getErrorReason() != null) {
            mistake.setErrorReason(req.getErrorReason());
        }
        if (req.getImageUrl() != null) {
            mistake.setImageUrl(req.getImageUrl());
        }
        if (StringUtils.isNotBlank(req.getSubject())) {
            mistake.setSubject(req.getSubject());
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
    public void delete(MistakeDeleteReq req) {

        Mistake mistake = getMistakeOwnedByCurrentUser(req.getId());
        mistake.setStatus(STATUS_DELETED);

        boolean success = updateById(mistake);
        if (!success) {
            oops("删除错题失败", "Failed to delete mistake.");
        }
    }

    // ===== 工具方法 =====

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

        List<Long> tagIds = mistakeTagService.getTagIdsByMistakeId(mistake.getId());
        List<TagResp> tags = tagService.getByIds(tagIds);
        resp.setTags(tags);

        return resp;
    }
}
