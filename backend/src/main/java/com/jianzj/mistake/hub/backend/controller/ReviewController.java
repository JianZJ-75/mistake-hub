package com.jianzj.mistake.hub.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzj.mistake.hub.backend.annotation.PreAuthorize;
import com.jianzj.mistake.hub.backend.dto.req.ReviewGenerateDailyReq;
import com.jianzj.mistake.hub.backend.dto.req.ReviewHistoryReq;
import com.jianzj.mistake.hub.backend.dto.req.ReviewRecordAdminReq;
import com.jianzj.mistake.hub.backend.dto.req.ReviewSkipReq;
import com.jianzj.mistake.hub.backend.dto.req.ReviewSubmitReq;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewProgressResp;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewRecordAdminResp;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewRecordResp;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewSubmitResp;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewTaskResp;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.service.ReviewRecordService;
import com.jianzj.mistake.hub.backend.service.ReviewScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 复习 前端控制器
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-02
 */
@Tag(name = "复习")
@RestController
@RequestMapping("/v1/review")
@Validated
@Slf4j
public class ReviewController {

    private final ReviewScheduleService reviewScheduleService;

    private final ReviewRecordService reviewRecordService;

    public ReviewController(ReviewScheduleService reviewScheduleService,
                            ReviewRecordService reviewRecordService) {

        this.reviewScheduleService = reviewScheduleService;
        this.reviewRecordService = reviewRecordService;
    }

    /**
     * 手动生成每日复习任务（管理员调试接口，正式环境由 ReviewScheduler 凌晨 2 点定时触发）
     */
    @Operation(summary = "手动生成每日复习任务")
    @PostMapping("/generate-daily")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public List<ReviewTaskResp> generateDaily(@RequestBody @Validated @NotNull ReviewGenerateDailyReq req) {

        return reviewScheduleService.generateDailyTasks(req.getAccountId());
    }

    /**
     * 获取今日复习任务列表
     */
    @Operation(summary = "获取今日复习任务列表")
    @PostMapping("/today")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public List<ReviewTaskResp> today() {

        return reviewScheduleService.todayTasks();
    }

    /**
     * 提交复习结果
     */
    @Operation(summary = "提交复习结果")
    @PostMapping("/submit")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public ReviewSubmitResp submit(@RequestBody @Validated @NotNull ReviewSubmitReq req) {

        return reviewScheduleService.submitReview(req);
    }

    /**
     * 跳过复习
     */
    @Operation(summary = "跳过复习")
    @PostMapping("/skip")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public void skip(@RequestBody @Validated @NotNull ReviewSkipReq req) {

        reviewScheduleService.skipReview(req);
    }

    /**
     * 获取今日复习进度
     */
    @Operation(summary = "获取今日复习进度")
    @PostMapping("/progress")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public ReviewProgressResp progress() {

        return reviewScheduleService.getProgress();
    }

    /**
     * 查询错题的复习历史记录
     */
    @Operation(summary = "查询复习历史")
    @PostMapping("/history")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public List<ReviewRecordResp> history(@RequestBody @Validated @NotNull ReviewHistoryReq req) {

        return reviewScheduleService.getReviewHistory(req);
    }

    /**
     * 管理员分页查询复习记录
     */
    @Operation(summary = "管理员查询复习记录")
    @PostMapping("/admin-records")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public Page<ReviewRecordAdminResp> adminRecords(@RequestBody @Validated @NotNull ReviewRecordAdminReq req) {

        return reviewRecordService.listPageForAdmin(req);
    }
}
