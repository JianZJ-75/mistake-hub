package com.jianzj.mistake.hub.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 图片上传配置属性
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-03
 */
@Data
@Component
@ConfigurationProperties(prefix = "upload")
public class UploadProperties {

    /**
     * 本地存储目录，绝对路径
     */
    private String dir;

    /**
     * 访问URL前缀，用于拼接返回给前端的图片地址
     */
    private String urlPrefix;
}
