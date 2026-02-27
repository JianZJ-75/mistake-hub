package com.jianzj.mistake.hub.common.convention.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * <p>
 * 抽象响应体
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-20
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseResult<T> {

    private int code;
    private Map<String, String> message;
    private T data;

    public static <T> BaseResult<T> success(T data) {
        return BaseResult.<T>builder()
                .code(0)
                .message(Map.of(
                        "zh_CN", "success",
                        "en_US", "success"
                ))
                .data(data)
                .build();
    }

    public static <T> BaseResult<T> error(int code, String zhCnMessage, String enUsMessage) {
        return BaseResult.<T>builder()
                .code(code)
                .message(Map.of(
                        "zh_CN", StringUtils.isBlank(zhCnMessage) ? "" : zhCnMessage,
                        "en_US", StringUtils.isBlank(enUsMessage) ? "" : enUsMessage
                ))
                .build();
    }
}
