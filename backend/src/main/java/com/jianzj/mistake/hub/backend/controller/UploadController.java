package com.jianzj.mistake.hub.backend.controller;

import com.jianzj.mistake.hub.backend.annotation.PreAuthorize;
import com.jianzj.mistake.hub.backend.config.UploadProperties;
import com.jianzj.mistake.hub.backend.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

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

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    private final UploadProperties uploadProperties;

    public UploadController(UploadProperties uploadProperties) {

        this.uploadProperties = uploadProperties;
    }

    /**
     * 上传图片，返回可访问的图片URL
     */
    @Operation(summary = "上传图片")
    @PostMapping("/image")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public String image(@RequestParam("file") MultipartFile file) {

        validateFile(file);

        String dateDir = LocalDate.now().toString();
        String extension = extractExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

        File targetDir = new File(uploadProperties.getDir() + File.separator + dateDir);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            oops("创建上传目录失败", "Failed to create upload directory.");
        }

        File target = new File(targetDir, fileName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            oops("图片上传失败", "Failed to upload image.");
        }

        return uploadProperties.getUrlPrefix() + "/" + dateDir + "/" + fileName;
    }

    /**
     * 校验文件类型和大小
     */
    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            oops("上传文件不能为空", "Upload file is empty.");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            oops("图片大小不能超过10MB", "Image size must not exceed 10MB.");
        }

        String contentType = file.getContentType();
        for (String allowed : ALLOWED_TYPES) {
            if (allowed.equals(contentType)) {
                return;
            }
        }

        oops("不支持的图片格式，仅支持 JPG/PNG/GIF/WEBP", "Unsupported image format.");
    }

    /**
     * 从原始文件名中提取扩展名
     */
    private String extractExtension(String originalFilename) {

        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".jpg";
        }

        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}
