package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.entity.ReviewPlan;
import com.jianzj.mistake.hub.backend.mapper.ReviewPlanMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

/**
 * <p>
 * 复习计划 服务类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-02
 */
@Service
@Slf4j
public class ReviewPlanService extends ServiceImpl<ReviewPlanMapper, ReviewPlan> {

    // ===== 业务方法 =====

    /**
     * 批量创建复习计划
     */
    public void batchCreate(List<ReviewPlan> plans) {

        if (CollectionUtils.isEmpty(plans)) {
            return;
        }

        boolean success = saveBatch(plans);
        if (!success) {
            oops("批量创建复习计划失败", "Failed to batch create review plans.");
        }
    }

    /**
     * 查询某用户某天的所有复习计划
     */
    public List<ReviewPlan> listByAccountAndDate(Long accountId, LocalDate date) {

        return lambdaQuery()
                .eq(ReviewPlan::getAccountId, accountId)
                .eq(ReviewPlan::getPlannedDate, date)
                .list();
    }

    /**
     * 查询某用户某天已有计划的 mistakeId 集合（幂等检查）
     */
    public Set<Long> getPlannedMistakeIds(Long accountId, LocalDate date) {

        List<ReviewPlan> plans = lambdaQuery()
                .eq(ReviewPlan::getAccountId, accountId)
                .eq(ReviewPlan::getPlannedDate, date)
                .list();

        return plans.stream()
                .map(ReviewPlan::getMistakeId)
                .collect(Collectors.toSet());
    }

    /**
     * 更新复习计划状态
     */
    public void updateStatus(Long planId, String status) {

        ReviewPlan plan = getByIdOrThrow(planId);
        plan.setStatus(status);

        boolean success = updateById(plan);
        if (!success) {
            oops("更新复习计划状态失败", "Failed to update review plan status.");
        }
    }

    /**
     * 按错题+日期查询复习计划
     */
    public ReviewPlan getByMistakeAndDate(Long accountId, Long mistakeId, LocalDate date) {

        List<ReviewPlan> plans = lambdaQuery()
                .eq(ReviewPlan::getAccountId, accountId)
                .eq(ReviewPlan::getMistakeId, mistakeId)
                .eq(ReviewPlan::getPlannedDate, date)
                .list();

        return CollectionUtils.isEmpty(plans) ? null : plans.get(0);
    }

    // ===== 工具方法 =====

    /**
     * 根据ID查询复习计划，不存在则抛异常
     */
    private ReviewPlan getByIdOrThrow(Long planId) {

        List<ReviewPlan> plans = lambdaQuery()
                .eq(ReviewPlan::getId, planId)
                .list();

        if (CollectionUtils.isEmpty(plans)) {
            oops("复习计划不存在", "Review plan does not exist.");
        }

        return plans.get(0);
    }
}
