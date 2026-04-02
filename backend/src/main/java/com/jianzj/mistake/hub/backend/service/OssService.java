package com.jianzj.mistake.hub.backend.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.jianzj.mistake.hub.backend.config.OssProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

/**
 * <p>
 * 阿里云 OSS 文件上传服务
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-04
 */
@Service
@Slf4j
public class OssService {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    private final OssProperties ossProperties;

    private final OSS ossClient;

    public OssService(OssProperties ossProperties) {

        this.ossProperties = ossProperties;
        this.ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
    }

    // ===== 业务方法 =====

    /**
     * 上传文件到 OSS，返回完整的访问 URL
     */
    public String upload(MultipartFile file) {

        validateFile(file);
        String extension = extractExtension(file.getOriginalFilename());

        String dateDir = LocalDate.now().toString();
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        String objectKey = "mistakes/" + dateDir + "/" + fileName;

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(ossProperties.getBucketName(), objectKey, inputStream);
        } catch (IOException e) {
            log.error("OSS 上传失败", e);
            oops("图片上传失败", "Failed to upload image.");
        }

        String endpoint = ossProperties.getEndpoint().replaceFirst("^https?://", "");
        return "https://" + ossProperties.getBucketName() + "." + endpoint + "/" + objectKey;
    }

    // ===== 工具方法 =====

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

    @PreDestroy
    public void destroy() {

        ossClient.shutdown();
    }
}
