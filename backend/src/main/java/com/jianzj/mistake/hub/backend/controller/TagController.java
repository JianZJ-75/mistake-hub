package com.jianzj.mistake.hub.backend.controller;

import com.jianzj.mistake.hub.backend.annotation.PreAuthorize;
import com.jianzj.mistake.hub.backend.dto.req.TagAddReq;
import com.jianzj.mistake.hub.backend.dto.req.TagCustomAddReq;
import com.jianzj.mistake.hub.backend.dto.req.TagDeleteReq;
import com.jianzj.mistake.hub.backend.dto.req.TagUpdateReq;
import com.jianzj.mistake.hub.backend.dto.resp.TagResp;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
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
     * 查询全局标签树
     */
    @Operation(summary = "查询全局标签树")
    @GetMapping("/tree")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public List<TagResp> tree() {

        return tagService.tree();
    }

    /**
     * 新增全局标签
     */
    @Operation(summary = "新增全局标签")
    @PostMapping("/add")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public void add(@RequestBody @Validated @NotNull TagAddReq req) {

        tagService.add(req);
    }

    /**
     * 编辑全局标签
     */
    @Operation(summary = "编辑全局标签")
    @PostMapping("/modify")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public void modify(@RequestBody @Validated @NotNull TagUpdateReq req) {

        tagService.modify(req);
    }

    /**
     * 删除全局标签
     */
    @Operation(summary = "删除全局标签")
    @PostMapping("/delete")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public void delete(@RequestBody @Validated @NotNull TagDeleteReq req) {

        tagService.delete(req);
    }

    /**
     * 查询当前用户的自定义标签列表
     */
    @Operation(summary = "查询自定义标签列表")
    @GetMapping("/custom/list")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public List<TagResp> listCustom() {

        return tagService.listCustom();
    }

    /**
     * 查询所有自定义标签（管理员）
     */
    @Operation(summary = "查询所有自定义标签")
    @GetMapping("/custom/list/all")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public List<TagResp> listAllCustom() {

        return tagService.listAllCustom();
    }

    /**
     * 新增自定义标签
     */
    @Operation(summary = "新增自定义标签")
    @PostMapping("/custom/add")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public void addCustom(@RequestBody @Validated @NotNull TagCustomAddReq req) {

        tagService.addCustom(req);
    }

    /**
     * 删除自定义标签
     */
    @Operation(summary = "删除自定义标签")
    @PostMapping("/custom/delete")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public void deleteCustom(@RequestBody @Validated @NotNull TagDeleteReq req) {

        tagService.deleteCustom(req);
    }
}
