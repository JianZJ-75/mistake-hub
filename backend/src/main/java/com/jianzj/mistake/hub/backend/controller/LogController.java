package com.jianzj.mistake.hub.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzj.mistake.hub.backend.annotation.PreAuthorize;
import com.jianzj.mistake.hub.backend.dto.req.LogListReq;
import com.jianzj.mistake.hub.backend.entity.OperationLog;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
