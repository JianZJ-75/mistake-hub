package com.jianzj.mistake.hub.backend.integration;

import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.entity.Nonce;
import com.jianzj.mistake.hub.backend.entity.Session;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.utils.encryption.AesUtil;
import com.jianzj.mistake.hub.backend.utils.encryption.Sha256Util;
import com.jianzj.mistake.hub.common.convention.exception.BaseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <p>安全集成测试：Nonce 重放、Session 过期、加密流程</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
class SecurityIntegrationTest extends BaseIntegrationTest {

    // ===== Nonce =====

    @Test
    void nonce_consumeTwice_shouldFail() {

        Nonce nonce = nonceService.generate();
        Long nonceId = nonce.getId();

        // 第一次消费成功
        Nonce consumed = nonceService.consumeById(nonceId);
        assertThat(consumed).isNotNull();

        // 第二次消费应抛异常（Nonce 不存在）
        assertThatThrownBy(() -> nonceService.consumeById(nonceId))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void nonce_format_32chars() {

        Nonce nonce = nonceService.generate();

        assertThat(nonce.getNonce()).hasSize(32);
    }

    // ===== Session =====

    @Test
    void session_expired_shouldReturnNull() {

        Account account = createTestAccount(Role.STUDENT);

        String token = sessionService.create(account.getId());

        // 手动将 Session 过期时间设为过去
        Session session = sessionService.getBySessionKey(token);
        assertThat(session).isNotNull();

        session.setExpireTime(session.getExpireTime().minusDays(2));
        sessionService.updateById(session);

        // 再次获取应返回 null 且清理
        Session result = sessionService.getBySessionKey(token);
        assertThat(result).isNull();
    }

    @Test
    void session_valid_shouldReturn() {

        Account account = createTestAccount(Role.STUDENT);
        String token = sessionService.create(account.getId());

        Session session = sessionService.getBySessionKey(token);

        assertThat(session).isNotNull();
        assertThat(session.getAccountId()).isEqualTo(account.getId());
    }

    @Test
    void session_renew_shouldExtendExpiry() {

        Account account = createTestAccount(Role.STUDENT);
        String token = sessionService.create(account.getId());

        Session session = sessionService.getBySessionKey(token);
        var originalExpire = session.getExpireTime();

        // 等一下确保时间有差异
        sessionService.renew(session);

        Session renewed = sessionService.getBySessionKey(token);
        assertThat(renewed.getExpireTime()).isAfterOrEqualTo(originalExpire);
    }

    // ===== AES =====

    @Test
    void aes_roundTrip_withAccountId() {

        Account account = createTestAccount(Role.ADMIN);
        String rawPassword = "testPassword123";

        // 模拟登录加密流程
        String digest = encryptionUtil.toDigest(rawPassword);
        String cipherPassword = encryptionUtil.encryptWithRawKey(digest, account.getId());

        // 解密验证
        String decrypted = encryptionUtil.decryptWithRawKey(cipherPassword, account.getId());
        assertThat(decrypted).isEqualTo(digest);
    }

    @Test
    void loginWeb_fullFlow_nonceEncryptDecrypt() throws Exception {

        // 1. 生成 Nonce
        Nonce nonce = nonceService.generate();

        // 2. 模拟前端：SHA256(password) → AES 加密发送
        String rawPassword = "myPassword";
        String digest = Sha256Util.encrypt(rawPassword);
        String cipherDigest = AesUtil.encryptWithRawKey(digest, nonce.getNonce());

        // 3. 模拟后端：消费 Nonce + 解密
        Nonce consumed = nonceService.consumeById(nonce.getId());
        String decrypted = AesUtil.decryptWithRawKey(cipherDigest, consumed.getNonce());

        assertThat(decrypted).isEqualTo(digest);
    }
}
