package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.jianzj.mistake.hub.backend.config.WechatProperties;
import com.jianzj.mistake.hub.backend.dto.req.AccountChangePasswordReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountChangeRoleReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountLoginWebReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountModifyProfileReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountResetPasswordReq;
import com.jianzj.mistake.hub.backend.dto.resp.AccountChangeRoleResp;
import com.jianzj.mistake.hub.backend.dto.resp.AccountDetailResp;
import com.jianzj.mistake.hub.backend.dto.resp.AccountResetPasswordResp;
import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.entity.Nonce;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.mapper.AccountMapper;
import com.jianzj.mistake.hub.backend.support.MockChainHelper;
import com.jianzj.mistake.hub.backend.utils.encryption.EncryptionUtil;
import com.jianzj.mistake.hub.common.convention.exception.BaseException;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * <p>AccountService 单元测试</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountMapper mockAccountMapper;

    @Mock
    private WechatProperties wechatProperties;

    @Mock
    private SessionService sessionService;

    @Mock
    private NonceService nonceService;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private ThreadStorageUtil threadStorageUtil;

    private AccountService accountService;

    @BeforeEach
    void setUp() {

        accountService = spy(new AccountService(
                wechatProperties, sessionService, nonceService,
                encryptionUtil, threadStorageUtil
        ));
        ReflectionTestUtils.setField(accountService, "baseMapper", mockAccountMapper);
    }

    // ===== loginWeb =====

    @Test
    void loginWeb_success() {

        Nonce nonce = Nonce.builder().id(1L).nonce("testNonce32chars1234567890ab").build();
        when(nonceService.consumeById(1L)).thenReturn(nonce);
        when(encryptionUtil.decryptWithRawKey("cipherDigest", "testNonce32chars1234567890ab")).thenReturn("sha256digest");

        Account account = Account.builder()
                .id(100L).code("admin001").role(Role.ADMIN.getCode())
                .cipherPassword("encryptedPassword").build();
        stubLambdaQueryChain(List.of(account));

        when(encryptionUtil.decryptWithRawKey("encryptedPassword", 100L)).thenReturn("sha256digest");
        when(sessionService.create(100L)).thenReturn("token123");

        AccountLoginWebReq req = new AccountLoginWebReq();
        req.setCode("admin001");
        req.setCipherDigest("cipherDigest");
        req.setNonceId(1L);

        String token = accountService.loginWeb(req);

        assertThat(token).isEqualTo("token123");
    }

    @Test
    void loginWeb_accountNotFound_shouldThrow() {

        Nonce nonce = Nonce.builder().id(1L).nonce("nonce").build();
        when(nonceService.consumeById(1L)).thenReturn(nonce);
        when(encryptionUtil.decryptWithRawKey(anyString(), anyString())).thenReturn("digest");

        stubLambdaQueryChain(List.of());

        AccountLoginWebReq req = new AccountLoginWebReq();
        req.setCode("notExist");
        req.setCipherDigest("cipher");
        req.setNonceId(1L);

        assertThatThrownBy(() -> accountService.loginWeb(req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void loginWeb_notAdmin_shouldThrow() {

        Nonce nonce = Nonce.builder().id(1L).nonce("nonce").build();
        when(nonceService.consumeById(1L)).thenReturn(nonce);
        when(encryptionUtil.decryptWithRawKey(anyString(), anyString())).thenReturn("digest");

        Account account = Account.builder()
                .id(100L).code("stu001").role(Role.STUDENT.getCode()).build();
        stubLambdaQueryChain(List.of(account));

        AccountLoginWebReq req = new AccountLoginWebReq();
        req.setCode("stu001");
        req.setCipherDigest("cipher");
        req.setNonceId(1L);

        assertThatThrownBy(() -> accountService.loginWeb(req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void loginWeb_noPassword_shouldThrow() {

        Nonce nonce = Nonce.builder().id(1L).nonce("nonce").build();
        when(nonceService.consumeById(1L)).thenReturn(nonce);
        when(encryptionUtil.decryptWithRawKey(anyString(), anyString())).thenReturn("digest");

        Account account = Account.builder()
                .id(100L).code("admin001").role(Role.ADMIN.getCode())
                .cipherPassword(null).build();
        stubLambdaQueryChain(List.of(account));

        AccountLoginWebReq req = new AccountLoginWebReq();
        req.setCode("admin001");
        req.setCipherDigest("cipher");
        req.setNonceId(1L);

        assertThatThrownBy(() -> accountService.loginWeb(req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void loginWeb_wrongPassword_shouldThrow() {

        Nonce nonce = Nonce.builder().id(1L).nonce("nonce").build();
        when(nonceService.consumeById(1L)).thenReturn(nonce);
        when(encryptionUtil.decryptWithRawKey("cipherDigest", "nonce")).thenReturn("userDigest");

        Account account = Account.builder()
                .id(100L).code("admin001").role(Role.ADMIN.getCode())
                .cipherPassword("encPwd").build();
        stubLambdaQueryChain(List.of(account));

        when(encryptionUtil.decryptWithRawKey("encPwd", 100L)).thenReturn("dbDigest");

        AccountLoginWebReq req = new AccountLoginWebReq();
        req.setCode("admin001");
        req.setCipherDigest("cipherDigest");
        req.setNonceId(1L);

        assertThatThrownBy(() -> accountService.loginWeb(req))
                .isInstanceOf(BaseException.class);
    }

    // ===== changeRole =====

    @Test
    void changeRole_toAdmin_shouldGeneratePassword() {

        Account account = Account.builder()
                .id(100L).code("stu001").role(Role.STUDENT.getCode()).build();
        doReturn(account).when(accountService).getById(100L);

        Nonce nonce = Nonce.builder().id(5L).nonce("nonceVal").build();
        when(nonceService.consumeById(5L)).thenReturn(nonce);

        when(encryptionUtil.generatePwd()).thenReturn("rawPwd");
        when(encryptionUtil.toDigest("rawPwd")).thenReturn("digest");
        when(encryptionUtil.encryptWithRawKey("digest", 100L)).thenReturn("cipherPwd");
        when(encryptionUtil.encrypt("rawPwd", "nonceVal")).thenReturn("encryptedForFrontend");
        doReturn(true).when(accountService).updateById(any(Account.class));

        AccountChangeRoleReq req = new AccountChangeRoleReq();
        req.setAccountId(100L);
        req.setRole(Role.ADMIN.getCode());
        req.setNonceId(5L);

        AccountChangeRoleResp resp = accountService.changeRole(req);

        assertThat(resp.getCipherPassword()).isEqualTo("encryptedForFrontend");
        assertThat(account.getRole()).isEqualTo(Role.ADMIN.getCode());
    }

    @Test
    void changeRole_toStudent_shouldClearPasswordAndSession() {

        Account account = Account.builder()
                .id(100L).code("admin001").role(Role.ADMIN.getCode())
                .cipherPassword("existingPwd").build();
        doReturn(account).when(accountService).getById(100L);
        doReturn(true).when(accountService).updateById(any(Account.class));

        AccountChangeRoleReq req = new AccountChangeRoleReq();
        req.setAccountId(100L);
        req.setRole(Role.STUDENT.getCode());

        accountService.changeRole(req);

        assertThat(account.getCipherPassword()).isNull();
        verify(sessionService).removeByAccountId(100L);
    }

    @Test
    void changeRole_sameRole_shouldThrow() {

        Account account = Account.builder()
                .id(100L).role(Role.ADMIN.getCode()).build();
        doReturn(account).when(accountService).getById(100L);

        AccountChangeRoleReq req = new AccountChangeRoleReq();
        req.setAccountId(100L);
        req.setRole(Role.ADMIN.getCode());

        assertThatThrownBy(() -> accountService.changeRole(req))
                .isInstanceOf(BaseException.class);
    }

    // ===== resetPassword =====

    @Test
    void resetPassword_success() {

        Account account = Account.builder()
                .id(100L).role(Role.ADMIN.getCode()).build();
        doReturn(account).when(accountService).getById(100L);

        when(encryptionUtil.generatePwd()).thenReturn("newRaw");
        when(encryptionUtil.toDigest("newRaw")).thenReturn("newDigest");
        when(encryptionUtil.encryptWithRawKey("newDigest", 100L)).thenReturn("newCipher");
        doReturn(true).when(accountService).updateById(any(Account.class));

        Nonce nonce = Nonce.builder().id(10L).nonce("nonceVal").build();
        when(nonceService.generate()).thenReturn(nonce);
        when(encryptionUtil.encrypt("newRaw", "nonceVal")).thenReturn("cipherForFrontend");

        AccountResetPasswordReq req = new AccountResetPasswordReq();
        req.setAccountId(100L);

        AccountResetPasswordResp resp = accountService.resetPassword(req);

        assertThat(resp.getNonceId()).isEqualTo(10L);
        assertThat(resp.getCipherPassword()).isEqualTo("cipherForFrontend");
        verify(sessionService).removeByAccountId(100L);
    }

    @Test
    void resetPassword_notAdmin_shouldThrow() {

        Account account = Account.builder()
                .id(100L).role(Role.STUDENT.getCode()).build();
        doReturn(account).when(accountService).getById(100L);

        AccountResetPasswordReq req = new AccountResetPasswordReq();
        req.setAccountId(100L);

        assertThatThrownBy(() -> accountService.resetPassword(req))
                .isInstanceOf(BaseException.class);
    }

    // ===== changePassword =====

    @Test
    void changePassword_success() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);

        Account account = Account.builder()
                .id(100L).cipherPassword("existingCipher").build();
        doReturn(account).when(accountService).getById(100L);

        Nonce oldNonce = Nonce.builder().id(1L).nonce("oldNonceVal").build();
        Nonce newNonce = Nonce.builder().id(2L).nonce("newNonceVal").build();
        when(nonceService.consumeById(1L)).thenReturn(oldNonce);
        when(nonceService.consumeById(2L)).thenReturn(newNonce);

        when(encryptionUtil.decryptWithRawKey("oldCipher", "oldNonceVal")).thenReturn("oldDigest");
        when(encryptionUtil.decryptWithRawKey("existingCipher", 100L)).thenReturn("oldDigest");
        when(encryptionUtil.decryptWithRawKey("newCipher", "newNonceVal")).thenReturn("newDigest");
        when(encryptionUtil.encryptWithRawKey("newDigest", 100L)).thenReturn("newCipherPassword");
        doReturn(true).when(accountService).updateById(any(Account.class));

        AccountChangePasswordReq req = new AccountChangePasswordReq();
        req.setOldCipherDigest("oldCipher");
        req.setOldNonceId(1L);
        req.setNewCipherDigest("newCipher");
        req.setNewNonceId(2L);

        accountService.changePassword(req);

        assertThat(account.getCipherPassword()).isEqualTo("newCipherPassword");
        verify(sessionService).removeByAccountId(100L);
    }

    @Test
    void changePassword_wrongOld_shouldThrow() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);

        Account account = Account.builder()
                .id(100L).cipherPassword("existingCipher").build();
        doReturn(account).when(accountService).getById(100L);

        Nonce oldNonce = Nonce.builder().id(1L).nonce("oldNonceVal").build();
        when(nonceService.consumeById(1L)).thenReturn(oldNonce);

        when(encryptionUtil.decryptWithRawKey("oldCipher", "oldNonceVal")).thenReturn("wrongDigest");
        when(encryptionUtil.decryptWithRawKey("existingCipher", 100L)).thenReturn("correctDigest");

        AccountChangePasswordReq req = new AccountChangePasswordReq();
        req.setOldCipherDigest("oldCipher");
        req.setOldNonceId(1L);
        req.setNewCipherDigest("newCipher");
        req.setNewNonceId(2L);

        assertThatThrownBy(() -> accountService.changePassword(req))
                .isInstanceOf(BaseException.class);
    }

    // ===== modifyProfile =====

    @Test
    void modifyProfile_nickname() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);

        Account account = Account.builder()
                .id(100L).nickname("旧昵称").avatarUrl("old.jpg").build();
        doReturn(account).when(accountService).getById(100L);
        doReturn(true).when(accountService).updateById(any(Account.class));

        AccountModifyProfileReq req = new AccountModifyProfileReq();
        req.setNickname("新昵称");

        accountService.modifyProfile(req);

        assertThat(account.getNickname()).isEqualTo("新昵称");
    }

    @Test
    void modifyProfile_avatarEmpty_shouldClear() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);

        Account account = Account.builder()
                .id(100L).nickname("用户").avatarUrl("old.jpg").build();
        doReturn(account).when(accountService).getById(100L);
        doReturn(true).when(accountService).updateById(any(Account.class));

        AccountModifyProfileReq req = new AccountModifyProfileReq();
        req.setAvatarUrl("");

        accountService.modifyProfile(req);

        assertThat(account.getAvatarUrl()).isNull();
    }

    // ===== currentDetail =====

    @Test
    void currentDetail_success() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);

        Account account = Account.builder()
                .id(100L).code("user001").nickname("测试用户")
                .role(Role.STUDENT.getCode()).build();
        doReturn(account).when(accountService).getById(100L);

        AccountDetailResp resp = accountService.currentDetail();

        assertThat(resp.getCode()).isEqualTo("user001");
        assertThat(resp.getNickname()).isEqualTo("测试用户");
    }

    // ===== 工具方法 =====

    private void stubLambdaQueryChain(List<Account> result) {

        LambdaQueryChainWrapper<Account> chain = MockChainHelper.mockChain();
        doReturn(chain).when(accountService).lambdaQuery();
        when(chain.list()).thenReturn(result);
    }
}
