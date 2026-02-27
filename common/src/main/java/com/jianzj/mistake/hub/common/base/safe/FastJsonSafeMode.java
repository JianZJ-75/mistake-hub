package com.jianzj.mistake.hub.common.base.safe;

import org.springframework.beans.factory.InitializingBean;

/**
 * <p>
 * 开启 fastjson 的安全模式
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-20
 */
public class FastJsonSafeMode implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        System.setProperty("fastjson2.parser.safeMode", "true");
    }
}
