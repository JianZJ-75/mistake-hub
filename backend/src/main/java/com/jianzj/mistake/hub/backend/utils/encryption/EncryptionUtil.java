package com.jianzj.mistake.hub.backend.utils.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 加解密 工具类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Component
@Slf4j
public class EncryptionUtil {

    /**
     * SHA-256 生成 digest
     * 生成长度为64的：432345abcde23434343252345abcdeabcdeabcdeabcdeabcdeabcdeabcdeabcd
     */
    public String toDigest(String input) {

        return Sha256Util.encrypt(input);
    }

    /**
     * 用 key 加密
     */
    public String encrypt(String plainText, String key) {

        try {
            return AesUtil.encrypt(plainText, key);
        } catch (Exception e) {
            log.error("fail to encrypt, plainText: {}, key: {}, error: ", plainText, key, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 用 key 加密
     */
    public String encryptWithRawKey(String plainText, String key) {

        try {
            return AesUtil.encryptWithRawKey(plainText, key);
        } catch (Exception e) {
            log.error("fail to encrypt, plainText: {}, key: {}, error: ", plainText, key, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 用 key 加密
     */
    public String encryptWithRawKey(String plainText, Long key) {

        return encryptWithRawKey(plainText, String.valueOf(key));
    }


    /**
     * 用 key 解密
     */
    public String decrypt(String cipherText, String key) {

        try {
            return AesUtil.decrypt(cipherText, key);
        } catch (Exception e) {
            log.error("fail to encrypt, cipherText: {}, key: {}, error: ", cipherText, key, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 用 key 解密
     */
    public String decryptWithRawKey(String cipherText, String key) {

        try {
            return AesUtil.decryptWithRawKey(cipherText, key);
        } catch (Exception e) {
            log.error("fail to encrypt, cipherText: {}, key: {}, error: ", cipherText, key, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 用 key 解密
     */
    public String decryptWithRawKey(String cipherText, Long key) {

        return decryptWithRawKey(cipherText, String.valueOf(key));
    }

    /**
     * 生成一个随机 key，长度 44
     *
     * 7vFyxx9S8JHrrT6MWFLakI7h1DGu17ngFC3Yvf6Da7A=
     */
    public String generateKey() {

        try {
            return AesUtil.generateKey(256);
        } catch (Exception e) {
            log.error("fail to generateKey, error: ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成一个随机密码
     *
     * m2FZwsJBCXcCvqiQw3oyCg==
     * aLEjPuaL8Bo+DGfGyodLVQ==
     * l7/HtazPanPAamkVHPeKPg==
     */
    public String generatePwd() {

        try {
            return AesUtil.generateKey(128);
        } catch (Exception e) {
            log.error("fail to generateKey, error: ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成 AES 可用的 key
     * 432345abcde23434343252345abcdeab
     */
    public String generateAesKey() {

        String rawKey = generateKey();
        String key = Sha256Util.encrypt(rawKey);
        return key.substring(0, 32);
    }
}
