package com.jianzj.mistake.hub.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzj.mistake.hub.backend.annotation.OperationLogAnno;
import com.jianzj.mistake.hub.backend.annotation.PreAuthorize;
import com.jianzj.mistake.hub.backend.dto.req.MistakeAddReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeAdminListReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeDeleteReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeDetailReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeUpdateReq;
import com.jianzj.mistake.hub.backend.dto.resp.MistakeDetailResp;
import com.jianzj.mistake.hub.backend.enums.OperationAction;
import com.jianzj.mistake.hub.backend.enums.OperationTargetType;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.manager.MistakeManager;
import com.jianzj.mistake.hub.backend.service.MistakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 错题 前端控制器
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Tag(name = "错题")
@RestController
@RequestMapping("/v1/mistake")
@Validated
@Slf4j
public class MistakeController {

    private final MistakeService mistakeService;

    private final MistakeManager mistakeManager;

    public MistakeController(MistakeService mistakeService,
                             MistakeManager mistakeManager) {

        this.mistakeService = mistakeService;
        this.mistakeManager = mistakeManager;
    }

    /**
     * 录入错题
     */
    @Operation(summary = "录入错题")
    @PostMapping("/add")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public void add(@RequestBody @Validated @NotNull MistakeAddReq req) {

        mistakeManager.add(req);
    }

    /**
     * 分页查询错题列表
     */
    @Operation(summary = "分页查询错题列表")
    @GetMapping("/list")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public Page<MistakeDetailResp> list(@RequestParam(value = "accountId", required = false) Long accountId,
                                        @RequestParam(value = "tagIds", required = false) String tagIds,
                                        @RequestParam(value = "masteryFilter", required = false) @Min(0) @Max(4) Integer masteryFilter,
                                        @RequestParam(value = "masteryGroup", required = false) @Min(0) @Max(1) Integer masteryGroup,
                                        @RequestParam(value = "pageNum") @NotNull @Min(1) Long pageNum,
                                        @RequestParam(value = "pageSize") @NotNull @Min(1) @Max(100) Long pageSize) {

        return mistakeService.listPage(accountId, tagIds, masteryFilter, masteryGroup, pageNum, pageSize);
    }

    /**
     * 管理员错题列表（全量视图）
     */
    @Operation(summary = "管理员错题列表")
    @PostMapping("/admin-list")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public Page<MistakeDetailResp> adminList(@RequestBody @Validated @NotNull MistakeAdminListReq req) {

        return mistakeService.adminListPage(req);
    }

    /**
     * 查询错题详情
     */
    @Operation(summary = "查询错题详情")
    @PostMapping("/detail")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public MistakeDetailResp detail(@RequestBody @Validated @NotNull MistakeDetailReq req) {

        return mistakeService.detail(req);
    }

    /**
     * 编辑错题
     */
    @Operation(summary = "编辑错题")
    @PostMapping("/modify")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public void modify(@RequestBody @Validated @NotNull MistakeUpdateReq req) {

        mistakeService.modify(req);
    }

    /**
     * 管理员编辑错题
     */
    @Operation(summary = "管理员编辑错题")
    @PostMapping("/admin-update")
    @PreAuthorize(requiredRole = Role.ADMIN)
    @OperationLogAnno(action = OperationAction.MISTAKE_ADMIN_UPDATE, targetType = OperationTargetType.MISTAKE)
    public void adminUpdate(@RequestBody @Validated @NotNull MistakeUpdateReq req) {

        mistakeService.adminModify(req);
    }

    /**
     * 删除错题
     */
    @Operation(summary = "删除错题")
    @PostMapping("/delete")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public void delete(@RequestBody @Validated @NotNull MistakeDeleteReq req) {

        mistakeManager.delete(req);
    }
}
