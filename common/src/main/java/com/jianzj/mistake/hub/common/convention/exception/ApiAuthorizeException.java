package com.jianzj.mistake.hub.common.convention.exception;

import org.apache.commons.lang3.tuple.Pair;

/**
 * <p>
 * 鉴权异常类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-20
 */
public class ApiAuthorizeException extends BaseException {

    /**
     * exception msg：xxx-fail(2)
     */
    public ApiAuthorizeException() {
        super(Pair.of("当前用户无权限", "The current user has no permission."));
    }
}
