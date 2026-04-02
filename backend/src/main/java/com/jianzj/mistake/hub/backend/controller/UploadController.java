package com.jianzj.mistake.hub.backend.controller;

import com.jianzj.mistake.hub.backend.annotation.PreAuthorize;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 文件上传 前端控制器
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Tag(name = "文件上传")
@RestController
@RequestMapping("/v1/upload")
@Validated
@Slf4j
public class UploadController {

    private final OssService ossService;

    public UploadController(OssService ossService) {

        this.ossService = ossService;
    }

    /**
     * 上传图片，返回可访问的图片URL
     */
    @Operation(summary = "上传图片")
    @PostMapping("/image")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public String image(@RequestParam("file") MultipartFile file) {

        return ossService.upload(file);
    }
}
