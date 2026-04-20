package com.jianzj.mistake.hub.backend.controller;

import com.jianzj.mistake.hub.backend.annotation.PreAuthorize;
import com.jianzj.mistake.hub.backend.dto.req.ForgettingCurveReq;
import com.jianzj.mistake.hub.backend.dto.resp.AdminOverviewResp;
import com.jianzj.mistake.hub.backend.dto.resp.DailyCompletionResp;
import com.jianzj.mistake.hub.backend.dto.resp.ForgettingCurveResp;
import com.jianzj.mistake.hub.backend.dto.resp.MasteryDistributionResp;
import com.jianzj.mistake.hub.backend.dto.resp.MasteryTrendResp;
import com.jianzj.mistake.hub.backend.dto.resp.MemoryHealthResp;
import com.jianzj.mistake.hub.backend.dto.resp.RetentionTrendResp;
import com.jianzj.mistake.hub.backend.dto.resp.StatsOverviewResp;
import com.jianzj.mistake.hub.backend.dto.resp.StreakResp;
import com.jianzj.mistake.hub.backend.dto.resp.SubjectStatsResp;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>统计 前端控制器</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@Tag(name = "统计")
@RestController
@RequestMapping("/v1/stats")
@Validated
@Slf4j
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {

        this.statsService = statsService;
    }

    /**
     * 错题总览统计
     */
    @Operation(summary = "错题总览统计")
    @PostMapping("/overview")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public StatsOverviewResp overview() {

        return statsService.overview();
    }

    /**
     * 学科分布统计
     */
    @Operation(summary = "学科分布统计")
    @PostMapping("/subject")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public List<SubjectStatsResp> subject() {

        return statsService.subject();
    }

    /**
     * 掌握度分布
     */
    @Operation(summary = "掌握度分布")
    @PostMapping("/mastery")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public MasteryDistributionResp mastery() {

        return statsService.mastery();
    }

    /**
     * 每日复习完成率
     */
    @Operation(summary = "每日复习完成率")
    @PostMapping("/daily-completion")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public List<DailyCompletionResp> dailyCompletion(
            @RequestParam(name = "days", defaultValue = "30") @Min(1) @Max(365) Integer days) {

        return statsService.dailyCompletion(days);
    }

    /**
     * 连续复习天数
     */
    @Operation(summary = "连续复习天数")
    @PostMapping("/streak")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public StreakResp streak() {

        return statsService.streak();
    }

    /**
     * 复习效果趋势
     */
    @Operation(summary = "复习效果趋势")
    @PostMapping("/mastery-trend")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public List<MasteryTrendResp> masteryTrend(
            @RequestParam(name = "days", defaultValue = "30") @Min(1) @Max(365) Integer days) {

        return statsService.masteryTrend(days);
    }

    /**
     * 管理端每日复习完成率（全平台）
     */
    @Operation(summary = "管理端每日复习完成率")
    @PostMapping("/admin-daily-completion")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public List<DailyCompletionResp> adminDailyCompletion() {

        return statsService.adminDailyCompletion();
    }

    /**
     * 管理端学科分布统计（全平台）
     */
    @Operation(summary = "管理端学科分布统计")
    @PostMapping("/admin-subject")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public List<SubjectStatsResp> adminSubject() {

        return statsService.adminSubject();
    }

    /**
     * 管理端全局统计
     */
    @Operation(summary = "管理端全局统计")
    @PostMapping("/admin-overview")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public AdminOverviewResp adminOverview() {

        return statsService.adminOverview();
    }

    /**
     * 管理端掌握度分布（全平台）
     */
    @Operation(summary = "管理端掌握度分布")
    @PostMapping("/admin-mastery")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public MasteryDistributionResp adminMastery() {

        return statsService.adminMastery();
    }

    /**
     * 单题遗忘曲线
     */
    @Operation(summary = "单题遗忘曲线")
    @PostMapping("/forgetting-curve")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public ForgettingCurveResp forgettingCurve(@RequestBody @Validated ForgettingCurveReq req) {

        return statsService.forgettingCurve(req.getMistakeId());
    }

    /**
     * 记忆健康总览
     */
    @Operation(summary = "记忆健康总览")
    @PostMapping("/memory-health")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public MemoryHealthResp memoryHealth() {

        return statsService.memoryHealth();
    }

    /**
     * 管理端记忆保持率趋势（近30天）
     */
    @Operation(summary = "管理端记忆保持率趋势")
    @PostMapping("/admin-retention-trend")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public List<RetentionTrendResp> adminRetentionTrend() {

        return statsService.adminRetentionTrend();
    }
}
