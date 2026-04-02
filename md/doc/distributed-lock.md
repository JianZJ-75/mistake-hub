# 分布式锁使用文档

## 1. 概述

基于 `common` 模块的分布式锁基础设施，支持 **MySQL**（默认）和 **Redis** 两种后端实现。提供注解式和编程式两种使用方式。

- MySQL 方案：利用数据库唯一约束实现互斥，零额外中间件依赖
- Redis 方案：基于 Redisson，适合高并发场景

## 2. 配置

### 2.1 建表（MySQL 方案必须）

```sql
CREATE TABLE `distributed_lock`
(
    `id`           BIGINT                             NOT NULL AUTO_INCREMENT COMMENT '主键',
    `lock_name`    VARCHAR(255)                       NOT NULL COMMENT '锁名称',
    `expired_time` DATETIME                           NOT NULL COMMENT '过期时间',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lock_name` (`lock_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='分布式锁表';
```

### 2.2 application.yml

```yaml
# MySQL（默认，可省略）
distributed:
  lock:
    type: mysql

# 或 Redis
distributed:
  lock:
    type: redis
```

Redis 方案需要 `spring.data.redis.host` 和 `spring.data.redis.port` 配置。

## 3. 注解式使用（推荐）

在方法上标注 `@DistributedLockAnno`，AOP 自动加锁/解锁：

```java
import com.jianzj.mistake.hub.common.lock.annotation.DistributedLockAnno;

@DistributedLockAnno(key = "'order:submit'")
public void submitOrder() {
    // 业务逻辑，方法执行期间持有锁
}
```

### 3.1 SpEL 动态 key

支持通过 SpEL 表达式引用方法参数：

```java
// 引用简单参数
@DistributedLockAnno(key = "'user:bindPhone:' + #userId")
public void bindPhone(Long userId, String phone) { ... }

// 引用对象属性
@DistributedLockAnno(key = "'mistake:review:' + #req.mistakeId")
public void reviewMistake(ReviewReq req) { ... }

// 拼接多个参数
@DistributedLockAnno(key = "'tag:update:' + #tagId + ':' + #accountId")
public void updateTag(Long tagId, Long accountId) { ... }
```

### 3.2 等待与超时

```java
import java.time.temporal.ChronoUnit;

// 等待 5 秒获取锁，持有 60 秒后自动释放
@DistributedLockAnno(
    key = "'report:generate'",
    waitTime = 5,
    leaseTime = 60,
    timeUnit = ChronoUnit.SECONDS
)
public void generateReport() { ... }
```

### 3.3 自定义错误信息

```java
@DistributedLockAnno(
    key = "'order:' + #orderId",
    errorMessageCn = "订单正在处理中，请勿重复提交",
    errorMessageEn = "Order is being processed, please do not submit again."
)
public void processOrder(Long orderId) { ... }
```

## 4. 编程式使用

注入 `LockService` 手动控制加锁/解锁：

```java
import com.jianzj.mistake.hub.common.lock.service.LockService;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class SomeService {

    private final LockService lockService;

    public SomeService(LockService lockService) {
        this.lockService = lockService;
    }

    public void doSomething() {
        String lockKey = "custom:lock:key";
        boolean locked = false;

        try {
            locked = lockService.tryLock(lockKey, 30, ChronoUnit.SECONDS);
            if (!locked) {
                oops("获取锁失败", "Failed to acquire lock.");
            }

            // 业务逻辑
        } finally {
            if (locked) {
                lockService.unlock(lockKey);
            }
        }
    }
}
```

## 5. 包结构

```
common/lock/
├── annotation/     注解定义
├── aspect/         AOP 切面
├── config/         条件配置（MySQL / Redis 自动切换）
├── entity/         数据库实体
├── mapper/         MyBatis Plus Mapper
├── service/        锁服务接口 + DistributedLockService
│   └── impl/       抽象模板 + MySQL/Redis 实现
└── support/        SpEL 表达式解析器
```

## 6. 注意事项

| 项目 | 说明 |
|------|------|
| 锁粒度 | key 越细粒度越高，如 `user:{id}` 比 `user:all` 并发更好 |
| leaseTime | 必须大于业务执行时间，否则锁提前释放会导致并发问题 |
| 默认行为 | waitTime=0 表示抢锁失败立即报错，不自旋等待 |
| 异常处理 | 注解方式抢锁失败抛 `BaseException`，由全局异常处理器返回 400 |
| 停机释放 | 应用关闭时自动释放所有锁（MySQL 清表，Redis 靠 TTL 自动过期） |
