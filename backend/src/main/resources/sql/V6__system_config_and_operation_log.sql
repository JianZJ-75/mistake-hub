-- ========== 迭代 6：系统配置表 ==========

CREATE TABLE IF NOT EXISTS system_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key      VARCHAR(128) NOT NULL UNIQUE COMMENT '配置键',
    config_value    VARCHAR(512) NOT NULL COMMENT '配置值',
    description     VARCHAR(256)          COMMENT '配置说明',
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '系统配置表';

-- 预置配置项
INSERT INTO system_config (config_key, config_value, description) VALUES
('review.intervals', '0,1,2,4,7,15,30', '复习间隔天数（逗号分隔，对应阶段 0~6）'),
('review.default_daily_limit', '30', '默认每日复习量上限'),
('review.mastery_correct_delta', '20', '答对时掌握度增量'),
('review.mastery_wrong_delta', '15', '答错时掌握度减量'),
('review.stage_wrong_back', '2', '答错时回退阶段数');

-- ========== 迭代 6：操作日志表 ==========

CREATE TABLE IF NOT EXISTS operation_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id      BIGINT                COMMENT '操作人ID',
    account_code    VARCHAR(64)           COMMENT '操作人 code',
    action          VARCHAR(128) NOT NULL COMMENT '操作类型',
    target_type     VARCHAR(64)           COMMENT '目标类型(ACCOUNT/MISTAKE/TAG/CONFIG)',
    target_id       VARCHAR(64)           COMMENT '目标ID',
    detail          TEXT                  COMMENT '操作详情(JSON)',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account (account_id),
    INDEX idx_time (created_time)
) COMMENT '操作日志表';
