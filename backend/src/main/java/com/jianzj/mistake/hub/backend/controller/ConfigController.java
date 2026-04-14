package com.jianzj.mistake.hub.backend.controller;

import com.jianzj.mistake.hub.backend.annotation.OperationLogAnno;
import com.jianzj.mistake.hub.backend.annotation.PreAuthorize;
import com.jianzj.mistake.hub.backend.dto.req.ConfigUpdateReq;
import com.jianzj.mistake.hub.backend.entity.SystemConfig;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.service.SystemConfigService;
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
 * 系统配置 前端控制器
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Tag(name = "系统配置")
@RestController
@RequestMapping("/v1/config")
@Validated
@Slf4j
public class ConfigController {

    private final SystemConfigService systemConfigService;

    public ConfigController(SystemConfigService systemConfigService) {

        this.systemConfigService = systemConfigService;
    }

    /**
     * 查询所有配置项
     */
    @Operation(summary = "查询所有配置项")
    @PostMapping("/list")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public List<SystemConfig> list() {

        return systemConfigService.listAll();
    }

    /**
     * 更新配置
     */
    @Operation(summary = "更新配置")
    @PostMapping("/update")
    @PreAuthorize(requiredRole = Role.ADMIN)
    @OperationLogAnno(action = "config/update", targetType = "CONFIG")
    public void update(@RequestBody @Validated @NotNull ConfigUpdateReq req) {

        systemConfigService.updateConfig(req);
    }
}
