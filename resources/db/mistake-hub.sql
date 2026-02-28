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
    `cipher_password` VARCHAR(128) DEFAULT NULL COMMENT '加密密码（仅管理员）',
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
