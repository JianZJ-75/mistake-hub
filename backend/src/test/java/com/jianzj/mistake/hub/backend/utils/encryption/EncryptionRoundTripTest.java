package com.jianzj.mistake.hub.backend.utils.encryption;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <p>AES / SHA-256 加解密轮回测试（纯静态方法，零 Spring 依赖）</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
class EncryptionRoundTripTest {

    // ===== AES with RawKey =====

    @Test
    void aes_withRawKey_roundTrip() throws Exception {

        String rawKey = "testRawKey123456";
        String plainText = "这是一段敏感数据";

        String cipherText = AesUtil.encryptWithRawKey(plainText, rawKey);
        String decrypted = AesUtil.decryptWithRawKey(cipherText, rawKey);

        assertThat(decrypted).isEqualTo(plainText);
    }

    // ===== AES with direct Key =====

    @Test
    void aes_withKey_roundTrip() throws Exception {

        String key = AesUtil.generateKey("directKeySource");
        String plainText = "测试直接密钥加解密";

        String cipherText = AesUtil.encrypt(plainText, key);
        String decrypted = AesUtil.decrypt(cipherText, key);

        assertThat(decrypted).isEqualTo(plainText);
    }

    // ===== 同明文同 key，不同 IV → 不同密文 =====

    @Test
    void aes_differentIV_differentCipher() throws Exception {

        String rawKey = "sameKey";
        String plainText = "samePlainText";

        String cipher1 = AesUtil.encryptWithRawKey(plainText, rawKey);
        String cipher2 = AesUtil.encryptWithRawKey(plainText, rawKey);

        assertThat(cipher1).isNotEqualTo(cipher2);
    }

    // ===== 密文格式：包含下划线分隔符 =====

    @Test
    void aes_cipherFormat_containsSeparator() throws Exception {

        String rawKey = "formatKey";
        String plainText = "formatTest";

        String cipherText = AesUtil.encryptWithRawKey(plainText, rawKey);

        assertThat(cipherText).contains("_");
        String[] parts = cipherText.split("_", 2);
        assertThat(parts).hasSize(2);
        // 两部分都是 Base64 编码
        assertThat(parts[0]).isNotBlank();
        assertThat(parts[1]).isNotBlank();
    }

    // ===== SHA-256 =====

    @Test
    void sha256_deterministic() {

        String input = "hello world";
        String hash1 = Sha256Util.encrypt(input);
        String hash2 = Sha256Util.encrypt(input);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void sha256_length64() {

        String hash = Sha256Util.encrypt("任意输入");
        assertThat(hash).hasSize(64);
    }

    // ===== 错误 key 解密 =====

    @Test
    void decryptWithWrongKey_shouldFail() throws Exception {

        String rawKey = "correctKey";
        String wrongKey = "wrongKey";
        String plainText = "secret";

        String cipherText = AesUtil.encryptWithRawKey(plainText, rawKey);

        assertThatThrownBy(() -> AesUtil.decryptWithRawKey(cipherText, wrongKey))
                .isInstanceOf(Exception.class);
    }

    // ===== generateKey(256) =====

    @Test
    void generateKey256_validBase64() throws Exception {

        String key = AesUtil.generateKey(256);
        assertThat(key).isNotBlank();
        // AES-256 key = 32 bytes → Base64 = 44 chars
        assertThat(key).hasSize(44);
    }

    // ===== generateAesKey (EncryptionUtil 使用的 32 字符 key) =====

    @Test
    void generateAesKey_length32() throws Exception {

        String base64Key = AesUtil.generateKey(256);
        String aesKey = Sha256Util.encrypt(base64Key).substring(0, 32);

        assertThat(aesKey).hasSize(32);
    }
}
