package com.jianzj.mistake.hub.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * <p>
 * thread local 存储信息
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Component
public class ThreadStorageUtil {

    /**
     * 线程中的 account id
     */
    private final ThreadLocal<Long> accountIdStorage = new ThreadLocal<>();

    /**
     * 线程中的 account code
     */
    private final ThreadLocal<String> accountCodeStorage = new ThreadLocal<>();

    /***********************
     * account id
     ***********************/

    public void setCurAccountId(Long accountId) {
        accountIdStorage.set(accountId);
    }

    public Long getCurAccountId() {
        Long id = accountIdStorage.get();
        if (id == null) {
            throw new RuntimeException("thread storage account id is empty");
        }
        return id;
    }

    private void removeCurAccountId() {
        accountIdStorage.remove();
    }

    /***********************
     * account code
     ***********************/

    public void setCurAccountCode(String accountCode) {
        accountCodeStorage.set(accountCode);
    }

    public String getCurAccountCode() {
        String code = accountCodeStorage.get();
        if (StringUtils.isBlank(code)) {
            throw new RuntimeException("thread storage account code is empty");
        }
        return code;
    }

    private void removeCurAccountCode() {
        accountCodeStorage.remove();
    }

    /***********************
     * 清空
     ***********************/

    public void clear() {
        removeCurAccountId();
        removeCurAccountCode();
    }
}
