package com.jianzj.mistake.hub.common.convention;

import com.jianzj.mistake.hub.common.convention.result.BaseResult;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * <p>
 * 全局返回值包装
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-20
 */
@ControllerAdvice(basePackages = "com.jianzj.mistake.hub")
public class GlobalResultWrapper implements ResponseBodyAdvice<Object> {

    /**
     * 判断是否需要包装（核心过滤逻辑）
     * 返回 true = 包装，false = 不包装
     */
    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return !returnType.getParameterType().equals(BaseResult.class);
    }

    /**
     * 统一包装
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        return BaseResult.success(body);
    }
}
