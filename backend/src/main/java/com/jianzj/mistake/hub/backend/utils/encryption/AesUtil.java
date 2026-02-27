package com.jianzj.mistake.hub.backend.utils.encryption;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * <p>
 * AES 加解密 工具类
 * 加密：原文 --key--> 密文
 * 解密：密文 --key--> 原文
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
public class AesUtil {

    // 基础算法
    private static final String ALGORITHM = "AES";

    // 改为 CBC 模式（核心修改）
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    // CBC 模式 IV 固定为 16 字节（AES 块大小）
    private static final int IV_SIZE = 16;

    // AES 256位密钥长度（32字节）
    private static final int AES_256_KEY_SIZE = 32;

    // IV 和密文的分隔符（用非 Base64 字符，避免冲突）
    private static final String IV_SEPARATOR = "_";

    /**
     * 加密方法（入参：明文、原始密钥；出参：密文 Base64）
     */
    public static String encryptWithRawKey(String plainText, String rawKey) throws Exception {
        String key = generateKey(rawKey);
        // 生成随机 IV
        String iv = generateIv();
        // CBC 加密
        String cipherText = encrypt(plainText, key, iv);
        // IV 拼接在密文后（密文|IV），对外只返回这个拼接串，保持出参格式不变
        return cipherText + IV_SEPARATOR + iv;
    }

    /**
     * 解密方法（入参：密文、原始密钥；出参：明文）
     */
    public static String decryptWithRawKey(String cipherText, String rawKey) throws Exception {
        // 拆分密文和 IV
        String[] parts = splitCipherTextAndIv(cipherText);
        String realCipherText = parts[0];
        String iv = parts[1];

        String key = generateKey(rawKey);
        // CBC 解密
        return decrypt(realCipherText, key, iv);
    }

    /**
     * 加密方法（入参：明文、密钥；出参：密文 Base64）
     */
    public static String encrypt(String plainText, String key) throws Exception {
        // 生成随机 IV
        String iv = generateIv();
        // CBC 加密
        String cipherText = encrypt(plainText, key, iv);
        // 拼接 IV 返回，保持出参格式不变
        return cipherText + IV_SEPARATOR + iv;
    }

    /**
     * 解密方法（入参：密文、密钥；出参：明文）
     */
    public static String decrypt(String cipherText, String key) throws Exception {
        // 拆分密文和 IV
        String[] parts = splitCipherTextAndIv(cipherText);
        String realCipherText = parts[0];
        String iv = parts[1];
        // CBC 解密
        return decrypt(realCipherText, key, iv);
    }

    /**
     * CBC 模式加密
     */
    private static String encrypt(String plainText, String key, String iv) throws Exception {
        // 1. 校验密钥长度
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        validateAesKeyLength(keyBytes.length);

        // 2. 解析 IV
        byte[] ivBytes = Base64.getDecoder().decode(iv);
        if (ivBytes.length != IV_SIZE) {
            throw new IllegalArgumentException("CBC 模式 IV 必须是 16 字节");
        }

        // 3. 初始化加密器
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        IvParameterSpec ivParam = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParam);

        // 4. 加密并返回 Base64 结果
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * CBC 模式解密
     */
    private static String decrypt(String cipherText, String key, String iv) throws Exception {
        // 1. 校验密钥长度
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        validateAesKeyLength(keyBytes.length);

        // 2. 解析 IV
        byte[] ivBytes = Base64.getDecoder().decode(iv);
        if (ivBytes.length != IV_SIZE) {
            throw new IllegalArgumentException("CBC 模式 IV 必须是 16 字节");
        }

        // 3. 初始化解密器
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        IvParameterSpec ivParam = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParam);

        // 4. 解密
        byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 生成 AES 密钥（SHA256 加密后截取 32 字节）
     */
    public static String generateKey(String rawKey) {
        String key = Sha256Util.encrypt(rawKey);
        return key.substring(0, AES_256_KEY_SIZE);
    }

    /**
     * 生成随机 AES 密钥
     */
    public static String generateKey(int keySize) throws Exception {
        javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance(ALGORITHM);
        SecureRandom secureRandom = new SecureRandom();
        keyGenerator.init(keySize, secureRandom);
        javax.crypto.SecretKey secretKey = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    /**
     * 生成随机 IV（16 字节），返回 Base64 编码结果
     */
    private static String generateIv() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }

    /**
     * 拆分密文和 IV
     */
    private static String[] splitCipherTextAndIv(String cipherTextWithIv) {
        String[] parts = cipherTextWithIv.split(IV_SEPARATOR, 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("密文格式错误，缺少 IV 信息");
        }
        return parts;
    }

    /**
     * 校验 AES 密钥长度
     */
    private static void validateAesKeyLength(int keyLength) {
        if (keyLength != 16 && keyLength != 24 && keyLength != 32) {
            throw new IllegalArgumentException("AES 密钥长度必须是 16/24/32 字节（对应 128/192/256 位）");
        }
    }

    public static void main(String[] args) throws Exception {
        // 原有调用方式：完全不变
        String rawKey = "myRawKey123456";
        String plainText = "这是要加密的敏感数据：123456";

        // 1. 调用原有加密方法（入参出参和原来一致）
        String cipherText = AesUtil.encryptWithRawKey(plainText, rawKey);
        System.out.println("加密结果（和原ECB格式一致）：" + cipherText);

        // 2. 调用原有解密方法（入参出参和原来一致）
        String decryptText = AesUtil.decryptWithRawKey(cipherText, rawKey);
        System.out.println("解密结果：" + decryptText);

        // 3. 验证原有 generateKey 方法
        System.out.println("AES-128 随机密钥：" + AesUtil.generateKey(128));
        System.out.println("AES-256 随机密钥：" + AesUtil.generateKey(256));
    }
}
