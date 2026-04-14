package com.jianzj.mistake.hub.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzj.mistake.hub.backend.annotation.PreAuthorize;
import com.jianzj.mistake.hub.backend.dto.req.LogListReq;
import com.jianzj.mistake.hub.backend.entity.OperationLog;
import com.jianzj.mistake.hub.backend.enums.OperationAction;
import com.jianzj.mistake.hub.backend.enums.OperationTargetType;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.service.OperationLogService;
import com.jianzj.mistake.hub.common.convention.enums.BasicEnumPojo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 操作日志 前端控制器
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/v1/log")
@Validated
@Slf4j
public class LogController {

    private final OperationLogService operationLogService;

    public LogController(OperationLogService operationLogService) {

        this.operationLogService = operationLogService;
    }

    /**
     * 分页查询操作日志
     */
    @Operation(summary = "分页查询操作日志")
    @PostMapping("/list")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public Page<OperationLog> list(@RequestBody @Validated @NotNull LogListReq req) {

        return operationLogService.listPage(req);
    }

    /**
     * 获取所有操作类型枚举
     */
    @Operation(summary = "获取操作类型枚举列表")
    @GetMapping("/action-types")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public List<BasicEnumPojo> actionTypes() {

        return OperationAction.listAll();
    }

    /**
     * 获取所有目标类型枚举
     */
    @Operation(summary = "获取目标类型枚举列表")
    @GetMapping("/target-types")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public List<BasicEnumPojo> targetTypes() {

        return OperationTargetType.listAll();
    }
}
