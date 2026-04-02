package com.jianzj.mistake.hub.backend.controller;

import com.jianzj.mistake.hub.backend.annotation.PreAuthorize;
import com.jianzj.mistake.hub.backend.dto.req.ReviewGenerateDailyReq;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewTaskResp;
import com.jianzj.mistake.hub.backend.enums.Role;
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

    public ReviewController(ReviewScheduleService reviewScheduleService) {

        this.reviewScheduleService = reviewScheduleService;
    }

    /**
     * 手动生成每日复习任务
     */
    @Operation(summary = "手动生成每日复习任务")
    @PostMapping("/generate-daily")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public List<ReviewTaskResp> generateDaily(@RequestBody @Validated @NotNull ReviewGenerateDailyReq req) {

        return reviewScheduleService.generateDailyTasks(req.getAccountId());
    }
}
