package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.client.OssClient;
import com.jianzj.mistake.hub.backend.dto.req.ReviewRecordAdminReq;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewRecordAdminResp;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewRecordResp;
import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.entity.Mistake;
import com.jianzj.mistake.hub.backend.entity.ReviewRecord;
import com.jianzj.mistake.hub.backend.mapper.ReviewRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

/**
 * <p>
 * 复习记录 服务类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-09
 */
@Service
@Slf4j
public class ReviewRecordService extends ServiceImpl<ReviewRecordMapper, ReviewRecord> {

    private final AccountService accountService;

    private final MistakeService mistakeService;

    private final OssClient ossClient;

    public ReviewRecordService(AccountService accountService,
                               MistakeService mistakeService,
                               OssClient ossClient) {

        this.accountService = accountService;
        this.mistakeService = mistakeService;
        this.ossClient = ossClient;
    }

    // ===== 业务方法 =====

    /**
     * 创建复习记录
     */
    public void createRecord(ReviewRecord record) {

        boolean success = save(record);
        if (!success) {
            oops("创建复习记录失败", "Failed to create review record.");
        }
    }

    /**
     * 查询某道错题的复习记录（按时间倒序）
     */
    public List<ReviewRecordResp> listByMistake(Long accountId, Long mistakeId) {

        List<ReviewRecord> records = lambdaQuery()
                .eq(ReviewRecord::getAccountId, accountId)
                .eq(ReviewRecord::getMistakeId, mistakeId)
                .orderByDesc(ReviewRecord::getReviewTime)
                .list();

        if (CollectionUtils.isEmpty(records)) {
            return new ArrayList<>();
        }

        return records.stream()
                .map(this::toResp)
                .collect(Collectors.toList());
    }

    /**
     * 管理员分页查询复习记录
     */
    public Page<ReviewRecordAdminResp> listPageForAdmin(ReviewRecordAdminReq req) {

        Page<ReviewRecord> page = lambdaQuery()
                .eq(req.getAccountId() != null, ReviewRecord::getAccountId, req.getAccountId())
                .eq(req.getMistakeId() != null, ReviewRecord::getMistakeId, req.getMistakeId())
                .eq(req.getIsCorrect() != null, ReviewRecord::getIsCorrect, req.getIsCorrect())
                .ge(req.getStartTime() != null, ReviewRecord::getReviewTime, req.getStartTime())
                .le(req.getEndTime() != null, ReviewRecord::getReviewTime, req.getEndTime())
                .orderByDesc(ReviewRecord::getReviewTime)
                .page(new Page<>(req.getPageNum(), req.getPageSize()));

        List<ReviewRecord> records = page.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            Page<ReviewRecordAdminResp> respPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            respPage.setRecords(new ArrayList<>());
            return respPage;
        }

        // 批量查询关联用户信息
        Set<Long> accountIds = records.stream().map(ReviewRecord::getAccountId).collect(Collectors.toSet());
        Map<Long, Account> accountMap = accountService.listByIds(new ArrayList<>(accountIds)).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        // 批量查询关联错题标题
        Set<Long> mistakeIds = records.stream().map(ReviewRecord::getMistakeId).collect(Collectors.toSet());
        Map<Long, Mistake> mistakeMap = mistakeService.listByIds(new ArrayList<>(mistakeIds)).stream()
                .collect(Collectors.toMap(Mistake::getId, m -> m));

        List<ReviewRecordAdminResp> respList = records.stream()
                .map(record -> {
                    ReviewRecordAdminResp resp = new ReviewRecordAdminResp();
                    BeanUtils.copyProperties(record, resp);

                    Account account = accountMap.get(record.getAccountId());
                    if (account != null) {
                        resp.setAccountCode(account.getCode());
                        resp.setAccountNickname(account.getNickname());
                    }

                    Mistake mistake = mistakeMap.get(record.getMistakeId());
                    if (mistake != null) {
                        String title = mistake.getTitle();
                        resp.setMistakeTitle(title != null && title.length() > 80 ? title.substring(0, 80) : title);
                    }

                    return resp;
                })
                .collect(Collectors.toList());

        Page<ReviewRecordAdminResp> respPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        respPage.setRecords(respList);
        return respPage;
    }

    /**
     * 查询某用户时间范围内的复习记录
     */
    public List<ReviewRecord> listByAccountAndTimeRange(Long accountId, LocalDateTime start, LocalDateTime end) {

        return lambdaQuery()
                .eq(ReviewRecord::getAccountId, accountId)
                .ge(ReviewRecord::getReviewTime, start)
                .le(ReviewRecord::getReviewTime, end)
                .list();
    }

    /**
     * 统计今日活跃用户数（有复习记录的用户去重）
     */
    public long countActiveUsersToday(LocalDateTime dayStart, LocalDateTime dayEnd) {

        List<ReviewRecord> records = lambdaQuery()
                .ge(ReviewRecord::getReviewTime, dayStart)
                .le(ReviewRecord::getReviewTime, dayEnd)
                .select(ReviewRecord::getAccountId)
                .list();

        if (CollectionUtils.isEmpty(records)) {
            return 0;
        }

        return records.stream()
                .map(ReviewRecord::getAccountId)
                .distinct()
                .count();
    }

    /**
     * 批量查询每个用户的最后活跃时间（max(review_time)）
     */
    public Map<Long, LocalDateTime> getLastActiveTimeByAccountIds(Collection<Long> accountIds) {

        if (CollectionUtils.isEmpty(accountIds)) {
            return Collections.emptyMap();
        }

        List<ReviewRecord> records = lambdaQuery()
                .in(ReviewRecord::getAccountId, accountIds)
                .select(ReviewRecord::getAccountId, ReviewRecord::getReviewTime)
                .orderByDesc(ReviewRecord::getReviewTime)
                .list();

        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyMap();
        }

        Map<Long, LocalDateTime> result = new HashMap<>();
        for (ReviewRecord record : records) {
            result.putIfAbsent(record.getAccountId(), record.getReviewTime());
        }
        return result;
    }

    /**
     * 查询某道错题的全部原始复习记录（按时间升序，供遗忘曲线使用）
     */
    public List<ReviewRecord> listRawByMistakeAsc(Long mistakeId) {

        return lambdaQuery()
                .eq(ReviewRecord::getMistakeId, mistakeId)
                .orderByAsc(ReviewRecord::getReviewTime)
                .list();
    }

    // ===== 工具方法 =====

    /**
     * 查询某错题的所有复习记录实体（供清理任务提取 noteImageUrl）
     */
    public List<ReviewRecord> listEntityByMistakeId(Long mistakeId) {

        return lambdaQuery()
                .eq(ReviewRecord::getMistakeId, mistakeId)
                .list();
    }

    /**
     * 清理笔记图片：先置空字段，再删 OSS；OSS 失败则事务回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupNoteImage(Long recordId, String noteImageUrl) {

        lambdaUpdate().eq(ReviewRecord::getId, recordId).set(ReviewRecord::getNoteImageUrl, null).update();
        ossClient.deleteByUrl(noteImageUrl);
    }

    /**
     * ReviewRecord -> ReviewRecordResp
     */
    private ReviewRecordResp toResp(ReviewRecord record) {

        ReviewRecordResp resp = new ReviewRecordResp();
        BeanUtils.copyProperties(record, resp);
        return resp;
    }
}
