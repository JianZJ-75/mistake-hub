package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.jianzj.mistake.hub.backend.entity.Session;
import com.jianzj.mistake.hub.backend.mapper.SessionMapper;
import com.jianzj.mistake.hub.backend.support.MockChainHelper;
import com.jianzj.mistake.hub.backend.utils.encryption.EncryptionUtil;
import com.jianzj.mistake.hub.common.convention.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <p>SessionService 单元测试</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionMapper mockSessionMapper;

    @Mock
    private EncryptionUtil encryptionUtil;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {

        sessionService = spy(new SessionService(encryptionUtil));
        ReflectionTestUtils.setField(sessionService, "baseMapper", mockSessionMapper);
    }

    // ===== create =====

    @Test
    void create_success() {

        when(encryptionUtil.generateKey()).thenReturn("generatedToken123");
        doReturn(true).when(sessionService).save(any(Session.class));

        String token = sessionService.create(100L);

        assertThat(token).isEqualTo("generatedToken123");
    }

    @Test
    void create_saveFails_shouldThrow() {

        when(encryptionUtil.generateKey()).thenReturn("token");
        doReturn(false).when(sessionService).save(any(Session.class));

        assertThatThrownBy(() -> sessionService.create(100L))
                .isInstanceOf(BaseException.class);
    }

    // ===== getBySessionKey =====

    @Test
    void getBySessionKey_valid_shouldReturn() {

        Session session = Session.builder()
                .id(1L).accountId(100L).token("validToken")
                .expireTime(LocalDateTime.now().plusHours(12))
                .build();

        stubLambdaQueryChain(List.of(session));

        Session result = sessionService.getBySessionKey("validToken");

        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo(100L);
    }

    @Test
    void getBySessionKey_expired_shouldReturnNullAndClean() {

        Session session = Session.builder()
                .id(1L).accountId(100L).token("expiredToken")
                .expireTime(LocalDateTime.now().minusHours(1))
                .build();

        stubLambdaQueryChain(List.of(session));
        doReturn(true).when(sessionService).removeById(1L);

        Session result = sessionService.getBySessionKey("expiredToken");

        assertThat(result).isNull();
        verify(sessionService).removeById(1L);
    }

    @Test
    void getBySessionKey_notExist_shouldReturnNull() {

        stubLambdaQueryChain(List.of());

        Session result = sessionService.getBySessionKey("noSuchToken");

        assertThat(result).isNull();
    }

    // ===== renew =====

    @Test
    void renew_shouldExtendExpiry() {

        Session session = Session.builder()
                .id(1L).accountId(100L).token("token")
                .expireTime(LocalDateTime.now().plusHours(1))
                .build();

        doReturn(true).when(sessionService).updateById(any(Session.class));

        LocalDateTime before = LocalDateTime.now();
        sessionService.renew(session);

        assertThat(session.getExpireTime()).isAfter(before.plusHours(23));
    }

    // ===== removeByAccountId =====

    @Test
    @SuppressWarnings("unchecked")
    void removeByAccountId_shouldCallLambdaUpdate() {

        LambdaUpdateChainWrapper<Session> updateChain = mock(LambdaUpdateChainWrapper.class, invocation -> {
            if (invocation.getMethod().getReturnType().isAssignableFrom(invocation.getMock().getClass())) {
                return invocation.getMock();
            }
            return RETURNS_DEFAULTS.answer(invocation);
        });
        doReturn(updateChain).when(sessionService).lambdaUpdate();
        when(updateChain.remove()).thenReturn(true);

        sessionService.removeByAccountId(100L);

        verify(updateChain).remove();
    }

    // ===== 工具方法 =====

    private void stubLambdaQueryChain(List<Session> result) {

        LambdaQueryChainWrapper<Session> chain = MockChainHelper.mockChain();
        doReturn(chain).when(sessionService).lambdaQuery();
        when(chain.list()).thenReturn(result);
    }
}
