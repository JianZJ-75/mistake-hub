package com.jianzj.mistake.hub.backend.controller;

import com.jianzj.mistake.hub.backend.annotation.PreAuthorize;
import com.jianzj.mistake.hub.backend.dto.req.TagAddReq;
import com.jianzj.mistake.hub.backend.dto.resp.TagResp;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * 标签 前端控制器
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Tag(name = "标签")
@RestController
@RequestMapping("/v1/tag")
@Validated
@Slf4j
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {

        this.tagService = tagService;
    }

    /**
     * 查询标签树
     */
    @Operation(summary = "查询标签树")
    @GetMapping("/tree")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public List<TagResp> tree() {

        return tagService.tree();
    }

    /**
     * 新增标签
     */
    @Operation(summary = "新增标签")
    @PostMapping("/add")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public void add(@RequestBody @Valid TagAddReq req) {

        tagService.add(req);
    }
}
