package com.jianzj.mistake.hub.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * 授权检查属性
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth-check")
public class AuthCheckProperties {

    private List<String> excludePathList;
}
