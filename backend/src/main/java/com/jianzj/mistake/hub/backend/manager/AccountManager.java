package com.jianzj.mistake.hub.backend.manager;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzj.mistake.hub.backend.dto.resp.AccountDetailResp;
import com.jianzj.mistake.hub.backend.service.AccountService;
import com.jianzj.mistake.hub.backend.service.MistakeService;
import com.jianzj.mistake.hub.backend.service.ReviewPlanService;
import com.jianzj.mistake.hub.backend.service.ReviewRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户 Manager 层，解决 AccountService 与 MistakeService/ReviewRecordService 的循环依赖。
 * 编排用户列表查询并聚合统计数据。
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Service
@Slf4j
public class AccountManager {

    private final AccountService accountService;

    private final MistakeService mistakeService;

    private final ReviewRecordService reviewRecordService;

    private final ReviewPlanService reviewPlanService;

    public AccountManager(AccountService accountService,
                          MistakeService mistakeService,
                          ReviewRecordService reviewRecordService,
                          ReviewPlanService reviewPlanService) {

        this.accountService = accountService;
        this.mistakeService = mistakeService;
        this.reviewRecordService = reviewRecordService;
        this.reviewPlanService = reviewPlanService;
    }

    /**
     * 分页查询用户列表（含学习统计字段）
     */
    public Page<AccountDetailResp> listWithStats(String code, String nickname, String role, Long pageNum, Long pageSize) {

        Page<AccountDetailResp> page = accountService.list(code, nickname, role, pageNum, pageSize);

        List<AccountDetailResp> records = page.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return page;
        }

        Set<Long> accountIds = records.stream()
                .map(AccountDetailResp::getId)
                .collect(Collectors.toSet());

        // 批量聚合统计数据
        Map<Long, Integer> totalMap = mistakeService.countValidGroupByAccountId(accountIds);
        Map<Long, Integer> masteredMap = mistakeService.countMasteredGroupByAccountId(accountIds);
        Map<Long, Integer> streakMap = reviewPlanService.calculateStreakByAccountIds(accountIds);
        Map<Long, LocalDateTime> lastActiveMap = reviewRecordService.getLastActiveTimeByAccountIds(accountIds);

        for (AccountDetailResp resp : records) {
            Long id = resp.getId();
            resp.setTotalMistakes(totalMap.getOrDefault(id, 0));
            resp.setMasteredCount(masteredMap.getOrDefault(id, 0));
            resp.setCurrentStreak(streakMap.getOrDefault(id, 0));
            resp.setLastActiveTime(lastActiveMap.get(id));
        }

        return page;
    }
}
