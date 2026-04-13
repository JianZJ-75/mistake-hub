DROP DATABASE IF EXISTS mistake_hub;

CREATE DATABASE IF NOT EXISTS mistake_hub;

USE mistake_hub;

/**
  用户表
 */
CREATE TABLE `account`
(
    `id`              BIGINT                                 NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`            VARCHAR(128)                           NOT NULL COMMENT '用户名',
    `nickname`        VARCHAR(128)                           NOT NULL COMMENT '昵称',
    `avatar_url`      VARCHAR(512) DEFAULT NULL COMMENT '用户头像URL',
    `cipher_password` VARCHAR(256) DEFAULT NULL COMMENT '加密密码（仅管理员）',
    `role`            VARCHAR(16)                            NOT NULL COMMENT '角色',
    `wechat_open_id`  VARCHAR(128) DEFAULT NULL COMMENT '微信OpenID',
    `daily_limit`     BIGINT       DEFAULT 30 COMMENT '每日最大复习量',
    `created_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_time`    DATETIME                               NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    CONSTRAINT `wechat_open_id_unique` UNIQUE (`wechat_open_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

/**
  用户 Session 表
 */
CREATE TABLE `session`
(
    `id`           BIGINT                             NOT NULL AUTO_INCREMENT COMMENT '主键',
    `account_id`   BIGINT                             NOT NULL COMMENT '用户ID',
    `token`        VARCHAR(128)                       NOT NULL COMMENT 'Session Token',
    `expire_time`  DATETIME                           NOT NULL COMMENT 'Session 过期时间',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_time` DATETIME                           NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    CONSTRAINT `token_unique` UNIQUE (`token`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户 Session 表';

/**
  随机数表
 */
CREATE TABLE `nonce`
(
    `id`           BIGINT                             NOT NULL AUTO_INCREMENT COMMENT '主键',
    `nonce`        VARCHAR(128)                       NOT NULL COMMENT '随机数',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='随机数表';

/**
  错题表
 */
CREATE TABLE `mistake`
(
    `id`               BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `account_id`       BIGINT  NOT NULL COMMENT '所属用户',
    `title`            TEXT    NOT NULL COMMENT '题干内容',
    `correct_answer`   TEXT COMMENT '正确答案',
    `title_image_url`  VARCHAR(512)     DEFAULT NULL COMMENT '题目图片URL',
    `answer_image_url` VARCHAR(512)     DEFAULT NULL COMMENT '答案图片URL',
    `review_stage`     INT     NOT NULL DEFAULT 0 COMMENT '复习阶段 0-6',
    `mastery_level`    INT     NOT NULL DEFAULT 0 COMMENT '掌握度 0-100',
    `last_review_time` DATETIME         DEFAULT NULL COMMENT '最近复习时间',
    `next_review_time` DATETIME         DEFAULT NULL COMMENT '下次复习时间',
    `status`           TINYINT NOT NULL DEFAULT 1 COMMENT '1-有效 0-删除',
    `created_time`     DATETIME         DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_time`     DATETIME         DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_account_status` (`account_id`, `status`),
    INDEX `idx_next_review` (`account_id`, `next_review_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='错题表';

/**
  标签表
 */
CREATE TABLE `tag`
(
    `id`           BIGINT                             NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         VARCHAR(128)                       NOT NULL COMMENT '标签名称',
    `type`         VARCHAR(32)                        NOT NULL COMMENT '类型: SUBJECT/CHAPTER/KNOWLEDGE/CUSTOM',
    `parent_id`    BIGINT   DEFAULT 0 COMMENT '父标签ID，0为顶级',
    `account_id`   BIGINT   DEFAULT NULL COMMENT '所属用户ID，NULL为全局标签',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='标签表';

/**
  错题-标签关联表
 */
CREATE TABLE `mistake_tag`
(
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `mistake_id` BIGINT NOT NULL COMMENT '错题ID',
    `tag_id`     BIGINT NOT NULL COMMENT '标签ID',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_mistake_tag` (`mistake_id`, `tag_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='错题-标签关联表';

/**
  分布式锁表
 */
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

-- ========== 迭代 3：艾宾浩斯复习调度 ==========

/**
  复习计划表
 */
CREATE TABLE `review_plan`
(
    `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `account_id`   BIGINT      NOT NULL COMMENT '所属用户',
    `mistake_id`   BIGINT      NOT NULL COMMENT '关联错题',
    `planned_date` DATE        NOT NULL COMMENT '计划复习日期',
    `status`       VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/COMPLETED/SKIPPED',
    `created_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_account_mistake_date` (`account_id`, `mistake_id`, `planned_date`),
    INDEX `idx_account_date` (`account_id`, `planned_date`),
    INDEX `idx_mistake_date` (`mistake_id`, `planned_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='复习计划表';

/**
  复习记录表
 */
CREATE TABLE `review_record`
(
    `id`                  BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `account_id`          BIGINT   NOT NULL COMMENT '所属用户',
    `mistake_id`          BIGINT   NOT NULL COMMENT '关联错题',
    `review_plan_id`      BIGINT            DEFAULT NULL COMMENT '关联计划ID',
    `is_correct`          TINYINT  NOT NULL COMMENT '1-答对 0-答错',
    `review_stage_before` INT      NOT NULL COMMENT '复习前阶段',
    `review_stage_after`  INT      NOT NULL COMMENT '复习后阶段',
    `mastery_before`      INT      NOT NULL COMMENT '复习前掌握度',
    `mastery_after`       INT      NOT NULL COMMENT '复习后掌握度',
    `note`                TEXT              DEFAULT NULL COMMENT '答题备注',
    `note_image_url`      VARCHAR(512)      DEFAULT NULL COMMENT '答题图片URL',
    `review_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '复习时间',
    `updated_time`        DATETIME          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_account_time` (`account_id`, `review_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='复习记录表';
