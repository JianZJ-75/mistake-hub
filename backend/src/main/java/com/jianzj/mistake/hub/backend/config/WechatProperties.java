package com.jianzj.mistake.hub.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 微信小程序配置
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-27
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatProperties {

    private String appId;

    private String appSecret;
}
