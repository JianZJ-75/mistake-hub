package com.jianzj.mistake.hub.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 阿里云 OSS 配置
 * </p>
 *
 * @author jian.zhong
 * @since 2026-03-04
 */
@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    /** OSS 服务地域节点，如 oss-cn-hangzhou.aliyuncs.com（可带或不带 https:// 前缀） */
    private String endpoint;

    /** 阿里云 AccessKey ID，用于 OSS API 鉴权 */
    private String accessKeyId;

    /** 阿里云 AccessKey Secret，用于 OSS API 签名 */
    private String accessKeySecret;

    /** OSS Bucket 名称，存放上传的错题图片 */
    private String bucketName;
}
