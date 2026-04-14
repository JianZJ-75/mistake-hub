package com.jianzj.mistake.hub.backend.service;

import com.jianzj.mistake.hub.backend.entity.Nonce;
import com.jianzj.mistake.hub.backend.mapper.NonceMapper;
import com.jianzj.mistake.hub.backend.utils.encryption.EncryptionUtil;
import com.jianzj.mistake.hub.common.convention.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <p>NonceService 单元测试</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@ExtendWith(MockitoExtension.class)
class NonceServiceTest {

    @Mock
    private NonceMapper mockNonceMapper;

    @Mock
    private EncryptionUtil encryptionUtil;

    private NonceService nonceService;

    @BeforeEach
    void setUp() {

        nonceService = spy(new NonceService(encryptionUtil));
        ReflectionTestUtils.setField(nonceService, "baseMapper", mockNonceMapper);
    }

    // ===== generate =====

    @Test
    void generate_success() {

        when(encryptionUtil.generateAesKey()).thenReturn("aesKey32charsLongValue1234567890");
        doReturn(true).when(nonceService).save(any(Nonce.class));

        Nonce nonce = nonceService.generate();

        assertThat(nonce.getNonce()).isEqualTo("aesKey32charsLongValue1234567890");
        verify(nonceService).save(any(Nonce.class));
    }

    @Test
    void generate_saveFails_shouldThrow() {

        when(encryptionUtil.generateAesKey()).thenReturn("key");
        doReturn(false).when(nonceService).save(any(Nonce.class));

        assertThatThrownBy(() -> nonceService.generate())
                .isInstanceOf(BaseException.class);
    }

    // ===== consumeById =====

    @Test
    void consumeById_success() {

        Nonce nonce = Nonce.builder().id(1L).nonce("nonceVal").build();
        doReturn(nonce).when(nonceService).getById(1L);
        doReturn(true).when(nonceService).removeById(1L);

        Nonce result = nonceService.consumeById(1L);

        assertThat(result.getNonce()).isEqualTo("nonceVal");
        verify(nonceService).removeById(1L);
    }

    @Test
    void consumeById_notExist_shouldThrow() {

        doReturn(null).when(nonceService).getById(999L);

        assertThatThrownBy(() -> nonceService.consumeById(999L))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void consumeById_removeFails_shouldThrow() {

        Nonce nonce = Nonce.builder().id(1L).nonce("val").build();
        doReturn(nonce).when(nonceService).getById(1L);
        doReturn(false).when(nonceService).removeById(1L);

        assertThatThrownBy(() -> nonceService.consumeById(1L))
                .isInstanceOf(BaseException.class);
    }
}
