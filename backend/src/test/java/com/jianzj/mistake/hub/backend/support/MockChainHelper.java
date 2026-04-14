package com.jianzj.mistake.hub.backend.support;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;

import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;

/**
 * <p>测试公共工具：构建 LambdaQueryChainWrapper mock</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
public final class MockChainHelper {

    private MockChainHelper() {

    }

    /**
     * 构建一个自返回的 LambdaQueryChainWrapper mock，所有链式方法自动返回 self
     */
    @SuppressWarnings("unchecked")
    public static <T> LambdaQueryChainWrapper<T> mockChain() {

        return mock(LambdaQueryChainWrapper.class, invocation -> {
            if (invocation.getMethod().getReturnType().isAssignableFrom(invocation.getMock().getClass())) {
                return invocation.getMock();
            }
            return RETURNS_DEFAULTS.answer(invocation);
        });
    }
}
