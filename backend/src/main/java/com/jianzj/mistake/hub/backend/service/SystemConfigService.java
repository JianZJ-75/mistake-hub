package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.dto.req.ConfigUpdateReq;
import com.jianzj.mistake.hub.backend.entity.SystemConfig;
import com.jianzj.mistake.hub.backend.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

/**
 * <p>
 * 系统配置 服务类，提供 Redis 缓存加速读取，修改时自动失效缓存
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Service
@Slf4j
public class SystemConfigService extends ServiceImpl<SystemConfigMapper, SystemConfig> {

    private static final String CACHE_KEY_PREFIX = "config:";

    private static final long CACHE_TTL_HOURS = 24;

    private final RedissonClient redissonClient;

    public SystemConfigService(RedissonClient redissonClient) {

        this.redissonClient = redissonClient;
    }

    // ===== 业务方法 =====

    /**
     * 查询所有配置项
     */
    public List<SystemConfig> listAll() {

        return lambdaQuery().orderByAsc(SystemConfig::getId).list();
    }

    /**
     * 根据 key 获取配置值（优先读 Redis 缓存）
     */
    public String getByKey(String key) {

        String cacheKey = CACHE_KEY_PREFIX + key;
        RBucket<String> bucket = redissonClient.getBucket(cacheKey);
        String cached = bucket.get();

        if (cached != null) {
            return cached;
        }

        SystemConfig config = getConfigByKey(key);
        if (config == null) {
            return null;
        }

        bucket.set(config.getConfigValue(), CACHE_TTL_HOURS, TimeUnit.HOURS);
        return config.getConfigValue();
    }

    /**
     * 获取配置值，不存在时返回默认值
     */
    public String getByKey(String key, String defaultValue) {

        String value = getByKey(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取整数配置值，不存在或格式错误时返回默认值
     */
    public int getIntByKey(String key, int defaultValue) {

        String value = getByKey(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("系统配置 {} 值 {} 不是合法整数，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 更新配置值，同时失效 Redis 缓存
     */
    public void updateConfig(ConfigUpdateReq req) {

        SystemConfig config = getConfigByKey(req.getConfigKey());
        if (config == null) {
            oops("配置项不存在：%s", "Config key not found: %s", req.getConfigKey());
        }

        config.setConfigValue(req.getConfigValue());

        boolean success = updateById(config);
        if (!success) {
            oops("更新配置失败", "Failed to update config.");
        }

        evictCache(req.getConfigKey());
    }

    // ===== 工具方法 =====

    /**
     * 根据 configKey 查询配置
     */
    private SystemConfig getConfigByKey(String key) {

        List<SystemConfig> configs = lambdaQuery()
                .eq(SystemConfig::getConfigKey, key)
                .list();
        return CollectionUtils.isEmpty(configs) ? null : configs.get(0);
    }

    /**
     * 失效指定 key 的 Redis 缓存
     */
    private void evictCache(String configKey) {

        String cacheKey = CACHE_KEY_PREFIX + configKey;
        redissonClient.getBucket(cacheKey).delete();
    }
}
