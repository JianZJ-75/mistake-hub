package com.jianzj.mistake.hub.common.base.config;

import com.jianzj.mistake.hub.common.base.ApplicationContextHolder;
import com.jianzj.mistake.hub.common.base.safe.FastJsonSafeMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * <p>
 * base 组件
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-20
 */
public class ApplicationBaseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "fastjson2.parser.safeMode", havingValue = "true")
    public FastJsonSafeMode fastJsonSafeMode() {
        return new FastJsonSafeMode();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationContextHolder applicationContextHolder() {
        return new ApplicationContextHolder();
    }
}
