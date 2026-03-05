package com.jianzj.mistake.hub.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzj.mistake.hub.backend.annotation.PreAuthorize;
import com.jianzj.mistake.hub.backend.dto.req.MistakeAddReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeDeleteReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeDetailReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeListReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeUpdateReq;
import com.jianzj.mistake.hub.backend.dto.resp.MistakeDetailResp;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.service.MistakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public MistakeController(MistakeService mistakeService) {

        this.mistakeService = mistakeService;
    }

    /**
     * 录入错题
     */
    @Operation(summary = "录入错题")
    @PostMapping("/add")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public void add(@RequestBody @Valid MistakeAddReq req) {

        mistakeService.add(req);
    }

    /**
     * 分页查询错题列表
     */
    @Operation(summary = "分页查询错题列表")
    @PostMapping("/list")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public Page<MistakeDetailResp> list(@RequestBody @Valid MistakeListReq req) {

        return mistakeService.listPage(req);
    }

    /**
     * 查询错题详情
     */
    @Operation(summary = "查询错题详情")
    @PostMapping("/detail")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public MistakeDetailResp detail(@RequestBody @Valid MistakeDetailReq req) {

        return mistakeService.detail(req);
    }

    /**
     * 编辑错题
     */
    @Operation(summary = "编辑错题")
    @PostMapping("/update")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public void update(@RequestBody @Valid MistakeUpdateReq req) {

        mistakeService.update(req);
    }

    /**
     * 删除错题
     */
    @Operation(summary = "删除错题")
    @PostMapping("/delete")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public void delete(@RequestBody @Valid MistakeDeleteReq req) {

        mistakeService.delete(req);
    }
}
