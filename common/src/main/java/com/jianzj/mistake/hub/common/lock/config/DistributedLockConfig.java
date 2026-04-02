package com.jianzj.mistake.hub.common.lock.config;

import com.jianzj.mistake.hub.common.lock.service.DistributedLockService;
import com.jianzj.mistake.hub.common.lock.service.LockService;
import com.jianzj.mistake.hub.common.lock.service.impl.MySQLDistributedLock;
import com.jianzj.mistake.hub.common.lock.service.impl.RedisDistributedLock;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>分布式锁自动配置，通过 distributed.lock.type 切换 MySQL / Redis 实现</p>
 *
 * @author jian.zhong
 * @since 2026-03-23
 */
@Configuration
public class DistributedLockConfig {

    /**
     * MySQL 实现（默认）
     * 需要额外扫描 common 包下的 Mapper，因为 backend 的 @MapperScan 只扫描 backend.mapper
     */
    @ConditionalOnProperty(
            name = "distributed.lock.type",
            havingValue = "mysql",
            matchIfMissing = true
    )
    @Configuration
    @org.mybatis.spring.annotation.MapperScan("com.jianzj.mistake.hub.common.lock.mapper")
    public static class MySQLConfig {

        @Bean
        @ConditionalOnMissingBean(DistributedLockService.class)
        public DistributedLockService distributedLockService() {

            return new DistributedLockService();
        }

        @Bean
        @ConditionalOnMissingBean(LockService.class)
        public LockService mySQLDistributedLock(DistributedLockService distributedLockService) {

            return new MySQLDistributedLock(distributedLockService);
        }
    }

    /**
     * Redis 实现
     * 仅当 classpath 中存在 RedissonClient 且配置 distributed.lock.type=redis 时生效
     */
    @ConditionalOnProperty(
            name = "distributed.lock.type",
            havingValue = "redis"
    )
    @ConditionalOnClass(RedissonClient.class)
    @Configuration
    public static class RedisConfig {

        @Bean
        @ConditionalOnMissingBean(RedissonClient.class)
        public RedissonClient redissonClient(@Value("${spring.data.redis.host}") String host,
                                             @Value("${spring.data.redis.port}") String port) {

            Config config = new Config();
            config.useSingleServer().setAddress("redis://" + host + ":" + port);
            return Redisson.create(config);
        }

        @Bean
        @ConditionalOnMissingBean(LockService.class)
        public LockService redisDistributedLock(RedissonClient redissonClient) {

            return new RedisDistributedLock(redissonClient);
        }
    }
}
