package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.jianzj.mistake.hub.backend.dto.req.ConfigUpdateReq;
import com.jianzj.mistake.hub.backend.entity.SystemConfig;
import com.jianzj.mistake.hub.backend.mapper.SystemConfigMapper;
import com.jianzj.mistake.hub.backend.support.MockChainHelper;
import com.jianzj.mistake.hub.common.convention.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * <p>SystemConfigService 单元测试</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SystemConfigMapper mockSystemConfigMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<String> bucket;

    private SystemConfigService systemConfigService;

    @BeforeEach
    void setUp() {

        systemConfigService = spy(new SystemConfigService(redissonClient));
        ReflectionTestUtils.setField(systemConfigService, "baseMapper", mockSystemConfigMapper);
    }

    // ===== getByKey =====

    @Test
    void getByKey_cacheHit_shouldNotQueryDB() {

        when(redissonClient.<String>getBucket("config:review.intervals")).thenReturn(bucket);
        when(bucket.get()).thenReturn("0,1,2,4,7,15,30");

        String result = systemConfigService.getByKey("review.intervals");

        assertThat(result).isEqualTo("0,1,2,4,7,15,30");
        verify(systemConfigService, never()).lambdaQuery();
    }

    @Test
    void getByKey_cacheMiss_shouldQueryDBAndWriteCache() {

        when(redissonClient.<String>getBucket("config:test.key")).thenReturn(bucket);
        when(bucket.get()).thenReturn(null);

        SystemConfig config = new SystemConfig();
        config.setConfigKey("test.key");
        config.setConfigValue("testValue");

        stubLambdaQueryChain(List.of(config));

        String result = systemConfigService.getByKey("test.key");

        assertThat(result).isEqualTo("testValue");
        verify(bucket).set(eq("testValue"), anyLong(), any());
    }

    @Test
    void getByKey_notExist_shouldReturnNull() {

        when(redissonClient.<String>getBucket("config:no.such")).thenReturn(bucket);
        when(bucket.get()).thenReturn(null);

        stubLambdaQueryChain(List.of());

        String result = systemConfigService.getByKey("no.such");

        assertThat(result).isNull();
    }

    // ===== getIntByKey =====

    @Test
    void getIntByKey_valid_shouldParse() {

        when(redissonClient.<String>getBucket(anyString())).thenReturn(bucket);
        when(bucket.get()).thenReturn("20");

        int result = systemConfigService.getIntByKey("review.mastery_correct_delta", 10);

        assertThat(result).isEqualTo(20);
    }

    @Test
    void getIntByKey_invalid_shouldReturnDefault() {

        when(redissonClient.<String>getBucket(anyString())).thenReturn(bucket);
        when(bucket.get()).thenReturn("notANumber");

        int result = systemConfigService.getIntByKey("review.mastery_correct_delta", 10);

        assertThat(result).isEqualTo(10);
    }

    // ===== updateConfig =====

    @Test
    void updateConfig_success_shouldEvictCache() {

        SystemConfig config = new SystemConfig();
        config.setId(1L);
        config.setConfigKey("review.mastery_correct_delta");
        config.setConfigValue("20");

        stubLambdaQueryChain(List.of(config));
        doReturn(true).when(systemConfigService).updateById(any(SystemConfig.class));

        RBucket<String> evictBucket = mock(RBucket.class);
        when(redissonClient.<String>getBucket("config:review.mastery_correct_delta")).thenReturn(evictBucket);

        ConfigUpdateReq req = new ConfigUpdateReq();
        req.setConfigKey("review.mastery_correct_delta");
        req.setConfigValue("25");

        systemConfigService.updateConfig(req);

        verify(evictBucket).delete();
        assertThat(config.getConfigValue()).isEqualTo("25");
    }

    @Test
    void updateConfig_notExist_shouldThrow() {

        stubLambdaQueryChain(List.of());

        ConfigUpdateReq req = new ConfigUpdateReq();
        req.setConfigKey("no.such.key");
        req.setConfigValue("val");

        assertThatThrownBy(() -> systemConfigService.updateConfig(req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void updateConfig_invalidIntervals_count_shouldThrow() {

        SystemConfig config = new SystemConfig();
        config.setId(1L);
        config.setConfigKey("review.intervals");
        config.setConfigValue("0,1,2,4,7,15,30");

        stubLambdaQueryChain(List.of(config));

        ConfigUpdateReq req = new ConfigUpdateReq();
        req.setConfigKey("review.intervals");
        req.setConfigValue("0,1,2,4,7,15");

        assertThatThrownBy(() -> systemConfigService.updateConfig(req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void updateConfig_invalidIntervals_notMonotonic_shouldThrow() {

        SystemConfig config = new SystemConfig();
        config.setId(1L);
        config.setConfigKey("review.intervals");
        config.setConfigValue("0,1,2,4,7,15,30");

        stubLambdaQueryChain(List.of(config));

        ConfigUpdateReq req = new ConfigUpdateReq();
        req.setConfigKey("review.intervals");
        req.setConfigValue("0,1,2,4,3,15,30");

        assertThatThrownBy(() -> systemConfigService.updateConfig(req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void updateConfig_intOutOfRange_shouldThrow() {

        SystemConfig config = new SystemConfig();
        config.setId(1L);
        config.setConfigKey("review.mastery_correct_delta");
        config.setConfigValue("20");

        stubLambdaQueryChain(List.of(config));

        ConfigUpdateReq req = new ConfigUpdateReq();
        req.setConfigKey("review.mastery_correct_delta");
        req.setConfigValue("999");

        assertThatThrownBy(() -> systemConfigService.updateConfig(req))
                .isInstanceOf(BaseException.class);
    }

    // ===== 工具方法 =====

    private void stubLambdaQueryChain(List<SystemConfig> result) {

        LambdaQueryChainWrapper<SystemConfig> chain = MockChainHelper.mockChain();
        doReturn(chain).when(systemConfigService).lambdaQuery();
        when(chain.list()).thenReturn(result);
    }
}
