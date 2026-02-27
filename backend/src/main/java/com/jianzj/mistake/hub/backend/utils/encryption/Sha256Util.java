package com.jianzj.mistake.hub.backend.utils.encryption;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * <p>
 * SHA-256 加解密 工具类
 * 字符串 --> digest
 * 单向的，不可逆
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
public class Sha256Util {

    /**
     * 加密方法，接收一个字符串作为输入，返回加密后的十六进制字符串
     * 生成长度为64的：432345abcde23434343252345abcdeabcdeabcdeabcdeabcdeabcdeabcdeabcd
     */
    public static String encrypt(String input) {
        try {
            // 获取SHA-256算法的MessageDigest实例
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 将输入字符串转换为字节数组
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // 创建一个StringBuilder来构建十六进制字符串
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                // 将字节转换为十六进制字符串
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            // 处理算法不存在的异常
            throw new RuntimeException(e);
        }
    }
}
