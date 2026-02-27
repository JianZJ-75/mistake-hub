package com.jianzj.mistake.hub.common.convention.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.IllegalFormatException;

/**
 * <p>
 * 抽象异常类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-20
 */
@Getter
@Slf4j
public class BaseException extends RuntimeException {

    private int code;

    /**
     * 中文msg，英文msg
     */
    private Pair<String, String> finalMessage;

    /**
     * exception msg：xxxxx(-1)
     */
    public BaseException(Pair<String, String> message) {
        super(message.getRight() + "(-1)");
        code = -1;
        this.finalMessage = message;
    }

    /**
     * 快速抛错
     */
    public static void oops(String zhCnFormat, String enUsFormat, Object... args) {
        try {
            throw new BaseException(Pair.of(
                    String.format(zhCnFormat, args), String.format(enUsFormat, args)
            ));
        } catch (IllegalFormatException e) {
            log.error("fail to do string format, error is: ", e);
        }
        throw new BaseException(Pair.of(zhCnFormat, enUsFormat));
    }
}
