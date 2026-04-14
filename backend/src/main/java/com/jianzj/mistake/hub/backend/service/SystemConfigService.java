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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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

    /**
     * 配置值校验器，key → 校验逻辑（不通过直接 oops）
     */
    private static final Map<String, Consumer<String>> VALIDATORS = Map.of(
            "review.intervals", SystemConfigService::validateIntervals,
            "review.mastery_correct_delta", v -> validateIntRange(v, 1, 100, "答对掌握度增量"),
            "review.mastery_wrong_delta", v -> validateIntRange(v, 1, 100, "答错掌握度减量"),
            "review.stage_wrong_back", v -> validateIntRange(v, 1, 6, "答错回退阶段数"),
            "review.default_daily_limit", v -> validateIntRange(v, 1, 200, "默认每日复习量")
    );

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

        Consumer<String> validator = VALIDATORS.get(req.getConfigKey());
        if (validator != null) {
            validator.accept(req.getConfigValue());
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
     * 校验 review.intervals 格式：逗号分隔的 7 个非负整数，且单调递增
     */
    private static void validateIntervals(String value) {

        String[] parts = value.split(",");
        if (parts.length != 7) {
            oops("复习间隔必须是 7 个逗号分隔的整数（对应阶段 0~6），当前 %d 个",
                    "review.intervals must contain exactly 7 comma-separated integers, got %d", parts.length);
        }

        int prev = -1;
        for (int i = 0; i < parts.length; i++) {
            int num;
            try {
                num = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                oops("复习间隔第 %d 项「%s」不是合法整数",
                        "review.intervals item %d '%s' is not a valid integer", i + 1, parts[i].trim());
                return;
            }
            if (num < 0) {
                oops("复习间隔第 %d 项不能为负数",
                        "review.intervals item %d must be non-negative", i + 1);
            }
            if (num < prev) {
                oops("复习间隔必须单调递增，第 %d 项(%d) < 第 %d 项(%d)",
                        "review.intervals must be non-decreasing, item %d (%d) < item %d (%d)",
                        i + 1, num, i, prev);
            }
            prev = num;
        }
    }

    /**
     * 校验整数范围
     */
    private static void validateIntRange(String value, int min, int max, String label) {

        int num;
        try {
            num = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            oops("%s必须是整数，当前值：%s", "%s must be an integer, got: %s", label, value);
            return;
        }
        if (num < min || num > max) {
            oops("%s必须在 %d ~ %d 之间，当前值：%d",
                    "%s must be between %d and %d, got: %d", label, min, max, num);
        }
    }

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
